-- ============================================================
-- V27: "Mantener sesión activa" (remember-me persistente).
--
--      Tabla estándar de Spring Security (PersistentTokenRememberMe-
--      Services / JdbcTokenRepositoryImpl): guarda series + token por
--      dispositivo. El residente marca "Mantener la sesión activa" al
--      iniciar sesión y el navegador queda autenticado aunque la sesión
--      HTTP expire o el servidor se reinicie.
--
--      Seguridad: en la BD solo vive el HASH del token; la cookie es
--      Secure/HttpOnly y se rota en cada uso. Cambiar o restablecer la
--      contraseña borra los tokens del usuario (dispositivos perdidos
--      dejan de estar autenticados).
-- ============================================================

CREATE TABLE persistent_logins (
    username  VARCHAR(64)  NOT NULL,
    series    VARCHAR(64)  PRIMARY KEY,
    token     VARCHAR(64)  NOT NULL,
    last_used TIMESTAMP    NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_persistent_logins_username ON persistent_logins (username);
