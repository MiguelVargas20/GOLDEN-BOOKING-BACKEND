#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────
# Respaldo diario de MongoDB para Golden Booking.
#
# Genera un archivo comprimido con toda la base (usuarios, reservas,
# habitaciones, espacios e imágenes de GridFS) y borra los respaldos
# más viejos que RETENCION_DIAS.
#
# Uso manual:     ./scripts/backup-mongo.sh
# Programado:     ver scripts/README-respaldos.md (cron a las 2:30 a. m.)
#
# Variables (opcionales):
#   MONGODB_URI      conexión (por defecto mongodb://localhost:27017/goldenbooking)
#   BACKUP_DIR       carpeta destino (por defecto /var/backups/goldenbooking)
#   RETENCION_DIAS   días que se guardan los respaldos (por defecto 14)
# ─────────────────────────────────────────────────────────────────────
set -euo pipefail

MONGODB_URI="${MONGODB_URI:-mongodb://localhost:27017/goldenbooking}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/goldenbooking}"
RETENCION_DIAS="${RETENCION_DIAS:-14}"

FECHA="$(date +%Y-%m-%d_%H%M)"
ARCHIVO="${BACKUP_DIR}/goldenbooking_${FECHA}.archive.gz"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

if ! command -v mongodump >/dev/null 2>&1; then
  log "ERROR: mongodump no está instalado (paquete mongodb-database-tools)."
  exit 1
fi

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"   # los respaldos contienen datos personales

log "Iniciando respaldo → ${ARCHIVO}"
# Se escribe primero a un .tmp: si falla a la mitad, no queda un respaldo
# incompleto que parezca válido.
if mongodump --uri="$MONGODB_URI" --archive="${ARCHIVO}.tmp" --gzip --quiet; then
  mv "${ARCHIVO}.tmp" "$ARCHIVO"
  chmod 600 "$ARCHIVO"
  log "Respaldo OK ($(du -h "$ARCHIVO" | cut -f1))."
else
  rm -f "${ARCHIVO}.tmp"
  log "ERROR: falló mongodump. No se borró ningún respaldo anterior."
  exit 1
fi

# Solo se limpian los viejos cuando el respaldo nuevo salió bien
BORRADOS=$(find "$BACKUP_DIR" -name 'goldenbooking_*.archive.gz' -type f -mtime +"$RETENCION_DIAS" -print -delete | wc -l)
log "Respaldos antiguos eliminados (más de ${RETENCION_DIAS} días): ${BORRADOS}."
