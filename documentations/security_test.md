# Audit de sécurité — CoreProject

> Date : 12/06/2026
> Périmètre : backend Spring Boot (`COREPROJECT_NOV25/CoreProject`) + frontend Angular (`FrontLast/CoreProject_Front`)
> Méthode : revue de la configuration Spring Security, des routes publiques/CSRF, du JWT, des cookies, du rate limiting, des webhooks, des secrets et des points d'injection.

---

## 🔴 Critiques (à corriger avant toute mise en production)

### 1. ~~Token Twilio accessible sans authentification — risque de fraude téléphonique~~ ✅ CORRIGÉ (15/06/2026)

~~`il/routes/telephony/CrmTwilioRoutes.java:32` expose publiquement `/api/crm/telephony/twilio/token/test`.~~

**Correction appliquée :** endpoint `/token/test`, méthode `generateTestReceiverToken()`, `test-receiver.html` et `test-receiver.js` **supprimés**. La route a été retirée de `TWILIO_PUBLIC_ROUTES`. Aucun artefact résiduel.

### 2. CSRF désactivé sur la quasi-totalité de l'API

`il/routes/SecurityRoutesAggregator.CSRF_IGNORE` agrège ~40 domaines, et les fichiers CRM font `CSRF_IGNORE = concatenate(READ_ROUTES, WRITE_ROUTES)` — donc **tous les endpoints d'écriture CRM (création/suppression de contacts, deals, appels…) sont attaquables en CSRF**, alors que l'auth repose sur des cookies. Idem pour `/api/auth/logout`, les notifications, etc. (les commentaires "⚠️ DEV ONLY" le reconnaissent).

**Aggravant :** les cookies sont créés via l'API `jakarta.servlet.Cookie` (`bll/common/services/cookies/BaseCookieService.java`) qui **ne pose aucun attribut `SameSite`**. La protection repose uniquement sur le défaut `Lax` des navigateurs récents.

**Recommandations :**
- Réactiver le CSRF sur toutes les routes cookie-authentifiées (ne garder en ignore que : login/refresh, webhooks Twilio, lead ingest).
- Migrer `BaseCookieService` vers `ResponseCookie` de Spring avec `.sameSite("Strict")` (ou `Lax`) sur `access_token`/`refresh_token`.
- Côté frontend : interceptor Angular pour renvoyer le header `X-XSRF-TOKEN`.

### 3. ~~Routes de debug/test publiques~~ ✅ CORRIGÉ (15/06/2026)

~~`il/routes/debug/DebugRoutes.java` ouvre `/api/debug/**` et `/api/test/device-security/**` sans auth ni CSRF.~~

**Correction appliquée :** `TokenDebugController` (déjà commenté) **supprimé**. `TestDeviceSecurityController` annoté `@Profile("dev")` — bean inexistant hors du profil dev. `DebugRoutes` nettoyé : `/api/debug/**` et `/api/test/device-security/**` retirés de `PUBLIC` et `CSRF_IGNORE`, seul `/error` conservé.

---

## 🟠 Élevées

### 4. ~~Wildcard public `/api/lead/**` "pour les futurs endpoints"~~ ✅ CORRIGÉ (15/06/2026)

**Correction appliquée :** `BASE + "/**"` retiré de `LeadRoutes.PUBLIC`. Seul `BASE` (`/api/lead`) reste public — match exact sur `POST /api/lead`. Tout futur endpoint sous `/api/lead/*` requiert une authentification par défaut.

### 5. Rate limiting contournable et absent où il compte ✅ CORRIGÉ (23/06/2026)

- `il/filters/RateLimitFilter.java` faisait confiance à `X-Forwarded-For` sans condition → contournable en falsifiant l'en-tête.
- `config/ratelimiting.yml` excluait `/api/public/**` → lead ingest sans rate limit.
- Note : `/api/auth/login` et `/api/password/forgot/**` ont leur propre protection brute-force au niveau service (`AuthServiceImpl`) avec lockout par username/IP/combiné — déjà suffisant.

**Corrections appliquées :**
- `server.forward-headers-strategy=native` ajouté dans `config/server.yml` — Tomcat's `RemoteIpValve` traite le XFF uniquement depuis les proxies de confiance (loopback par défaut). `getClientIP()` simplifié à `request.getRemoteAddr()`.
- `/api/public/**` retiré des `exclude-paths` dans `ratelimiting.yml`.

**Action prod requise :** si nginx est sur un VPS séparé, ajouter `server.tomcat.remoteip.trusted-proxies=<ip-nginx>` dans la config prod.

### 6. ~~HSTS désactivé~~ ✅ CORRIGÉ (avant 15/06/2026)

~~`il/configs/SecurityConfig.java:122` — le bloc HSTS est commenté.~~

**Correction appliquée :** HSTS conditionnel via `@Value("${security.hsts.enabled:false}")` dans `SecurityConfig.java:72-128`. Désactivé par défaut (dev), activable en prod via `security.hsts.enabled=true`. `includeSubDomains(true)` + 1 an (`maxAgeInSeconds(31536000)`).

### 7. ~~`/actuator/prometheus` public~~ ✅ DÉCISION ARCHITECTURALE (18/06/2026)

**Décision :** rester public dans Spring Security — protection déléguée au niveau infra (nginx/ufw sur VPS).

**Rationale :** Prometheus scrape via HTTP brut, pas de session possible. Sur VPS mono-tenant contrôlé, bloquer l'accès externe à `/actuator/prometheus` au niveau nginx (ou ufw) est la solution standard. L'auth applicative (JWT/cookies) ne s'applique pas aux scrapers.

**Action prod requise :** configurer nginx pour que `/actuator/prometheus` ne soit accessible que depuis `127.0.0.1` (ou le réseau interne du VPS).

---

## 🟡 Moyennes

### 8. Clé de chiffrement téléphonie et TOTP avec défaut silencieux ✅ CORRIGÉ (23/06/2026)

**Problème 1 — `config/telephony.yml`** : `encryption-key: ${TELEPHONY_ENCRYPTION_KEY:local-dev-key-not-for-production}`. Valeur par défaut publiquement connue (dans le repo) → credentials SIP déchiffrables si la var manque en prod.

**Problème 2 — `config/security/two-factor/totp.yml`** : `encryption-key: "StebyTOTPKey2025!"` hardcodée en dur. Secrets TOTP (2FA) de tous les utilisateurs chiffrés avec une clé compromise dès qu'un accès au repo est obtenu.

**Correction appliquée :**
- `telephony.yml` → `${TELEPHONY_ENCRYPTION_KEY}` (pas de valeur par défaut — fail-fast au démarrage)
- `totp.yml` → `${TOTP_ENCRYPTION_KEY}` (pas de valeur par défaut — fail-fast au démarrage)

**Génération des clés pour la prod :**

```bash
# TELEPHONY_ENCRYPTION_KEY — credentials SIP (32 chars recommandé)
openssl rand -base64 24

# TOTP_ENCRYPTION_KEY — secrets 2FA utilisateurs (16 chars minimum, AES-128)
openssl rand -base64 12
```

> ⚠️ **TOTP en prod :** la clé `TOTP_ENCRYPTION_KEY` doit être **identique à celle utilisée pour chiffrer les secrets TOTP existants**. Changer cette clé sur une base non vide invalide tous les 2FA utilisateurs. Stocker la clé dans un gestionnaire de secrets (Vault, Secret Manager…) et ne jamais la faire tourner sans migration préalable des données.

### 9. `show-sql: true` dans la config globale ✅ CORRIGÉ (23/06/2026)

`config/database.yml` — toutes les requêtes SQL (avec données potentiellement personnelles) partaient dans les logs en prod.

**Correction appliquée :** `show-sql: false` et `format_sql: false` par défaut. Surcharge via bloc `---` avec `spring.config.activate.on-profile: dev` qui réactive les deux uniquement en profil dev.

### 10. Injection de wildcards LIKE dans la recherche globale ✅ CORRIGÉ (23/06/2026)

`bll/domains/crm/search/services/GlobalSearchServiceImpl.java` ne neutralisait pas `%` et `_` dans le mot-clé. Un utilisateur pouvait taper `%` pour énumérer toute la base, ou des patterns complexes pour dégrader les performances.

**Correction appliquée :**
- Méthode `escapeLike()` ajoutée dans `GlobalSearchServiceImpl` : échappe `\` → `\\`, `%` → `\%`, `_` → `\_` avant le wrapping LIKE.
- Clause `ESCAPE '\\'` ajoutée sur les 4 requêtes JPQL (`ContactRepository`, `LeadRepository`, `DealRepository`, `OrganisationRepository`).

### 11. Réflexion de message d'erreur non échappé dans du JSON ✅ CORRIGÉ (23/06/2026)

`il/filters/JwtFilter.java` : `write("{\"error\": \"" + message + "\"}")` — concaténation brute pouvant produire du JSON invalide si le message contient des guillemets, et fuite d'information sur la structure interne du token.

**Correction appliquée :** message statique `{"error": "Unauthorized"}` — le frontend Angular gère le 401 via l'interceptor HTTP, le contenu du message n'est pas exploité.

### 12. CSP avec artefacts dev et directives larges ✅ CORRIGÉ (23/06/2026)

`il/configs/SecurityConfig.java` : `ws://localhost:8088` et `wss://localhost:8089` hardcodés (dev Asterisk), `img-src https:` (toute origine HTTPS), `style-src 'unsafe-inline'` (inutile — CSS maison uniquement).

**Correction appliquée :**
- `security.csp.extra-connect-src` ajouté dans `config/security/auth.yml` — vide par défaut (prod), URLs Asterisk injectées via profil `dev`.
- `img-src` restreint à `'self' data:` — suppression du wildcard `https:`.
- `style-src 'unsafe-inline'` supprimé — `'self'` uniquement.
- `font-src https://fonts.gstatic.com` supprimé — fonts servies en local uniquement.

---

## ✅ Points solides relevés

- Architecture d'autorisation centralisée et lisible (`SecurityRoutesAggregator`), `@PreAuthorize` en doublon sur les contrôleurs CRM (défense en profondeur).
- JWT en cookies `HttpOnly` + validation du device (fingerprint, blacklist, logout) dans `JwtFilter` — bien au-dessus du standard.
- Secrets via variables d'environnement, aucune credential en dur trouvée.
- Webhooks Twilio vérifiés par signature (`RequestValidator`), lead ingest signé par plateforme.
- BCrypt, 2FA multi-méthodes, GDPR export à double confirmation par token avec TTL.
- Le `bypassSecurityTrustHtml` du frontend ne porte que sur des SVG statiques internes — acceptable.

---

## Plan d'action (ordre de priorité)

| # | Chantier | Effort estimé | Findings couverts |
| --- | --- | --- | --- |
| 1 | Supprimer/profiler `token/test` Twilio + routes debug | ~30 min | #1, #3 |
| 2 | `SameSite` sur les cookies + réactivation CSRF (backend + interceptor Angular) | ~1 journée | #2 |
| 3 | Fix `X-Forwarded-For` + bucket strict sur login | 2-3 h | #5 |
| 4 | Wildcard `/api/lead/**`, actuator, HSTS, fail-fast encryption key | 1-2 h | #4, #6, #7, #8 |
| 5 | Nettoyages moyens (show-sql, LIKE escape, JSON error, CSP) | 1-2 h | #9-#12 |

---
---

# Audit de sécurité — Frontend Angular

> Périmètre : `FrontLast/CoreProject_Front`
> Verdict global : **aucune faille critique propre au frontend**. Surface d'attaque réduite, patterns sains.

## ✅ Points solides

- **Aucun token dans localStorage/sessionStorage** — l'auth repose entièrement sur les cookies `HttpOnly` posés par le backend. Le localStorage ne contient que le thème, l'état de la sidebar, et des événements de synchro inter-onglets sans payload sensible (`core/auth/services/auth-sync.service.ts`).
- **Un seul `bypassSecurityTrustHtml`** dans tout le projet (`features/auth/login/components/oauth-button/oauth-button.component.ts:67`), portant uniquement sur des SVG statiques définis dans le code — pas de données utilisateur.
- **Aucun sink XSS trouvé** : pas de `[innerHTML]` sur des données utilisateur, pas d'`eval`, pas de manipulation DOM brute. Le contenu riche TipTap (notes, emails) est réinjecté via `editor.commands.setContent()` qui filtre à travers le schéma ProseMirror — pas de rendu HTML brut.
- **L'interceptor envoie déjà `X-XSRF-TOKEN`** sur les requêtes non-GET (`core/http/auth.interceptor.ts:109-114`). Le frontend est **déjà prêt** pour le chantier CSRF backend (finding #2) — il ne reste qu'à réactiver la vérification côté Spring.
- **Refresh token bien géré** : file d'attente des requêtes concurrentes pendant le refresh, nettoyage de session et broadcast multi-onglets en cas d'échec.
- **Dépendances récentes** (Angular 21, SIP.js 0.21, Twilio Voice SDK 2.18) — pas de lib notoirement vulnérable. Lancer `npm audit` régulièrement.

## 🟠 Élevée

### A1. Mot de passe SIP transmis en clair au navigateur

`core/telephony/services/sip.service.ts:85` — le backend envoie `sipPassword` en JSON et le navigateur l'utilise pour s'enregistrer sur Asterisk. C'est **inhérent au fonctionnement de SIP.js** (pas un bug), mais la conséquence est réelle : toute XSS ou extension navigateur malveillante peut voler les credentials téléphonie et passer des appels via l'Asterisk.

**Recommandation :** limiter les permissions des extensions côté Asterisk (pas d'appels sortants internationaux/premium dans le dialplan) et prévoir une rotation des mots de passe SIP. Défense en profondeur qui compense l'exposition obligatoire.

## 🟡 Moyennes / mineures

### A2. `returnUrl` non validé ✅ CORRIGÉ (23/06/2026)

`core/auth/services/auth.facade.ts` — `router.navigateByUrl(returnUrl)` avec une valeur venant du query param.

**Correction appliquée :** `safeUrl` vérifie que la valeur commence par `/` et pas `//` — sinon fallback sur `/`. Appliqué avant la navigation et avant le passage en query param `mustChangePassword`.

### A3. `document.write` pour l'impression des backup codes ✅ CORRIGÉ (23/06/2026)

`features/account/.../backup-codes-modal.component.ts` — `document.write` sur une fenêtre ouverte via `window.open('', '_blank')`, pattern fragile.

**Correction appliquée :** Blob URL — le contenu HTML est encapsulé dans un `Blob`, une URL objet est créée via `URL.createObjectURL`, la fenêtre s'ouvre directement sur cette URL. La Blob URL est révoquée via `URL.revokeObjectURL` dans `onafterprint` (ou immédiatement si `window.open` échoue).

### A4. `secure: false` dans le proxy dev

`proxy.conf.js` désactive la vérification TLS — normal pour un certificat auto-signé en dev, à ne jamais répliquer dans une config de prod (en prod le front est servi statiquement, donc non concerné en pratique).
