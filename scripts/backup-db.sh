#!/bin/bash
# ============================================================================
# Backup de la base de datos `senderos` (mysqldump dentro del contenedor).
# Uso:        scripts/backup-db.sh [carpeta-destino]
# Por defecto guarda en <proyecto>/backups/backup-YYYY-MM-DD_HHMM.sql
#
# Programar en producción con cron (backup diario a las 2:00 a. m.):
#   crontab -e
#   0 2 * * * /ruta/al/proyecto/scripts/backup-db.sh /var/backups/senderos >> /var/log/backup-db.log 2>&1
#
# Restaurar un backup:
#   docker exec -i senderos-mysql sh -c 'mysql -usenderos -p"$MYSQL_PASSWORD" senderos' < backup-FECHA.sql
# ============================================================================
set -e

FECHA=$(date +%Y-%m-%d_%H%M)
DESTINO="${1:-$(cd "$(dirname "$0")/.." && pwd)/backups}"
ARCHIVO="$DESTINO/backup-$FECHA.sql"

mkdir -p "$DESTINO"

# --single-transaction: dump consistente sin bloquear escrituras (InnoDB).
docker exec senderos-mysql sh -c 'mysqldump -usenderos -p"$MYSQL_PASSWORD" --default-character-set=utf8mb4 --single-transaction --routines --triggers --no-tablespaces senderos' > "$ARCHIVO"

# Verificación mínima: el dump debe terminar con el cierre de volcado.
if ! tail -1 "$ARCHIVO" | grep -q "Dump completed"; then
  echo "ERROR: el backup no parece completo" >&2
  rm -f "$ARCHIVO"
  exit 1
fi

echo "Backup creado: $ARCHIVO ($(du -h "$ARCHIVO" | cut -f1))"

# Retención opcional: borra backups de más de 30 días.
# find "$DESTINO" -name "backup-*.sql" -mtime +30 -delete
