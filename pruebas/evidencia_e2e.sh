#!/usr/bin/env bash
# Prueba extremo a extremo por API: crear -> modificar -> cancelar una reserva y comprobar en BD tras cada paso.
# Uso: export API_CORREO=... API_PASSWORD=... DB_PASSWORD=...; ./pruebas/evidencia_e2e.sh <idHuesped> <idHabitacion>
set -euo pipefail
cd "$(dirname "$0")/.."
API="${API:-http://localhost:8080}"
: "${API_CORREO:?}" "${API_PASSWORD:?}" "${DB_PASSWORD:?}"
HUESPED="${1:?idHuesped}"; HAB="${2:?idHabitacion}"
fecha() { date -d "+$1 days" +%F 2>/dev/null || date -v+"$1"d +%F; }
IN="${FECHA_ENTRADA:-$(fecha 30)}"; OUT="${FECHA_SALIDA:-$(fecha 32)}"; IN2="$(fecha 31)"
mkdir -p evidencias; F="evidencias/e2e_$(date +%Y%m%d_%H%M%S).txt"
{
echo "== E2E $(date '+%F %T') =="
TOKEN="$(curl -s -X POST "$API/api/auth/login" -H "Content-Type: application/json" \
  -d "{\"correo\":\"$API_CORREO\",\"password\":\"$API_PASSWORD\"}" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)"
[ -n "$TOKEN" ] || { echo "Login fallido"; exit 1; }
H=(-H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")
echo "-- 1. CREAR ($IN a $OUT)"
R="$(curl -s -w ' HTTP %{http_code}' -X POST "$API/api/reservas" "${H[@]}" -d "{\"huespedId\":$HUESPED,\"habitacionId\":$HAB,\"fechaEntrada\":\"$IN\",\"fechaSalida\":\"$OUT\"}")"; echo "$R"
ID="$(echo "$R" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)"; [ -n "$ID" ] || { echo "No se creó la reserva"; exit 1; }
NO_ARCHIVO=1 ./pruebas/evidencia.sh "$ID"
echo "-- 2. MODIFICAR (nueva salida: $IN2)"
curl -s -w ' HTTP %{http_code}\n' -X PATCH "$API/api/reservas/$ID" "${H[@]}" -d "{\"fechaSalida\":\"$IN2\"}"
NO_ARCHIVO=1 ./pruebas/evidencia.sh "$ID"
echo "-- 3. CANCELAR"
curl -s -w ' HTTP %{http_code}\n' -X PATCH "$API/api/reservas/$ID/cancelar" "${H[@]}"
NO_ARCHIVO=1 ./pruebas/evidencia.sh "$ID"
} 2>&1 | tee "$F"
echo "Guardado en $F"
