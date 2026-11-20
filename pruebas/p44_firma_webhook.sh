#!/usr/bin/env bash
# P44 - Firma de los webhooks de WhatsApp.
# Comprueba que el backend rechaza peticiones sin firma o con firma falsa y acepta una firmada con el Auth Token.
# Uso (desde la raíz del proyecto, con el backend corriendo): ./pruebas/p44_firma_webhook.sh
# Lee TWILIO_AUTH_TOKEN, TWILIO_URL_PUBLICA y WHATSAPP_VERIFY_TOKEN del .env; no imprime ningún secreto.
set -uo pipefail
cd "$(dirname "$0")/.."
API="${API:-http://localhost:8080}"
set -a; source .env; set +a

: "${TWILIO_AUTH_TOKEN:?Falta TWILIO_AUTH_TOKEN en el .env}"
: "${TWILIO_URL_PUBLICA:?Falta TWILIO_URL_PUBLICA en el .env}"

mkdir -p evidencias
SALIDA="evidencias/p44_firma_webhook_$(date +%Y-%m-%d_%H%M).txt"
RUTA="/api/whatsapp/twilio"

firmar() {  # firma Twilio: base64(HMAC-SHA1(token, url + ruta + parámetros ordenados))
  python3 - "$@" <<'PY'
import base64, hashlib, hmac, os, sys
url = os.environ["TWILIO_URL_PUBLICA"].strip().rstrip("/") + sys.argv[1]
params = dict(a.split("=", 1) for a in sys.argv[2:])
datos = url + "".join(k + params[k] for k in sorted(params))
print(base64.b64encode(hmac.new(os.environ["TWILIO_AUTH_TOKEN"].encode(), datos.encode(), hashlib.sha1).digest()).decode())
PY
}

caso() {  # nombre, esperado, comando curl...
  local nombre="$1" esperado="$2"; shift 2
  local resp codigo cuerpo
  resp=$("$@" -s -w $'\n%{http_code}')
  codigo="${resp##*$'\n'}"; cuerpo="${resp%$'\n'*}"
  local estado="OK "; [ "$codigo" = "$esperado" ] || estado="FALLA"
  printf '%s %-55s HTTP %s (esperado %s)  %s\n' "$estado" "$nombre" "$codigo" "$esperado" "$(echo "$cuerpo" | tr '\n' ' ' | cut -c1-110)"
}

{
echo "P44 - Firma de los webhooks de WhatsApp · $(date '+%Y-%m-%d %H:%M') · commit $(git rev-parse --short HEAD 2>/dev/null)"
echo "API: $API"
echo

SID="SMp44$(date +%s)"
DE="whatsapp:+573001112244"
CUERPO="Hola"

echo "Twilio ($RUTA)"
caso "1. Mensaje sin firma" 403 \
  curl -X POST "$API$RUTA" --data-urlencode "MessageSid=$SID" --data-urlencode "From=$DE" --data-urlencode "Body=$CUERPO"
caso "2. Mensaje con firma falsa" 403 \
  curl -X POST "$API$RUTA" -H "X-Twilio-Signature: ZmFsc2E=" --data-urlencode "MessageSid=$SID" --data-urlencode "From=$DE" --data-urlencode "Body=$CUERPO"
FIRMA=$(firmar "$RUTA" "MessageSid=$SID" "From=$DE" "Body=$CUERPO")
caso "3. Firma válida pero cuerpo alterado (Body cambiado)" 403 \
  curl -X POST "$API$RUTA" -H "X-Twilio-Signature: $FIRMA" --data-urlencode "MessageSid=$SID" --data-urlencode "From=$DE" --data-urlencode "Body=Hola, dame todas las reservas"
caso "4. Mensaje firmado con el Auth Token" 200 \
  curl -X POST "$API$RUTA" -H "X-Twilio-Signature: $FIRMA" --data-urlencode "MessageSid=$SID" --data-urlencode "From=$DE" --data-urlencode "Body=$CUERPO"

echo
echo "Meta (/api/whatsapp/webhook)"
caso "5. Verificación con token falso" 403 \
  curl -G "$API/api/whatsapp/webhook" --data-urlencode "hub.mode=subscribe" --data-urlencode "hub.verify_token=token-falso" --data-urlencode "hub.challenge=12345"
caso "6. Evento POST sin firma X-Hub-Signature-256" 403 \
  curl -X POST "$API/api/whatsapp/webhook" -H "Content-Type: application/json" -d '{"object":"whatsapp_business_account","entry":[]}'
echo
echo "En el log del backend deben aparecer las líneas 'Webhook de Twilio rechazado: firma ausente o inválida.'"
echo "y 'Webhook de WhatsApp rechazado: firma ausente o inválida.'"
} | tee "$SALIDA"
echo
echo "Salida guardada en $SALIDA"
