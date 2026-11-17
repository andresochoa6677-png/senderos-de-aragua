-- V4: Corrige los hashes BCrypt del seed V2, que no correspondian a ninguna
-- contrasena documentada y bloqueaban el login de admin/demo.
-- Contrasenas dev (rotar en despliegue real):
--   admin@senderos.local -> admin1234
--   demo@senderos.local  -> demo1234

UPDATE usuario
SET password_hash = '$2a$10$0wrRPahtgP7OcHCSlc1C4.0C31drt6KqkHBcJoknwg.oYqzWp18ee'
WHERE email = 'admin@senderos.local';

UPDATE usuario
SET password_hash = '$2a$10$TYJL7/.7WDGljVP3GViqbeKHyymVovkn3omCQ.9kpIDKV.0G9omFG'
WHERE email = 'demo@senderos.local';
