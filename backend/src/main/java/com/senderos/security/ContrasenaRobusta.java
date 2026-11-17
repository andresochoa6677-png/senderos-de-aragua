package com.senderos.security;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Política de contraseñas NIST 800-63B adaptada (auditoría 2026-09-12, A-3):
 * longitud mínima y rechazo de contraseñas comprometidas/obvias.
 * NO exige reglas de composición (mayúsculas, símbolos): obligar a mezclar
 * solo induce patrones predecibles; la longitud y la lista de denegación
 * son lo que realmente sube la entropía.
 */
@Documented
@Constraint(validatedBy = ContrasenaRobustaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ContrasenaRobusta {

    String message() default "Elige una contraseña de al menos 10 caracteres, "
        + "que no sea demasiado común ni solo de números";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
