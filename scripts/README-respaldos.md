# Respaldos de MongoDB

El script `backup-mongo.sh` guarda **toda** la base `goldenbooking` (incluidas
las imágenes de los espacios, que están en GridFS) en un solo archivo
comprimido, y borra los respaldos de más de 14 días.

## 1. Instalar (una sola vez, en el servidor EC2)

```bash
# Herramientas de Mongo (trae mongodump / mongorestore)
sudo apt-get install -y mongodb-database-tools   # o el paquete de tu distro
mongodump --version

# Carpeta de respaldos (solo el usuario que corre el backend puede leerla)
sudo mkdir -p /var/backups/goldenbooking
sudo chown "$USER" /var/backups/goldenbooking

# Primera prueba manual (desde la carpeta del backend)
./scripts/backup-mongo.sh
ls -lh /var/backups/goldenbooking
```

> Si Mongo tiene usuario y contraseña, exporta la misma `MONGODB_URI` que usa
> el backend antes de correr el script (o ponla en la línea de cron).

## 2. Programarlo todos los días a las 2:30 a. m.

```bash
crontab -e
```

Agrega esta línea (cambia la ruta por la de tu clon del backend):

```
30 2 * * * /home/ubuntu/GOLDEN-BOOKING-BACKEND/scripts/backup-mongo.sh >> /var/backups/goldenbooking/respaldo.log 2>&1
```

Revisa que corrió al día siguiente con `tail /var/backups/goldenbooking/respaldo.log`.

## 3. Restaurar un respaldo

```bash
# ⚠️ --drop reemplaza las colecciones actuales por las del respaldo
mongorestore --uri="mongodb://localhost:27017" --gzip \
  --archive=/var/backups/goldenbooking/goldenbooking_2026-09-25_0230.archive.gz --drop
```

Para probar sin tocar la base real, restaura en otra base:

```bash
mongorestore --uri="mongodb://localhost:27017" --gzip --archive=ARCHIVO \
  --nsFrom='goldenbooking.*' --nsTo='goldenbooking_prueba.*'
```

## 4. Recomendado: copia fuera del servidor

Si se daña el disco de la instancia EC2 se pierden la base **y** los
respaldos. Cada cierto tiempo copia el último respaldo a otro lugar, por
ejemplo a tu PC:

```bash
scp -i tu-llave.pem ubuntu@32.194.207.246:/var/backups/goldenbooking/goldenbooking_*.archive.gz .
```

o a un bucket de S3 con `aws s3 cp` (requiere configurar AWS CLI).
