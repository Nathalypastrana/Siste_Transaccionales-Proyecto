#!/usr/bin/env bash
# Prueba de concurrencia: lanza N solicitudes SIMULTÁNEAS para reservar la misma
# habitación en las mismas fechas. Resultado esperado: exactamente UNA con 201
# y las demás con 409 (sin reservas superpuestas).
#
# Requiere un usuario del sistema: export API_CORREO=... API_PASSWORD=...
# Uso:   ./pruebas/concurrencia.sh [N] [habitacionId] [fechaEntrada] [fechaSalida] [huespedId]
# Ej.:   ./pruebas/concurrencia.sh 10 4 2027-03-10 2027-03-12
# Variable opcional: API=http://localhost:8080

set -u

N="${1:-10}"
HAB="${2:-4}"
ENTRADA="${3:-2027-03-10}"
SALIDA="${4:-2027-03-12}"
HUESPED="${5:-1}"
API="${API:-http://localhost:8080}"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

BODY="{\"huespedId\":$HUESPED,\"habitacionId\":$HAB,\"fechaEntrada\":\"$ENTRADA\",\"fechaSalida\":\"$SALIDA\"}"

echo "API:           $API"
echo "Solicitudes:   $N simultáneas"
echo "Habitación:    $HAB   Fechas: $ENTRADA -> $SALIDA"
echo "------------------------------------------------------------"

if ! curl -s -o /dev/null --max-time 5 "$API/api/salud"; then
  echo "No se pudo conectar con $API. ¿Está corriendo el backend?"
  exit 2
fi

if [ -z "${API_CORREO:-}" ] || [ -z "${API_PASSWORD:-}" ]; then
  echo "Define API_CORREO y API_PASSWORD (un usuario del sistema) antes de ejecutar la prueba."
  exit 2
fi
TOKEN="$(curl -s --max-time 10 -X POST "$API/api/auth/login" -H "Content-Type: application/json" \
  -d "{\"correo\":\"$API_CORREO\",\"password\":\"$API_PASSWORD\"}" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)"
if [ -z "$TOKEN" ]; then
  echo "No se pudo iniciar sesión: revisa API_CORREO y API_PASSWORD."
  exit 2
fi

# Lanza todas las solicitudes en paralelo
for i in $(seq 1 "$N"); do
  (
    curl -s -o "$TMP/resp_$i.json" -w "%{http_code}" --max-time 60 \
      -X POST "$API/api/reservas" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d "$BODY" > "$TMP/code_$i.txt"
  ) &
done
wait

CREADAS=0
CONFLICTOS=0
OTROS=0
for i in $(seq 1 "$N"); do
  CODE="$(cat "$TMP/code_$i.txt" 2>/dev/null || echo 000)"
  case "$CODE" in
    201) CREADAS=$((CREADAS + 1)); ID="$(grep -o '"id": *[0-9]*' "$TMP/resp_$i.json" | head -1)"
         echo "Solicitud $i -> 201 Created  ($ID)" ;;
    409) CONFLICTOS=$((CONFLICTOS + 1))
         echo "Solicitud $i -> 409 Conflict (rechazada)" ;;
    *)   OTROS=$((OTROS + 1))
         echo "Solicitud $i -> $CODE (inesperado): $(head -c 200 "$TMP/resp_$i.json" 2>/dev/null)" ;;
  esac
done

echo "------------------------------------------------------------"
echo "Creadas (201):     $CREADAS"
echo "Rechazadas (409):  $CONFLICTOS"
echo "Otros códigos:     $OTROS"

if [ "$CREADAS" -eq 1 ] && [ "$OTROS" -eq 0 ]; then
  echo "RESULTADO: OK - solo una reserva fue confirmada, no hubo superposición."
  exit 0
fi

if [ "$CREADAS" -eq 0 ] && [ "$CONFLICTOS" -eq "$N" ]; then
  echo "RESULTADO: todas rechazadas. La habitación ya estaba reservada en esas fechas;"
  echo "           cambia las fechas o la habitación y repite la prueba."
  exit 3
fi

echo "RESULTADO: FALLO - revisar (se esperaba exactamente un 201)."
exit 1
