# Asterisk local — Test SIP/WebRTC avec Docker

## Ce que ce guide couvre

Faire tourner Asterisk sur ta machine en Docker pour tester l'intégration SIP du CRM **sans opérateur télécom**, sans serveur distant, sans domaine public.

À la fin tu auras :
- Asterisk qui tourne en Docker sur `localhost:8088` (WS plain — pour dev local)
- Le CRM configuré pour s'y connecter
- Un test de son (echo) pour valider le micro/audio du navigateur
- Un test d'appel entre deux onglets pour valider le flux complet

---

## Vue d'ensemble de ce qu'on monte

```
Navigateur (SIP.js)
    ↕ WS — ws://localhost:8088/ws  (dev local — plain WebSocket)
    ↕ WSS — wss://localhost:8089/ws (prod — WSS + certificat valide)
Asterisk (Docker)
    (pas de SIP trunk — appels internes uniquement)
```

---

## Prérequis

- Docker Desktop installé et démarré
- OpenSSL installé (`openssl version` dans un terminal pour vérifier)
- Le backend CRM qui tourne (`./mvnw spring-boot:run`)
- `openssl` : disponible sur Windows via Git Bash, WSL, ou nativement

---

## Étape 1 — Créer la structure de fichiers

Crée ce dossier dans ton projet (à la racine, à côté de `src/`) :

```
asterisk-local/
├── docker-compose.yml
├── keys/
│   ├── asterisk.crt      ← à générer (Étape 2)
│   └── asterisk.key      ← à générer (Étape 2)
└── config/
    ├── http.conf
    ├── pjsip.conf
    └── extensions.conf
```

---

## Étape 2 — Générer un certificat self-signed

Dans un terminal, depuis le dossier `asterisk-local/keys/` :

```bash
openssl req -x509 -newkey rsa:2048 -keyout asterisk.key -out asterisk.crt \
  -days 3650 -nodes \
  -subj "/CN=localhost" \
  -addext "subjectAltName=IP:127.0.0.1,DNS:localhost"
```

Ça génère un certificat valide 10 ans pour `localhost`. Pas besoin de le signer par une CA officielle — c'est uniquement pour du test local.

---

## Étape 3 — Créer les fichiers de configuration

### `asterisk-local/config/http.conf`

```ini
[general]
enabled=yes
bindaddr=0.0.0.0
bindport=8088

tlsenable=yes
tlsbindaddr=0.0.0.0:8089
tlscertfile=/etc/asterisk/keys/asterisk.crt
tlsprivatekey=/etc/asterisk/keys/asterisk.key
```

### `asterisk-local/config/pjsip.conf`

Deux extensions : `1001` (commercial) et `1002` (pour simuler un second participant).  
Copie ce bloc, il est autonome — aucun SIP trunk nécessaire.

```ini
; ── Transport WebSocket plain (dev local — Chrome accepte sans certificat) ─
[transport-ws]
type=transport
protocol=ws
bind=0.0.0.0:8088

; ── Transport WebSocket sécurisé (prod — nécessite un vrai certificat) ────
[transport-wss]
type=transport
protocol=wss
bind=0.0.0.0:8089
cert_file=/etc/asterisk/keys/asterisk.crt
priv_key_file=/etc/asterisk/keys/asterisk.key

; ── Extension 1001 (commercial CRM) ──────────────────────────────────────
[1001]
type=endpoint
transport=transport-ws
context=from-internal
disallow=all
allow=opus
allow=ulaw
webrtc=yes
dtls_cert_file=/etc/asterisk/keys/asterisk.crt
dtls_private_key=/etc/asterisk/keys/asterisk.key
dtls_verify=fingerprint
dtls_setup=actpass
ice_support=yes
auth=auth-1001
aors=1001

[auth-1001]
type=auth
auth_type=userpass
username=1001
password=test1234

[1001]
type=aor
max_contacts=1
remove_existing=yes

; ── Extension 1002 (second participant pour test d'appel) ────────────────
[1002]
type=endpoint
transport=transport-ws
context=from-internal
disallow=all
allow=opus
allow=ulaw
webrtc=yes
dtls_cert_file=/etc/asterisk/keys/asterisk.crt
dtls_private_key=/etc/asterisk/keys/asterisk.key
dtls_verify=fingerprint
dtls_setup=actpass
ice_support=yes
auth=auth-1002
aors=1002

[auth-1002]
type=auth
auth_type=userpass
username=1002
password=test5678

[1002]
type=aor
max_contacts=1
remove_existing=yes
```

> **Note :** Le nom de la section `[aor]` **doit correspondre exactement** au SIP username (`1001`, `1002`). Un nom différent (ex. `aor-1001`) provoque un 404 sur REGISTER.

### `asterisk-local/config/extensions.conf`

```ini
[from-internal]

; ── Test echo : le commercial entend sa propre voix ──────────────────────
; Appeler le numéro "echo" (ou n'importe quel numéro mappé dans le CRM)
exten => echo,1,Answer()
 same => n,Playback(demo-congrats)
 same => n,Echo()
 same => n,Hangup()

; ── Appel entre les deux extensions locales ──────────────────────────────
exten => 1001,1,Dial(PJSIP/1001,30)
 same => n,Hangup()

exten => 1002,1,Dial(PJSIP/1002,30)
 same => n,Hangup()
```

---

## Étape 4 — `docker-compose.yml`

```yaml
services:
  asterisk:
    image: andrius/asterisk:20.latest
    container_name: asterisk-crm-local
    ports:
      - "5060:5060/udp"
      - "8088:8088"
      - "8089:8089"
      - "10000-10099:10000-10099/udp"
    volumes:
      - ./config:/etc/asterisk
      - ./keys:/etc/asterisk/keys
    restart: unless-stopped
```

> Le RTP est limité à 100 ports (10000–10099) — largement suffisant pour du test local.

---

## Étape 5 — Démarrer Asterisk

```bash
cd asterisk-local
docker compose up -d
```

Vérifier que le container tourne :

```bash
docker logs asterisk-crm-local --tail 30
```

Tu dois voir des lignes comme :
```
Asterisk Ready.
```

Si tu vois des erreurs sur les modules PJSIP ou les certificats, voir la section Résolution de problèmes en bas.

---

## Étape 6 — (Dev local) Pas d'étape certificat nécessaire

En dev local on utilise le transport **WS plain** (port 8088), donc le navigateur n'a pas à valider de certificat pour ouvrir le WebSocket. Cette étape est inutile.

> **En production**, quand tu passes sur WSS (port 8089), il faudra soit un certificat signé par une CA reconnue, soit accepter manuellement le self-signed dans le navigateur :
> 1. Ouvrir `https://localhost:8089/httpstatus`
> 2. Accepter l'avertissement de sécurité
> 3. Mettre `transport=transport-wss` dans `pjsip.conf` et `wsUrl: wss://...` dans la TelephonyConfig

---

## Étape 7 — Configurer le CRM

### 7.1 — Créer la TelephonyConfig SIP

```
POST /api/crm/telephony/configs
```
```json
{
  "provider": "SIP",
  "credentialsJson": "{\"wsUrl\":\"ws://localhost:8088/ws\",\"sipDomain\":\"localhost\",\"outboundContext\":\"from-internal\"}",
  "callerId": null,
  "active": true
}
```

### 7.2 — Assigner l'extension 1001 au commercial

```
POST /api/crm/telephony/sip
```
```json
{
  "targetUserPublicId": "<publicId du commercial>",
  "sipUsername": "1001",
  "sipPassword": "test1234",
  "displayName": "Commercial Test"
}
```

### 7.3 — Faire les deux appels depuis la console du navigateur (plus rapide que Postman)

Ouvre le CRM, connecte-toi en tant qu'**admin** ou **commercial**, puis appuie sur **F12** → onglet **Console**.

**Étape A — Récupérer le publicId du commercial**

Si tu ne connais pas le publicId, colle ceci pour le trouver directement :

```javascript
fetch('/api/users/me', { credentials: 'include' })
  .then(r => r.json())
  .then(u => console.log('publicId :', u.publicId));
```

Copie la valeur affichée — tu en as besoin pour l'étape C.

---

**Étape B — Créer la TelephonyConfig SIP**

```javascript
fetch('/api/crm/telephony/configs', {
  method: 'POST',
  credentials: 'include',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    provider: 'SIP',
    credentialsJson: JSON.stringify({
      wsUrl: 'ws://localhost:8088/ws',
      sipDomain: 'localhost',
      outboundContext: 'from-internal'
    }),
    callerId: null,
    active: true
  })
}).then(r => r.json()).then(console.log);
```

Tu dois voir la config créée dans la console (avec son `id`). Si tu as un 409 Conflict, la config existe déjà — passe à l'étape C.

---

**Étape C — Assigner l'extension SIP au commercial**

Remplace `COLLE_LE_PUBLIC_ID_ICI` par la valeur récupérée à l'étape A :

```javascript
fetch('/api/crm/telephony/sip', {
  method: 'POST',
  credentials: 'include',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    targetUserPublicId: 'COLLE_LE_PUBLIC_ID_ICI',
    sipUsername: '1001',
    sipPassword: 'test1234',
    displayName: 'Commercial Test'
  })
}).then(r => r.json()).then(console.log);
```

---

**Étape D — Vérifier que le CRM récupère bien la config**

```javascript
fetch('/api/crm/telephony/sip/connection', { credentials: 'include' })
  .then(r => r.json())
  .then(console.log);
```

Tu dois voir `wsUrl`, `sipDomain`, `sipUsername`. Si la réponse est vide, la config n'est pas assignée à cet utilisateur.

---

## Étape 8 — Vérifier que SIP.js s'enregistre

1. Ouvrir le CRM dans le navigateur
2. Ouvrir les DevTools → Console
3. Le CRM tente de s'enregistrer au démarrage via `SipService.initialize()`

Si tout va bien, aucune erreur SIP dans la console. Pour voir le statut Asterisk :

```bash
docker exec asterisk-crm-local asterisk -rx "pjsip show endpoints"
```

L'extension 1001 doit apparaître avec le statut `Avail` ou `InUse`.

---

## Étape 9 — Tester

### Test minimal — Echo (valide micro + audio)

Dans le CRM, créer un contact avec le numéro de téléphone `echo`.  
Lancer l'appel → le commercial entend un message Asterisk puis sa propre voix avec 1 seconde de délai.  
Raccrocher. Une Interaction doit apparaître dans le CRM.

### Test complet — Appel entre deux onglets

1. **Onglet A** : ouvrir le CRM avec le compte du commercial (extension 1001 configurée)
2. **Onglet B** : utiliser un client SIP en ligne comme [SIP.js demo](https://demo.sipjs.com/) ou [Jssip online](http://tryit.jssip.net/) :
   - Server : `ws://localhost:8088/ws`
   - SIP URI : `sip:1002@localhost`
   - Password : `test5678`
3. Depuis l'**Onglet A**, appeler le numéro `1002`
4. L'**Onglet B** sonne — décrocher
5. Les deux entendent de l'audio

---

## Arrêter Asterisk

```bash
docker compose down
```

---

## Résolution de problèmes

| Symptôme | Cause | Solution |
|---|---|---|
| `docker compose up` : image not found | L'image `andrius/asterisk:20.latest` n'existe pas dans ce tag exact | Essayer `andrius/asterisk:latest` ou `fonoster/asterisk:20` |
| Logs : `Unable to load module res_pjsip` | Modules manquants dans l'image Docker | Utiliser l'image `fonoster/asterisk` qui inclut tous les modules |
| SIP.js reste `UNREGISTERED` | Certificat non accepté dans le navigateur | Refaire l'Étape 6 |
| SIP.js reste `UNREGISTERED` | Config CRM incorrecte (mauvais wsUrl) | Vérifier via `GET /api/crm/telephony/sip/connection` |
| Appel initié mais pas de son | Ports RTP non exposés | Vérifier que les ports 10000-10099/UDP sont bien dans le docker-compose |
| `pjsip show endpoints` : extension `Unavail` | Extension jamais connectée | Normal avant la première connexion SIP.js |
| Appel qui échoue immédiatement | Numéro appelé non reconnu dans le dialplan | Utiliser `echo`, `1001` ou `1002` uniquement en local |

---

## Si `andrius/asterisk` ne fonctionne pas

Certaines versions de l'image `andrius/asterisk` n'incluent pas tous les modules PJSIP WebRTC. Dans ce cas, remplacer l'image dans `docker-compose.yml` :

```yaml
image: fonoster/asterisk:20
```

Les volumes et ports restent identiques.
