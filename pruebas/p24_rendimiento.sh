#!/usr/bin/env bash
# P24 - Prueba de rendimiento completa en un solo paso:
#   1) carga los datos de volumen (100 habitaciones P-xxx y 10.000 reservas),
#   2) ejecuta pruebas/rendimiento.py y guarda la salida en evidencias/,
#   3) elimina los datos de volumen al terminar (aunque la prueba falle).
# Requisitos: backend corriendo (idealmente con NOTIFICACIONES_CANALES vacío), contenedor Docker "sqlserver",
# y las variables SA_PASSWORD (clave de sa), API_CORREO y API_PASSWORD (usuario que puede crear reservas).
set -u
cd "$(dirname "$0")/.."

for v in SA_PASSWORD API_CORREO API_PASSWORD; do
  if [ -z "${!v:-}" ]; then echo "Falta la variable $v. Defínela y vuelve a ejecutar."; exit 2; fi
done
if ! curl -s --max-time 5 http://localhost:8080/api/salud > /dev/null; then
  echo "El backend no responde en http://localhost:8080. Arráncalo y vuelve a ejecutar."; exit 2
fi

SQLCMD=(docker exec -e SQLCMDPASSWORD="$SA_PASSWORD" sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -d ReservasHotelDB)
SALIDA="evidencias/rendimiento_$(date +%Y-%m-%d_%H%M).txt"

limpiar() {
  echo; echo "== Limpiando los datos de rendimiento =="
  "${SQLCMD[@]}" -Q "
SET NOCOUNT ON;
DELETE c FROM Comprobante c JOIN Reserva r ON r.idReserva = c.idReserva JOIN Habitacion h ON h.idHabitacion = r.idHabitacion WHERE h.numero LIKE 'P-%';
DELETE r FROM Reserva r JOIN Habitacion h ON h.idHabitacion = r.idHabitacion WHERE h.numero LIKE 'P-%';
DELETE FROM Habitacion WHERE numero LIKE 'P-%';
SELECT COUNT(*) AS habitaciones_P_restantes FROM Habitacion WHERE numero LIKE 'P-%';"
}
trap limpiar EXIT

echo "== Cargando datos de volumen =="
docker cp database/datos_rendimiento.sql sqlserver:/tmp/datos_rendimiento.sql > /dev/null
"${SQLCMD[@]}" -i /tmp/datos_rendimiento.sql
"${SQLCMD[@]}" -Q "SET NOCOUNT ON; SELECT (SELECT COUNT(*) FROM Habitacion WHERE numero LIKE 'P-%') AS habitaciones_P, (SELECT COUNT(*) FROM Reserva) AS reservas_totales;"

echo; echo "== Ejecutando la prueba de rendimiento (unos 2 minutos) =="
{
  echo "P24 - Prueba de rendimiento · $(date '+%Y-%m-%d %H:%M') · commit $(git rev-parse --short HEAD 2>/dev/null)"
  python3 pruebas/rendimiento.py "$@"
} 2>&1 | tee "$SALIDA"
echo; echo "Salida guardada en $SALIDA"
