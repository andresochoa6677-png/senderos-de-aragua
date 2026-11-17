# Senderos de Aragua

Plataforma web educativa y de investigación académica (UNISANGIL, sede Yopal) para el manejo de residuos sólidos del Condominio Residencial Senderos de Aragua. Cada vivienda tiene una única cuenta con la que accede a un recorrido educativo (lecciones, actividades y práctica libre), responde una encuesta diagnóstica y una final, participa por medio de comentarios y queda trazada digitalmente (consentimiento informado con hash, accesos y módulos consultados) para la investigación. El administrador gestiona el estudio desde un panel privado.


## Requisitos

- **Java 21** (Temurin recomendado) + **Maven 3.9+**
- **Node.js 20** + npm 10
- **Docker** con Docker Compose (MySQL 8.4)

## Arranque en local

```bash
# 1. Base de datos MySQL (puerto 3306, volumen persistente)
cd docker
docker compose up -d

# 2. Backend (puerto 8080) — carga las credenciales de docker/.env
cd ../backend
export DB_USERNAME=$(grep -E "^DB_USERNAME=" ../docker/.env | cut -d= -f2-)
export DB_PASSWORD=$(grep -E "^DB_PASSWORD=" ../docker/.env | cut -d= -f2-)
mvn spring-boot:run

# 3. Frontend (puerto 5173, proxy /api → 8080)
cd ../frontend
npm install
npm run dev
```

Abrir **http://localhost:5173**. Las migraciones Flyway (V1–V27) se aplican solas al arrancar el backend.

> Si el repositorio local tiene configuración de correo en un `.env` raíz (`MAIL_MODO=smtp` o `brevo`), arrancar el backend con `set -a; source .env; set +a` antes del comando para que los correos salgan reales.

## Tests

```bash
cd backend
mvn test
```

116 tests de integración sobre H2 en memoria (las mismas migraciones Flyway): flujos completos de registro, encuestas, lecciones, panel, seguridad y sesión persistente.

## Variables de entorno (nombres; valores por canal seguro)

| Área | Variables |
|---|---|
| Base de datos | `DB_URL` · `DB_USERNAME` · `DB_PASSWORD` |
| Correo (SMTP/Brevo) | `MAIL_MODO` (`captura`/`smtp`/`brevo`) · `MAIL_HOST` · `MAIL_PORT` · `MAIL_USERNAME` · `MAIL_PASSWORD` · `MAIL_FROM` · `MAIL_FROM_NAME` · `BREVO_API_KEY` · `BREVO_TEMPLATE_VERIFICACION` · `BREVO_TEMPLATE_RECUPERACION` |
| Aplicación | `CORS_ALLOWED_ORIGINS` · `APP_BASE_URL` · `COOKIE_SECURE` · `RATE_LIMIT_MAX_INTENTOS` · `RATE_LIMIT_VENTANA_MINUTOS` · `LIMPIEZA_DIAS_PENDIENTE` |
| Solo desarrollo | `PASSWORD_RESET_LOG_TOKEN` · `PASSWORD_VERIFICATION_LOG_TOKEN` |

## Estructura del proyecto

```
plataforma-educativa/
├── backend/               # Spring Boot 3.5 (Java 21)
│   ├── src/main/java/com/senderos/
│   │   ├── controller/    # API REST (28 endpoints)
│   │   ├── service/       # Lógica de negocio
│   │   ├── repository/    # Spring Data JPA
│   │   ├── entity/        # Entidades JPA
│   │   ├── security/      # Config, rate limiter, tokens
│   │   └── exception/     # Manejo centralizado de errores
│   ├── src/main/resources/
│   │   ├── db/migration/  # Flyway V1–V27
│   │   └── consentimiento/REG-2.0.txt
│   └── src/test/          # 116 tests de integración (H2)
├── frontend/              # React 18 + Vite + Tailwind
│   └── src/{pages,layouts,components,context,api,data}
├── docker/                # docker-compose.yml (MySQL 8.4) + .env
└── scripts/               # backup-db.sh, limpieza_datos.sql, desplegar-vps.sh
```

## Cuentas semilla (rotar antes de producción)

| Cuenta | Uso |
|---|---|
| `andresochoa224@unisangil.edu.co` | Administrador único (contraseña por canal aparte) |
| `demo@senderos.local` | Cuenta de demostración (excluida de métricas) |

## Seguridad

- Contraseñas con BCrypt y política adaptada de NIST 800-63B (mínimo 10 caracteres, lista de denegación).
- Sesiones server-side con rotación de identificador, máximo 1 por usuario, y "mantener la sesión activa" opcional por dispositivo (token persistente rotativo, invalidado al cambiar la contraseña).
- CSRF por doble envío, cabeceras CSP/HSTS/X-Frame-Options y límites de tasa en todos los endpoints públicos de escritura.
- Claves de calificación y tokens solo en el servidor; migraciones versionadas para todo cambio de esquema.

## Mantenimiento

- **Backup de BD**: `scripts/backup-db.sh` (ver cron en el encabezado del script)
- **Limpieza a estado demo**: `scripts/limpieza_datos.sql`
- **Escaneo de dependencias**: `mvn -Psecurity dependency-check:aggregate`
