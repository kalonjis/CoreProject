# Asterisk — Configuration SIP/WebRTC pour l'intégration CRM

## Vue d'ensemble

Ce document couvre la configuration complète d'Asterisk pour l'intégration téléphonie du CRM.  
Le CRM utilise **SIP.js** côté browser (WebRTC) pour passer les appels directement depuis l'application.  
Asterisk sert de pont entre le browser (WSS) et l'opérateur télécom (SIP trunk UDP).

```
Browser (SIP.js)
    ↕ WSS (port 8089)
Asterisk (PJSIP + WebRTC)
    ↕ SIP/UDP (port 5060)
Opérateur SIP trunk (OVH / Destiny / Proximus / Orange)
    ↕
Téléphone du client
```

---

## Prérequis

| Élément | Détail |
|---|---|
| Version Asterisk | 18 LTS ou 20 LTS |
| Modules requis | `res_pjsip`, `res_pjsip_session`, `res_http_websocket`, `codec_opus` |
| Certificat TLS | Let's Encrypt recommandé (obligatoire pour WSS en production) |
| Ports à ouvrir | `5060/UDP` (SIP trunk), `8089/TCP` (WSS browser), `10000-20000/UDP` (RTP) |

### Vérifier les modules disponibles

```bash
asterisk -rx "module show like res_pjsip"
asterisk -rx "module show like res_http_websocket"
asterisk -rx "module show like codec_opus"
```

---

## 1. `http.conf` — Serveur HTTP Asterisk (WebSocket)

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

Le browser SIP.js se connectera à `wss://ton-pbx.com:8089/ws`.

---

## 2. `pjsip.conf` — Transports

```ini
; ── WebSocket sécurisé : browser SIP.js → Asterisk ────────────────────
[transport-wss]
type=transport
protocol=wss
bind=0.0.0.0:8089
cert_file=/etc/asterisk/keys/asterisk.crt
priv_key_file=/etc/asterisk/keys/asterisk.key

; ── UDP : Asterisk → SIP trunk opérateur ──────────────────────────────
[transport-udp]
type=transport
protocol=udp
bind=0.0.0.0
```

---

## 3. `pjsip.conf` — Extension par commercial (WebRTC)

Répéter ce bloc pour chaque commercial.  
Le `username` correspond au `CommercialSipConfig.sipUsername` stocké dans le CRM.  
Le `password` correspond au `CommercialSipConfig.sipPassword` (en clair dans Asterisk, chiffré dans le CRM).

```ini
; ── Exemple pour le commercial avec sipUsername = "1001" ──────────────

[1001]
type=endpoint
transport=transport-wss
context=from-internal
disallow=all
allow=opus
allow=ulaw
; Obligatoire pour WebRTC (DTLS-SRTP + ICE)
webrtc=yes
dtls_cert_file=/etc/asterisk/keys/asterisk.crt
dtls_private_key=/etc/asterisk/keys/asterisk.key
dtls_verify=fingerprint
dtls_setup=actpass
ice_support=yes
auth=auth-1001
aors=aor-1001

[auth-1001]
type=auth
auth_type=userpass
username=1001
password=MotDePasseCommercial

[aor-1001]
type=aor
max_contacts=1
remove_existing=yes
```

---

## 4. `pjsip.conf` — SIP trunk OVH

Adapter `TON_NUMERO` et `MOT_DE_PASSE_OVH` selon les credentials fournis par OVH.

```ini
[ovh-registration]
type=registration
transport=transport-udp
outbound_auth=ovh-auth
server_uri=sip:sip.ovh.net
client_uri=sip:TON_NUMERO@sip.ovh.net
retry_interval=60

[ovh-auth]
type=auth
auth_type=userpass
username=TON_NUMERO
password=MOT_DE_PASSE_OVH

[ovh-trunk]
type=endpoint
transport=transport-udp
context=from-trunk
disallow=all
allow=ulaw
allow=alaw
outbound_auth=ovh-auth
aors=ovh-aors

[ovh-aors]
type=aor
contact=sip:sip.ovh.net
```

### Autres opérateurs

Le bloc ci-dessus fonctionne pour tout SIP trunk standard. Seuls `server_uri` et `client_uri` changent :

| Opérateur | `server_uri` |
|---|---|
| OVH | `sip:sip.ovh.net` |
| Destiny | selon configuration fournie par Destiny |
| Proximus | selon configuration fournie par Proximus |
| Orange BE | selon configuration fournie par Orange |

---

## 5. `extensions.conf` — Dialplan

```ini
[from-internal]
; Commercial appelle un numéro client (format E.164 attendu : +32XXXXXXXXX)
exten => _+.,1,NoOp(Appel sortant CRM — ${CALLERID(num)} vers ${EXTEN})
 same => n,Dial(PJSIP/${EXTEN}@ovh-trunk,60,rU)
 same => n,Hangup()

[from-trunk]
; Appels entrants depuis le SIP trunk (à configurer selon besoin métier)
exten => _.,1,Hangup()
```

---

## 6. Configuration CRM — TelephonyConfig

Une fois Asterisk opérationnel, configurer le provider dans le CRM via :

```
POST /api/crm/telephony/configs
```

Body :
```json
{
  "provider": "SIP",
  "credentialsJson": "{\"wsUrl\":\"wss://ton-pbx.com:8089/ws\",\"sipDomain\":\"ton-pbx.com\",\"outboundContext\":\"from-internal\"}",
  "callerId": "+32XXXXXXXXX",
  "active": true
}
```

Puis assigner une extension SIP à chaque commercial via :

```
POST /api/crm/telephony/sip
```

Body :
```json
{
  "targetUserPublicId": "uuid-du-commercial",
  "sipUsername": "1001",
  "sipPassword": "MotDePasseCommercial",
  "displayName": "John Doe"
}
```

---

## 7. Commandes de diagnostic

```bash
# État des endpoints PJSIP
asterisk -rx "pjsip show endpoints"

# Vérifier la registration OVH
asterisk -rx "pjsip show registrations"

# Lister les canaux actifs
asterisk -rx "core show channels"

# Test d'appel sortant depuis CLI
asterisk -rx "channel originate PJSIP/+32XXXXXXXXX@ovh-trunk application Playback demo-congrats"

# Logs en temps réel
asterisk -rx "core set verbose 5"
asterisk -rvvvvv
```

---

## 8. Certificat TLS (Let's Encrypt)

```bash
# Installer certbot
apt install certbot

# Générer le certificat (remplacer ton-pbx.com)
certbot certonly --standalone -d ton-pbx.com

# Copier dans le répertoire Asterisk
cp /etc/letsencrypt/live/ton-pbx.com/fullchain.pem /etc/asterisk/keys/asterisk.crt
cp /etc/letsencrypt/live/ton-pbx.com/privkey.pem   /etc/asterisk/keys/asterisk.key
chown asterisk:asterisk /etc/asterisk/keys/*

# Renouvellement automatique + rechargement Asterisk
# Ajouter dans /etc/cron.d/certbot-asterisk :
# 0 3 * * * root certbot renew --quiet && asterisk -rx "module reload res_pjsip_transport_websocket"
```
