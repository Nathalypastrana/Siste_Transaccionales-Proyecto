#!/usr/bin/env bash
# Ejecuta las consultas de evidencia en SQL Server (Docker) y guarda el resultado con fecha.
# Uso:  export DB_PASSWORD='...'   (y opcional DB_USER, SQL_CONTENEDOR)
#       ./pruebas/evidencia.sh [numero_de_reserva]
# NO_ARCHIVO=1 imprime sin guardar. La contraseña va por variable de entorno (SQLCMDPASSWORD), nunca en la línea de comandos.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${DB_PASSWORD:?Define DB_PASSWORD (export DB_PASSWORD='...')}"
DB_USER="${DB_USER:-sa}"; CONT="${SQL_CONTENEDOR:-sqlserver}"; RID="${1:-}"
SQLCMD=/opt/mssql-tools18/bin/sqlcmd
docker exec "$CONT" test -x "$SQLCMD" 2>/dev/null || SQLCMD=/opt/mssql-tools/bin/sqlcmd
docker cp pruebas/consultas_evidencia.sql "$CONT":/tmp/consultas_evidencia.sql >/dev/null
run() { docker exec -e SQLCMDPASSWORD="$DB_PASSWORD" "$CONT" "$SQLCMD" -S localhost -U "$DB_USER" -C -W -s " | " -y 40 \
          -i /tmp/consultas_evidencia.sql -v RESERVA_ID="$RID"; }
if [ "${NO_ARCHIVO:-0}" = "1" ]; then run; else
  mkdir -p evidencias; F="evidencias/persistencia_$(date +%Y%m%d_%H%M%S).txt"
  { echo "Evidencia de persistencia - generada $(date '+%Y-%m-%d %H:%M:%S') - commit $(git rev-parse --short HEAD 2>/dev/null || echo n/a)"; run; } | tee "$F"
  echo; echo "Guardado en $F"
fi
