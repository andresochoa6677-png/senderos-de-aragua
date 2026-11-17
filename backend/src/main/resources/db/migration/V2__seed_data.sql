-- V2: Datos iniciales - roles, vivienda DEMO y cuentas admin/demo
-- Hashes BCrypt generados externamente; rotar contraseñas en primer inicio.

INSERT INTO rol (nombre) VALUES ('RESIDENTE'), ('ADMIN');

INSERT INTO vivienda (codigo, direccion, sector, demo, activa)
VALUES ('DEMO-001', 'Vivienda de demostración', 'DEMO', 1, 1);

INSERT INTO usuario (email, password_hash, nombre, enabled, demo, vivienda_id)
VALUES ('admin@senderos.local', '$2a$10$nlrnjDkNvY8j2amwPxWn1Or.BeqsYQw6RUKaIk1pXmVMHIVkthCVa', 'Administrador', 1, 0, NULL),
       ('demo@senderos.local',  '$2a$10$RfP3o2/pPxwv37E4TJ1TTux0Ljcj3ASIwCOYRdfoJxprFb.Q/r/Cq', 'Residente Demo', 1, 1, 1);

INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id FROM usuario u, rol r WHERE u.email = 'admin@senderos.local' AND r.nombre = 'ADMIN';

INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id FROM usuario u, rol r WHERE u.email = 'demo@senderos.local' AND r.nombre = 'RESIDENTE';
