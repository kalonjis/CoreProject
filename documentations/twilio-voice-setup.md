# Twilio Voice — Configuration pour l'intégration CRM

## Vue d'ensemble

Ce document couvre la configuration complète de Twilio Voice pour l'intégration téléphonie du CRM.  
Le CRM utilise **Twilio Voice SDK** (`@twilio/voice-sdk`) côté browser pour passer les appels directement depuis l'application.  
Twilio sert de pont entre le browser (WebRTC) et le téléphone du client (PSTN).

```
Browser (Twilio Voice SDK)
    ↕ WebRTC / HTTPS
Twilio Edge Network
    ↕ HTTPS
Backend CRM (TwiML App)         ← Voice URL + Status Callback
    ↓
    ↕ PSTN
Téléphone du client
```

---

## Prérequis

| Élément | Détail |
|---|---|
| Compte Twilio | Account SID + Auth Token (console.twilio.com) |
| Numéro Twilio | Numéro E.164 acheté ou vérifié dans la console Twilio |
| API Key | Paire SK… / secret (meilleure pratique — ne pas utiliser l'Auth Token) |
| Backend accessible | URL HTTPS publique pointant vers le backend CRM |

---

## Étape 1 — Créer une API Key Twilio

L'Access Token frontend est signé avec une API Key (pas l'Auth Token). C'est plus sécurisé car une API Key peut être révoquée sans changer l'Auth Token.

1. Aller dans **Twilio Console → Account → API Keys & Tokens**
2. Cliquer **Create API Key**
3. Choisir le type **Standard**
4. Nommer la clé (ex : `crm-voice-key`)
5. Copier le **SID** (`SKxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`) et le **Secret** — le secret n'est affiché qu'une seule fois

---

## Étape 2 — Créer une TwiML Application

La TwiML Application configure les URLs que Twilio appelle lors d'un appel initié depuis le browser.

1. Aller dans **Twilio Console → Voice → TwiML Apps**
2. Cliquer **Create new TwiML App**
3. Remplir :

| Champ | Valeur |
|---|---|
| Friendly Name | `CRM Voice App` (ou tout autre nom) |
| Voice → Request URL | `POST https://<votre-domaine>/api/crm/telephony/twilio/twiml` |
| Voice → Status Callback URL | `POST https://<votre-domaine>/api/crm/telephony/twilio/webhook` |

4. Cliquer **Save**
5. Copier le **SID** de l'application (`APxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`)

> **Note :** `<votre-domaine>` est la valeur de `TWILIO_WEBHOOK_BASE_URL` (voir Étape 4).  
> En développement local, utiliser un tunnel ngrok : `ngrok http 8443 --scheme https`.

---

## Étape 3 — Configurer la TelephonyConfig dans le CRM

Appeler l'endpoint admin pour enregistrer la configuration Twilio dans le CRM.  
Les credentials sont chiffrés en AES-256/GCM avant stockage en base.

**Endpoint :** `POST /api/crm/telephony/configs`  
**Auth :** ADMIN requis  
**Content-Type :** `application/json`

```json
{
  "provider": "TWILIO",
  "credentialsJson": "{\"accountSid\":\"ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"authToken\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"apiKeySid\":\"SKxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"apiKeySecret\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"twimlAppSid\":\"APxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}",
  "callerId": "+32XXXXXXXXX",
  "active": true
}
```

| Champ | Valeur |
|---|---|
| `accountSid` | SID du compte Twilio (commence par `AC`) |
| `authToken` | Auth Token du compte (pour vérifier les signatures webhook) |
| `apiKeySid` | SID de l'API Key créée à l'Étape 1 (commence par `SK`) |
| `apiKeySecret` | Secret de l'API Key créée à l'Étape 1 |
| `twimlAppSid` | SID de la TwiML App créée à l'Étape 2 (commence par `AP`) |
| `callerId` | Numéro Twilio en format E.164 (ex : `+32XXXXXXXXX`) |
| `active` | `true` pour activer immédiatement |

> **Important :** Si une autre configuration est déjà active (SIP ou TEL_URI), désactiver d'abord l'ancienne :  
> `PATCH /api/crm/telephony/configs/{publicId}/deactivate`

---

## Étape 4 — Variables d'environnement

Ajouter à votre fichier `.env` (ou configuration serveur) :

```env
# Clé de chiffrement pour les credentials téléphonie (AES-256)
# Doit être identique à celle utilisée lors de la création de la config
TELEPHONY_ENCRYPTION_KEY=votre_cle_32_caracteres_minimum

# URL de base du backend telle que Twilio la voit
# Utilisé pour valider la signature X-Twilio-Signature sur les webhooks
# Obligatoire si le backend est derrière un reverse proxy (Nginx, Caddy, etc.)
TWILIO_WEBHOOK_BASE_URL=https://api.votre-domaine.com
```

> **Pourquoi `TWILIO_WEBHOOK_BASE_URL` ?**  
> Twilio signe ses requêtes avec l'URL complète qu'il a utilisée (ex: `https://api.example.com/...`).  
> Si le backend est derrière un proxy qui termine SSL, Spring reçoit `http://localhost:8080/...` — la signature serait invalide.  
> Cette variable force l'URL correcte pour la vérification.  
> Si le backend est exposé directement en HTTPS sans proxy, cette variable peut être omise.

---

## Étape 5 — Vérification

### Tester le token endpoint

```bash
curl -X GET https://<domaine>/api/crm/telephony/twilio/token \
  -H "Cookie: access_token=<votre_jwt>"
```

Réponse attendue :
```json
{
  "token": "eyJ..."
}
```

### Tester le TwiML endpoint (simulation Twilio)

```bash
curl -X POST https://<domaine>/api/crm/telephony/twilio/twiml \
  -d "CallSid=CAtest&callPublicId=test-public-id&To=%2B32XXXXXXXXX" \
  -H "X-Twilio-Signature: <signature_valide>"
```

Réponse attendue :
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Response>
  <Dial callerId="+32XXXXXXXXX">
    <Number>+32XXXXXXXXX</Number>
  </Dial>
</Response>
```

> **Note :** Sans signature valide, le endpoint retourne 403. Pour tester en développement, utiliser le validateur Twilio ou temporairement désactiver la vérification.

---

## Flux d'appel complet

```
1. Commercial ouvre le CRM
   → CrmShellComponent.ngOnInit()
   → TwilioService.initialize()
   → GET /api/crm/telephony/twilio/token
   → Device.register() — connexion WebRTC à Twilio Edge

2. Commercial clique sur un numéro de téléphone
   → CallFacade.initiate(phoneNumber, contactId)
   → POST /api/crm/calls  [provider=TWILIO, status=INITIATED]
   → device.connect({ params: { To: "+32...", callPublicId: "uuid" } })

3. Twilio appelle notre TwiML App
   → POST /api/crm/telephony/twilio/twiml
   → registerExternalCallId(callPublicId, CallSid)  [correlation webhook]
   → Retourne TwiML <Dial><Number>+32...</Number></Dial>

4. Twilio appelle le téléphone du client
   → Webhook POST /api/crm/telephony/twilio/webhook  [CallStatus=ringing]
   → callService.ring()  →  session RINGING

5. Client décroche
   → Webhook  [CallStatus=in-progress]
   → callService.answer()  →  session ACTIVE + answeredAt=now()
   → call.on('accept') frontend  →  store ACTIVE + timer démarre

6. Fin d'appel (raccroché par commercial OU client)
   → call.on('disconnect') frontend
   → PATCH /api/crm/calls/{id}/terminate  [status=ENDED]
   → TwilioAdapter.terminate()  →  durationSeconds calculé depuis answeredAt
   → CallTerminatedEvent  →  Interaction créée dans le CRM

7. Webhook [CallStatus=completed] arrive (peut arriver avant ou après l'étape 6)
   → Si session déjà terminée : ignoré silencieusement
```

---

## Développement local avec ngrok

Pour recevoir les webhooks Twilio en développement :

```bash
# Démarrer le backend
./mvnw spring-boot:run

# Dans un autre terminal, exposer le port HTTPS du backend
ngrok http https://localhost:8443 --host-header=rewrite
```

ngrok affiche une URL publique comme `https://xxxx-xx-xx.ngrok.io`.

1. Mettre à jour la TwiML App dans Twilio Console avec cette URL (Étapes 2 et 3)
2. Ajouter dans `.env` : `TWILIO_WEBHOOK_BASE_URL=https://xxxx-xx-xx.ngrok.io`
3. Redémarrer le backend

---

## Résolution de problèmes

| Symptôme | Cause probable | Solution |
|---|---|---|
| `GET /token` retourne 404 | Aucune config TWILIO active | Vérifier via `GET /api/crm/telephony/configs/active` |
| `POST /twiml` retourne 403 | Signature X-Twilio-Signature invalide | Vérifier `TWILIO_WEBHOOK_BASE_URL` + Auth Token |
| Device ne se registre pas | Token expiré ou twimlAppSid incorrect | Recharger le CRM (nouveau token) + vérifier l'App SID |
| Appel initié mais pas de son | Permissions microphone navigateur refusées | Autoriser le micro dans les paramètres du navigateur |
| Pas d'Interaction créée après appel | Webhook non reçu | Vérifier les logs Twilio Console → Calls → See All Calls → Webhooks |
| `CallDuration` absent dans webhook | Appel non répondu (missed/failed) | Normal — la durée reste null pour les appels sans réponse |

---

## Sécurité

| Point | Détail |
|---|---|
| Credentials chiffrés | AES-256/GCM en base, jamais exposés dans les réponses API |
| Access Token | TTL 1 heure, outbound-only (pas d'appels entrants) |
| Webhooks | Vérifiés via HMAC-SHA1 (`X-Twilio-Signature`) — rejets 403 si signature invalide |
| Auth Token | Utilisé uniquement pour valider les signatures, jamais envoyé au frontend |
| API Key vs Auth Token | L'API Key peut être révoquée indépendamment — bonne pratique de rotation |
