package com.senderos.support;

/**
 * Credenciales del administrador SOLO para el entorno de pruebas (H2).
 *
 * Los tests sobrescriben el hash del administrador en la base de datos de
 * pruebas con {@link #HASH} al iniciar, de modo que la contraseña real de
 * producción nunca aparece en el código fuente. La contraseña de tests es
 * {@link #CLAVE} y no da acceso a ningún entorno real.
 */
public final class AdminDePruebas {

    public static final String EMAIL = "andresochoa224@unisangil.edu.co";

    /** Clave ficticia usada únicamente contra la BD H2 de pruebas. */
    public static final String CLAVE = "clave-admin-de-pruebas-2026";

    /** Hash BCrypt de CLAVE (pre-generado; asignado al admin en cada test). */
    public static final String HASH =
        "$2b$10$J4kuQ0e2Pc2h/2SD4h7yp.1QMevbwt/Ylb3YdFEP/GBwzWHNGpn9K";

    private AdminDePruebas() {}

    /** Fija el hash de pruebas sobre el administrador de la BD de tests. */
    public static void fijarClaveDePruebas(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update(
            "UPDATE usuario SET password_hash = ? WHERE email = ?", HASH, EMAIL);
    }

    /** Cuerpo JSON listo para POST /api/auth/login en los tests. */
    public static String cuerpoLogin() {
        return "{\"email\":\"" + EMAIL + "\",\"password\":\"" + CLAVE + "\"}";
    }
}
