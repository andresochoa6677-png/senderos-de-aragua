-- ============================================================
-- V22: Rotación de la contraseña del administrador único.
--      Actualiza el hash BCrypt de andresochoa224@unisangil.edu.co
--      (la clave anterior quedó expuesta como constante de prueba).
--      La clave en claro NO aparece en este archivo: el hash fue
--      generado con BCryptPasswordEncoder (strength 10) fuera del
--      repositorio y la nueva clave se comunica por canal aparte.
--      V19 no se modifica.
-- ============================================================

UPDATE usuario
   SET password_hash = '$2a$10$SB9RZee.x6nFlXL8nn5GJufkrSAwaOUnQvwifdSCA940fZfp0L2l.',
       actualizado_en = CURRENT_TIMESTAMP
 WHERE email = 'andresochoa224@unisangil.edu.co';
