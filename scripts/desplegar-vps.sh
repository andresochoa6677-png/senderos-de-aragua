#!/bin/bash
# ============================================================================
# desplegar-vps.sh — Despliegue completo de Senderos de Aragua en el VPS
#
# QUÉ HACE (idempotente: puede re-ejecutarse sin romper nada):
#   1. Instala dependencias: Java 21, Maven 3.9.9, Node 20, Docker, Nginx,
#      certbot, git, ufw, rsync.
#   2. Prepara y valida /opt/senderos/senderos.env (desde
#      .env.produccion.example del repo; NUNCA sobrescribe si ya existe).
#   3. Levanta MySQL 8.4 en Docker (volumen persistente, solo escucha en
#      127.0.0.1 — nunca expuesto a Internet).
#   4. Compila frontend y backend; sirve el build con Nginx (/var/www/senderos
#      + proxy /api) y corre el jar como servicio systemd `senderos-backend`
#      (perfil prod, usuario sin privilegios).
#   5. Si el DNS del dominio ya apunta a este VPS, emite el certificado
#      Let's Encrypt con certbot (HTTPS + redirección automática).
#   6. Programa el backup diario de la BD (cron 2:00 a. m.) con
#      scripts/backup-db.sh y hace un primer backup de verificación.
#
# USO (en el VPS, como root, desde la copia del repo):
#   sudo bash scripts/desplegar-vps.sh            # despliegue completo
#   sudo bash scripts/desplegar-vps.sh verificar  # sólo verificación final
#
# ANTES DE EJECUTAR: subir el repo al VPS (rsync o git clone), copiar
# .env.produccion.example a /opt/senderos/senderos.env y llenar TODOS los
# valores CHANGE_ME. El script aborta si quedan placeholders o si falta el
# archivo (y lo crea como plantilla para que lo edites).
#
# Arquitectura resultante:
#   Usuario → https://miespacioeco.com (Nginx :443)
#               ├─ /            → /var/www/senderos (build React/Vite)
#               └─ /api/*       → http://127.0.0.1:8080 (jar Spring Boot)
#                                    └─ 127.0.0.1:3306 → Docker MySQL 8.4
# ============================================================================
set -euo pipefail

# --- Configuración (sobrescribible por variables de entorno) -----------------
DOMINIO="${DOMINIO:-miespacioeco.com}"
VPS_IP="${VPS_IP:-2.24.213.7}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_EJEMPLO="$RAIZ/.env.produccion.example"
ENV_PROD="/opt/senderos/senderos.env"
JAR_PROD="/opt/senderos/senderos-backend.jar"
WEB_RAIZ="/var/www/senderos"
DIR_BACKUPS="/var/backups/senderos"
SERVICIO="senderos-backend"
MAVEN_VERSION="3.9.9"

# --- Helpers ------------------------------------------------------------------
paso()  { printf '\n\033[1;32m===== %s =====\033[0m\n' "$*"; }
info()  { printf '\033[0;36m· %s\033[0m\n' "$*"; }
warn()  { printf '\033[0;33m⚠ %s\033[0m\n' "$*"; }
fatal() { printf '\033[0;31m✗ ERROR: %s\033[0m\n' "$*" >&2; exit 1; }

# Lee una variable del archivo de producción (sin revelar secretos).
get_env_val() {
  grep -E "^$1=" "$ENV_PROD" 2>/dev/null | head -1 | cut -d= -f2- | tr -d '"' || true
}

# ============================================================================
# MODO VERIFICACIÓN: bash scripts/desplegar-vps.sh verificar
# ============================================================================
verificar() {
  local fallos=0
  local ip_dns estado

  echo "Verificación de despliegue — $(date)"
  echo "Dominio: $DOMINIO · IP esperada del VPS: $VPS_IP"

  ip_dns="$(getent ahostsv4 "$DOMINIO" 2>/dev/null | awk '{print $1; exit}' || true)"
  if [ "$ip_dns" = "$VPS_IP" ]; then
    info "DNS: $DOMINIO → $ip_dns ✓"
  else
    warn "DNS: $DOMINIO resuelve a '${ip_dns:-nada}' (esperado $VPS_IP). Falta propagación o el registro."
    fallos=$((fallos+1))
  fi

  if systemctl is-active --quiet "$SERVICIO"; then
    info "Servicio backend ($SERVICIO): activo ✓"
  else
    warn "Servicio backend ($SERVICIO): INACTIVO — ver journalctl -u $SERVICIO -n 50"
    fallos=$((fallos+1))
  fi

  estado="$(docker inspect -f '{{.State.Health.Status}}' senderos-mysql 2>/dev/null || echo ausente)"
  if [ "$estado" = "healthy" ]; then
    info "Contenedor MySQL: healthy ✓"
  else
    warn "Contenedor MySQL: $estado"
    fallos=$((fallos+1))
  fi

  if curl -fsS -o /dev/null "http://127.0.0.1:8080/api/config"; then
    info "Backend local (127.0.0.1:8080/api/config): responde ✓"
  else
    warn "Backend local no responde en :8080"
    fallos=$((fallos+1))
  fi

  if curl -fsS -o /dev/null "https://$DOMINIO/"; then
    info "HTTPS https://$DOMINIO/: OK ✓"
  else
    warn "HTTPS aún no funciona (¿DNS sin propagar, certbot pendiente o Nginx caído?)"
    fallos=$((fallos+1))
  fi

  if curl -fsS "https://$DOMINIO/api/config" 2>/dev/null | grep -q mailCaptura; then
    info "API pública https://$DOMINIO/api/config: OK ✓"
  else
    warn "API pública no responde vía HTTPS"
    fallos=$((fallos+1))
  fi

  if command -v certbot >/dev/null && certbot certificates 2>/dev/null | grep -q "$DOMINIO"; then
    info "Certificado Let's Encrypt para $DOMINIO: presente ✓"
  else
    warn "Sin certificado para $DOMINIO (ejecuta de nuevo este script cuando el DNS apunte al VPS)"
    fallos=$((fallos+1))
  fi

  if systemctl is-active --quiet nginx; then
    info "Nginx: activo ✓"
  else
    warn "Nginx: INACTIVO"
    fallos=$((fallos+1))
  fi

  if [ -f /etc/cron.d/senderos-backup ]; then
    info "Backup diario programado (cron) ✓"
  else
    warn "Backup diario NO programado"
    fallos=$((fallos+1))
  fi

  local ultimo_backup
  ultimo_backup="$(ls -1t "$DIR_BACKUPS" 2>/dev/null | head -1 || true)"
  if [ -n "$ultimo_backup" ]; then
    info "Último backup: $ultimo_backup"
  else
    warn "Aún no hay backups en $DIR_BACKUPS"
  fi

  df -h / | tail -1 | awk '{print "· Disco raíz: " $3 " usados de " $2 " (" $5 ")"}'

  if [ "$fallos" -gt 0 ]; then
    warn "VERIFICACIÓN: $fallos comprobación(es) pendientes — revisa los ⚠ de arriba."
    return 1
  fi
  info "VERIFICACIÓN: TODO OK — la plataforma está servida en https://$DOMINIO"
}

# ============================================================================
# 1. Paquetes base + zona horaria + swap de protección
# ============================================================================
instalar_base() {
  paso "1/9 · Paquetes base"
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -qq
  apt-get install -y -qq curl git ufw rsync ca-certificates unzip tar >/dev/null
  info "curl, git, ufw, rsync instalados"

  timedatectl set-timezone America/Caracas 2>/dev/null \
    && info "Zona horaria del VPS: America/Caracas" \
    || warn "No se pudo fijar la zona horaria (continúa con la actual)"

  # Swap de protección: compilar Spring Boot + Vite en un VPS pequeño puede
  # agotar la RAM. Crea 2G si no hay al menos 1G de swap ya configurado.
  local swap_actual
  swap_actual="$(free -m | awk '/^Swap:/{print $2}')"
  if [ "${swap_actual:-0}" -lt 1024 ] && [ ! -f /swapfile ]; then
    info "Swap insuficiente (${swap_actual:-0} MB): creando /swapfile de 2G"
    fallocate -l 2G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile >/dev/null
    swapon /swapfile
    grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  fi
}

# ============================================================================
# 2. Java 21 (apt; si no existe, repositorio Temurin de Adoptium)
# ============================================================================
instalar_java() {
  paso "2/9 · Java 21"
  if command -v java >/dev/null 2>&1 && java -version 2>&1 | grep -q 'version "21'; then
    info "Java 21 ya instalado: $(java -version 2>&1 | head -1)"
    return 0
  fi
  if apt-get install -y -qq openjdk-21-jdk-headless >/dev/null 2>&1; then
    info "OpenJDK 21 instalado desde apt"
  else
    info "OpenJDK 21 no está en apt: instalando Temurin 21 (Adoptium)"
    install -d -m 0755 /etc/apt/keyrings
    curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
      | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo "$VERSION_CODENAME") main" \
      > /etc/apt/sources.list.d/adoptium.list
    apt-get update -qq
    apt-get install -y -qq temurin-21-jdk >/dev/null
  fi
  java -version 2>&1 | grep -q 'version "21' || fatal "Java 21 no quedó instalado"
  info "Java 21 OK: $(java -version 2>&1 | head -1)"
}

# ============================================================================
# 3. Maven 3.9.9 (binario oficial en /opt/maven; versión idéntica a la local)
# ============================================================================
instalar_maven() {
  paso "3/9 · Maven $MAVEN_VERSION"
  if [ -x /opt/maven/bin/mvn ] && /opt/maven/bin/mvn -version 2>/dev/null | grep -q "Apache Maven $MAVEN_VERSION"; then
    info "Maven $MAVEN_VERSION ya instalado"
  else
    info "Descargando Maven $MAVEN_VERSION"
    curl -fsSL "https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz" -o /tmp/maven.tgz \
      || curl -fsSL "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz" -o /tmp/maven.tgz \
      || fatal "No se pudo descargar Maven"
    rm -rf /opt/maven /opt/apache-maven-$MAVEN_VERSION
    tar -xzf /tmp/maven.tgz -C /opt
    mv /opt/apache-maven-$MAVEN_VERSION /opt/maven
    rm -f /tmp/maven.tgz
  fi
  ln -sf /opt/maven/bin/mvn /usr/local/bin/mvn
  info "Maven OK: $(mvn -version | head -1)"
}

# ============================================================================
# 4. Node 20 (NodeSource) — necesario para compilar el frontend
# ============================================================================
instalar_node() {
  paso "4/9 · Node.js 20"
  if command -v node >/dev/null 2>&1 \
     && [ "$(node -p 'parseInt(process.versions.node)')" -ge 18 ]; then
    info "Node ya instalado: $(node -v)"
    return 0
  fi
  curl -fsSL "https://deb.nodesource.com/setup_20.x" | bash - >/dev/null
  apt-get install -y -qq nodejs >/dev/null
  info "Node instalado: $(node -v) · npm $(npm -v)"
}

# ============================================================================
# 5. Docker (motor oficial + compose plugin) — para MySQL 8.4
# ============================================================================
instalar_docker() {
  paso "5/9 · Docker"
  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    info "Docker ya instalado: $(docker --version)"
  else
    curl -fsSL https://get.docker.com | sh >/dev/null
    systemctl enable --now docker
  fi
  docker compose version >/dev/null 2>&1 || fatal "docker compose no disponible tras la instalación"
  systemctl is-active --quiet docker || systemctl start docker
  info "Docker activo: $(docker --version)"
}

# ============================================================================
# 6. Firewall (ufw): solo SSH, HTTP y HTTPS abiertos.
#    MySQL (3306) y backend (8080) SOLO escuchan en 127.0.0.1 → sin abertura.
# ============================================================================
configurar_ufw() {
  paso "6/9 · Firewall (ufw)"
  ufw allow OpenSSH >/dev/null
  ufw allow 80/tcp  >/dev/null
  ufw allow 443/tcp >/dev/null
  ufw --force enable >/dev/null
  info "ufw activo: SSH + 80 + 443. MySQL/backend no se publican a Internet."
}

# ============================================================================
# 7. Archivo de producción /opt/senderos/senderos.env
#    - Si no existe: copia la plantilla y ABORTA para que se llene a mano.
#    - Nunca sobrescribe un archivo ya llenado (re-ejecuciones seguras).
# ============================================================================
preparar_env() {
  paso "7/9 · Variables de producción"

  if [ ! -f "$ENV_EJEMPLO" ]; then
    fatal "No encontré $ENV_EJEMPLO — ejecuta este script desde la copia del repo en el VPS"
  fi

  if [ ! -f "$ENV_PROD" ]; then
    install -d -m 755 /opt/senderos
    install -m 600 "$ENV_EJEMPLO" "$ENV_PROD"
    fatal "He copiado la plantilla a $ENV_PROD. Edítala (nano $ENV_PROD), llena TODOS los valores CHANGE_ME y vuelve a ejecutar este script."
  fi
  info "$ENV_PROD presente (no se sobrescribe nunca)"

  local pendientes
  pendientes="$(grep -c 'CHANGE_ME' "$ENV_PROD" || true)"
  if [ "$pendientes" -gt 0 ]; then
    fatal "Quedan $pendientes valor(es) CHANGE_ME en $ENV_PROD — edítalo y vuelve a ejecutar."
  fi

  # Consistencia MySQL↔backend: el usuario/clave de la BD debe coincidir.
  local mysql_user db_user mysql_pass db_pass
  mysql_user="$(get_env_val MYSQL_USER)";  db_user="$(get_env_val DB_USERNAME)"
  mysql_pass="$(get_env_val MYSQL_PASSWORD)"; db_pass="$(get_env_val DB_PASSWORD)"
  [ -n "$mysql_pass" ] && [ -n "$db_pass" ] || fatal "MYSQL_PASSWORD/DB_PASSWORD vacías en $ENV_PROD"
  [ "$mysql_user" = "$db_user" ] || fatal "MYSQL_USER ($mysql_user) ≠ DB_USERNAME ($db_user) — deben ser el mismo usuario"
  [ "$mysql_pass" = "$db_pass" ] || fatal "MYSQL_PASSWORD ≠ DB_PASSWORD — deben coincidir (misma cuenta MySQL)"

  chmod 600 "$ENV_PROD"
  info "Variables validadas (sin placeholders, MySQL↔backend consistentes, permisos 600)"
}

# ============================================================================
# 8. MySQL 8.4 en Docker (usa docker/docker-compose.yml del repo con el
#    archivo de producción como fuente de variables; puerto solo en 127.0.0.1)
# ============================================================================
levantar_mysql() {
  paso "8/9 · MySQL 8.4 (Docker)"
  docker compose --env-file "$ENV_PROD" -f "$RAIZ/docker/docker-compose.yml" up -d

  local estado="desconocido"
  for _ in $(seq 1 60); do
    estado="$(docker inspect -f '{{.State.Health.Status}}' senderos-mysql 2>/dev/null || echo arrancando)"
    [ "$estado" = "healthy" ] && break
    sleep 2
  done
  [ "$estado" = "healthy" ] || fatal "MySQL no llegó a healthy (estado: $estado) — ver: docker logs senderos-mysql"
  info "MySQL healthy · $(docker port senderos-mysql 3306)"
  docker port senderos-mysql 3306 | grep -q '^127.0.0.1' \
    || fatal "MySQL está publicado fuera de 127.0.0.1 — revisa MYSQL_PORT en $ENV_PROD (debe ser 127.0.0.1:3306)"
}

# ============================================================================
# 9a. Frontend: build de producción (Vite) → /var/www/senderos
# ============================================================================
construir_frontend() {
  paso "9/9 · Frontend (build Vite)"
  cd "$RAIZ/frontend"
  if [ -f package-lock.json ]; then
    npm ci --no-audit --no-fund
  else
    npm install --no-audit --no-fund
  fi
  npm run build
  mkdir -p "$WEB_RAIZ"
  rsync -a --delete dist/ "$WEB_RAIZ"/
  info "Build instalado en $WEB_RAIZ ($(du -sh "$WEB_RAIZ" | cut -f1))"
  cd "$RAIZ"
}

# ============================================================================
# 9b. Backend: jar (mvn package, tests ya verificados en local) → systemd
# ============================================================================
construir_backend() {
  paso "9/9 · Backend (jar + servicio systemd)"
  cd "$RAIZ/backend"
  mvn -q -DskipTests package
  local jar_compilado
  jar_compilado="$(ls -1 target/senderos-backend-*.jar 2>/dev/null | grep -v '\.original\.' | head -1 || true)"
  [ -n "$jar_compilado" ] || fatal "No se encontró el jar compilado en backend/target/"
  install -m 644 "$jar_compilado" "$JAR_PROD"

  # Usuario sin privilegios para el proceso
  id -u senderos >/dev/null 2>&1 || useradd --system --no-create-home --shell /usr/sbin/nologin senderos

  local java_bin
  java_bin="$(command -v java)"

  cat > /etc/systemd/system/${SERVICIO}.service <<UNIT
[Unit]
Description=Senderos de Aragua - backend (Spring Boot, perfil prod)
After=network-online.target docker.service
Wants=network-online.target

[Service]
User=senderos
Group=senderos
WorkingDirectory=/opt/senderos
EnvironmentFile=${ENV_PROD}
ExecStart=${java_bin} -Xms256m -Xmx768m -jar ${JAR_PROD} --spring.profiles.active=prod --server.forward-headers-strategy=native
SuccessExitStatus=143
Restart=on-failure
RestartSec=5
NoNewPrivileges=true
ProtectSystem=full
ProtectHome=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
UNIT

  systemctl daemon-reload
  systemctl enable "$SERVICIO" >/dev/null
  systemctl restart "$SERVICIO"
  info "Servicio $SERVICIO reiniciado (Flyway aplicará migraciones si hace falta)"

  local listo=""
  for _ in $(seq 1 90); do
    if curl -fsS -o /dev/null http://127.0.0.1:8080/api/config 2>/dev/null; then listo=1; break; fi
    sleep 2
  done
  [ -n "$listo" ] || fatal "El backend no responde en :8080 tras 3 min — ver: journalctl -u $SERVICIO -n 80"
  info "Backend activo y respondiendo en 127.0.0.1:8080"
  cd "$RAIZ"
}

# ============================================================================
# 9c. Nginx: SPA + proxy /api → backend (HTTP por ahora; certbot añade TLS)
# ============================================================================
configurar_nginx() {
  paso "9/9 · Nginx (SPA + proxy /api)"
  apt-get install -y -qq nginx certbot python3-certbot-nginx >/dev/null

  cat > /etc/nginx/sites-available/senderos <<NGINX
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name ${DOMINIO};

    root ${WEB_RAIZ};
    index index.html;

    add_header X-Frame-Options DENY always;
    add_header X-Content-Type-Options nosniff always;
    add_header Referrer-Policy strict-origin-when-cross-origin always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    gzip on;
    gzip_comp_level 5;
    gzip_min_length 1024;
    gzip_types text/css application/javascript application/json image/svg+xml;

    client_max_body_size 2m;

    # API → backend Spring Boot. X-Forwarded-For se SOBREESCRIBE (no añade)
    # para que la IP registrada en consentimientos/rate-limit no sea falsificable.
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$remote_addr;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 60s;
    }

    # Activos con hash de contenido → caché larga
    location /assets/ {
        expires 30d;
        add_header Cache-Control "public, immutable";
        try_files \$uri =404;
    }

    # SPA: cualquier ruta no encontrada cae en index.html (router de React).
    # index.html NUNCA se cachea: evita que navegadores con un bundle viejo
    # pidan assets con hashes que ya no existen (pantalla en blanco).
    location / {
        try_files \$uri /index.html;
        expires -1;
    }
}
NGINX

  rm -f /etc/nginx/sites-enabled/default
  ln -sf /etc/nginx/sites-available/senderos /etc/nginx/sites-enabled/senderos
  nginx -t
  systemctl reload nginx
  info "Nginx sirviendo el frontend y proxificando /api (puerto 80; TLS llega con certbot)"
}

# ============================================================================
# 9d. Certificado Let's Encrypt (solo si el DNS ya apunta a este VPS)
# ============================================================================
emitir_certificado() {
  paso "9/9 · HTTPS (certbot / Let's Encrypt)"
  local ip_dns staging
  ip_dns="$(getent ahostsv4 "$DOMINIO" 2>/dev/null | awk '{print $1; exit}' || true)"

  if [ "$ip_dns" != "$VPS_IP" ]; then
    warn "El DNS de $DOMINIO aún no apunta a $VPS_IP (resuelve: '${ip_dns:-nada}')."
    warn "Se omite certbot. Cuando el DNS propague, re-ejecuta este script o corre a mano:"
    warn "  certbot --nginx -d $DOMINIO --redirect -m <tu-correo> --agree-tos"
    return 0
  fi

  if certbot certificates 2>/dev/null | grep -q "Domains:.*$DOMINIO"; then
    info "Ya existe certificado para $DOMINIO (renovación automática con certbot.timer)"
    systemctl is-enabled certbot.timer >/dev/null 2>&1 || systemctl enable --now certbot.timer
    return 0
  fi

  local correo
  correo="$(get_env_val CERTBOT_EMAIL)"
  [ -n "$correo" ] || fatal "CERTBOT_EMAIL no definida en $ENV_PROD"

  staging="${CERTBOT_STAGING:-$(get_env_val CERTBOT_STAGING)}"
  local flags=(--non-interactive --agree-tos -m "$correo" --redirect)
  if [ "$staging" = "true" ]; then
    flags+=(--staging)
    warn "Usando entorno STAGING de Let's Encrypt (cert de prueba)"
  fi

  certbot --nginx -d "$DOMINIO" "${flags[@]}"
  systemctl is-enabled certbot.timer >/dev/null 2>&1 || systemctl enable --now certbot.timer
  info "HTTPS activo en https://$DOMINIO (renovación automática programada)"
}

# ============================================================================
# 9e. Backups diarios (cron 2:00 a. m.) + primer backup de verificación
# ============================================================================
programar_backups() {
  paso "9/9 · Backups diarios de la BD"
  mkdir -p "$DIR_BACKUPS"
  cat > /etc/cron.d/senderos-backup <<CRON
# Senderos de Aragua — backup diario de MySQL (contenedor senderos-mysql)
0 2 * * * root $RAIZ/scripts/backup-db.sh $DIR_BACKUPS >> /var/log/senderos-backup.log 2>&1
CRON
  chmod 644 /etc/cron.d/senderos-backup
  "$RAIZ/scripts/backup-db.sh" "$DIR_BACKUPS"
  info "Backup diario programado a las 2:00 a. m. (hora del VPS) en $DIR_BACKUPS"
  warn "Copia periódicamente $DIR_BACKUPS fuera del VPS (retención fuera del servidor)."
}

# ============================================================================
# Resumen final
# ============================================================================
resumen_final() {
  local mail_from remitente
  mail_from="$(get_env_val MAIL_FROM)"
  remitente="${mail_from:-<MAIL_FROM no definida>}"
  cat <<RESUMEN

============================================================================
 DESPLIEGUE COMPLETADO — Senderos de Aragua
============================================================================
  Sitio:            http://$DOMINIO  (HTTPS si certbot emitió certificado)
  Panel admin:      $DOMINIO/admin  (login con la cuenta admin)
  Servicio backend: systemctl status $SERVICIO · journalctl -u $SERVICIO -f
  MySQL:            docker logs senderos-mysql · datos en volumen senderos-mysql-data
  Build frontend:   $WEB_RAIZ   · Jar: $JAR_PROD
  Variables:        $ENV_PROD (600, root) — edits futuros: editar y
                    ejecutar 'systemctl restart $SERVICIO'

 PENDIENTES OPERATIVOS (no automatizables desde aquí):
  1. Si certbot se omitió por DNS: re-ejecutar este script cuando
     $DOMINIO apunte a $VPS_IP.
  2. Brevo: autenticar el dominio de envío (SPF/DKIM/DMARC) y validar el
     remitente $remitente — ver instrucciones del despliegue.
  3. Rotar la contraseña del admin por la plataforma (POST /api/auth/password).
  4. Probar el envío real: panel admin → correo de prueba.
  5. Copiar los backups fuera del VPS.
============================================================================
RESUMEN
}

# ============================================================================
# Flujo principal
# ============================================================================
main() {
  [ "$(id -u)" -eq 0 ] || fatal "Ejecuta como root: sudo bash scripts/desplegar-vps.sh"
  [ -f "$RAIZ/backend/pom.xml" ] && [ -f "$RAIZ/frontend/package.json" ] \
    || fatal "Este script debe ejecutarse desde la copia del repo en el VPS (no encontré backend/pom.xml junto al script)"

  case "${1:-}" in
    verificar) verificar ;;
    "")        instalar_base
               instalar_java
               instalar_maven
               instalar_node
               instalar_docker
               configurar_ufw
               preparar_env
               levantar_mysql
               construir_frontend
               construir_backend
               configurar_nginx
               emitir_certificado
               programar_backups
               resumen_final ;;
    *)         fatal "Uso: sudo bash scripts/desplegar-vps.sh  |  sudo bash scripts/desplegar-vps.sh verificar" ;;
  esac
}

main "$@"
