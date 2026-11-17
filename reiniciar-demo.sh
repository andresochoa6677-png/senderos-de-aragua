#!/bin/bash
# Reinicia el PROGRESO EDUCATIVO de la cuenta demo (lecciones, ejercicios
# y práctica vuelven a cero). Conserva su consentimiento y su encuesta
# inicial para no repetirlos en la demostración. No toca a admin ni
# residentes reales.
set -e
# La contraseña no vive en el archivo: la toma del entorno del contenedor.
docker exec -i senderos-mysql sh -c 'mysql -usenderos -p"$MYSQL_PASSWORD" --default-character-set=utf8mb4 senderos' <<'SQL'
START TRANSACTION;
DELETE FROM intento_ejercicio WHERE vivienda_id = (SELECT id FROM vivienda WHERE demo = 1 LIMIT 1);
DELETE FROM progreso_leccion WHERE vivienda_id = (SELECT id FROM vivienda WHERE demo = 1 LIMIT 1);
DELETE FROM intento_practica WHERE vivienda_id = (SELECT id FROM vivienda WHERE demo = 1 LIMIT 1);
COMMIT;
SELECT 'progreso_lecciones' AS dato, COUNT(*) AS cantidad FROM progreso_leccion p JOIN vivienda v ON v.id=p.vivienda_id WHERE v.demo=1
UNION ALL SELECT 'intentos_ejercicios', (SELECT COUNT(*) FROM intento_ejercicio i JOIN vivienda v ON v.id=i.vivienda_id WHERE v.demo=1)
UNION ALL SELECT 'intentos_practica', (SELECT COUNT(*) FROM intento_practica ip JOIN vivienda v ON v.id=ip.vivienda_id WHERE v.demo=1);
SQL
echo "Demo reiniciada: entra con demo@senderos.local / demo1234"
