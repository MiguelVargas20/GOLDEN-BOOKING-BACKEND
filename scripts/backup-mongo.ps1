# ---------------------------------------------------------------------
# Respaldo de MongoDB para Golden Booking (servidor Windows).
#
# Genera un archivo comprimido con toda la base (usuarios, reservas,
# habitaciones, espacios, eventos, consumos e imagenes de GridFS) y borra
# los respaldos mas viejos que RetencionDias.
#
# Uso manual (PowerShell como administrador):
#   powershell -ExecutionPolicy Bypass -File C:\apps\GOLDEN-BOOKING-BACKEND\scripts\backup-mongo.ps1
#
# La conexion se toma de MONGODB_URI en el .env del backend; si no existe,
# se usa la base local mongodb://localhost:27017/goldenbooking.
# ---------------------------------------------------------------------
param(
    [string]$Repo = "C:\apps\GOLDEN-BOOKING-BACKEND",
    [string]$Destino = "C:\respaldos\goldenbooking",
    [int]$RetencionDias = 14
)

$ErrorActionPreference = "Stop"
function Log($texto) { Write-Output ("[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $texto) }

# mongodump: en el PATH o en la carpeta de instalacion de MongoDB Database Tools
$mongodump = (Get-Command mongodump -ErrorAction SilentlyContinue).Source
if (-not $mongodump) {
    $mongodump = Get-ChildItem "C:\Program Files\MongoDB", "C:\mongodb-tools" -Recurse -Filter mongodump.exe -ErrorAction SilentlyContinue |
                 Select-Object -First 1 -ExpandProperty FullName
}
if (-not $mongodump) {
    Log "ERROR: no se encontro mongodump.exe (instala MongoDB Database Tools)."
    exit 1
}

# Misma conexion que usa el backend
$uri = "mongodb://localhost:27017/goldenbooking"
$archivoEnv = Join-Path $Repo ".env"
if (Test-Path $archivoEnv) {
    $linea = Get-Content $archivoEnv | Where-Object { $_ -match '^\s*MONGODB_URI\s*=' } | Select-Object -First 1
    if ($linea) { $uri = ($linea -split '=', 2)[1].Trim().Trim('"') }
}

New-Item -ItemType Directory -Force $Destino | Out-Null
$fecha = Get-Date -Format "yyyy-MM-dd_HHmm"
$archivo = Join-Path $Destino ("goldenbooking_{0}.archive.gz" -f $fecha)
$temporal = $archivo + ".tmp"

Log "Iniciando respaldo -> $archivo"
# Se escribe primero a un .tmp: si falla a la mitad, no queda un respaldo
# incompleto que parezca valido.
& $mongodump ("--uri=" + $uri) ("--archive=" + $temporal) --gzip --quiet
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $temporal)) {
    Remove-Item $temporal -ErrorAction SilentlyContinue
    Log "ERROR: fallo mongodump. No se borro ningun respaldo anterior."
    exit 1
}
Move-Item $temporal $archivo -Force
$tamano = "{0:N1} MB" -f ((Get-Item $archivo).Length / 1MB)
Log "Respaldo OK ($tamano)."

# Solo se limpian los viejos cuando el respaldo nuevo salio bien
$viejos = Get-ChildItem $Destino -Filter "goldenbooking_*.archive.gz" |
          Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetencionDias) }
$viejos | Remove-Item -Force
Log ("Respaldos antiguos eliminados (mas de {0} dias): {1}." -f $RetencionDias, @($viejos).Count)
