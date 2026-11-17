package com.senderos.security;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Validador de la política de contraseñas: longitud mínima, rechazo de
 * contraseñas solo numéricas, de baja entropía o presentes en la lista de
 * las más comunes (recurso contrasenas-comunes.txt). La comparación es por
 * contenido normalizado (minúsculas, sin acentos), de modo que
 * "Contraseña123" y "contrasena123" caen igual.
 */
public class ContrasenaRobustaValidator implements ConstraintValidator<ContrasenaRobusta, String> {

    private static final int MINIMA = 10;
    private static final Set<String> COMUNES = cargarLista();

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null || valor.isBlank()) {
            return true; // la obligatoriedad la impone @NotBlank
        }
        if (valor.length() < MINIMA) {
            return false;
        }
        if (valor.chars().allMatch(Character::isDigit)) {
            return false; // solo números: entropía insuficiente aunque sean 10+
        }
        String normalizada = normalizar(valor);
        if (normalizada.chars().distinct().count() < 3) {
            return false; // p. ej. "aaaaaaaaaa" o "ababababab"
        }
        for (String comun : COMUNES) {
            if (normalizada.contains(comun)) {
                return false;
            }
        }
        return true;
    }

    private static String normalizar(String valor) {
        String sinAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase(Locale.ROOT);
    }

    private static Set<String> cargarLista() {
        Set<String> lista = new HashSet<>();
        try (InputStream recurso = ContrasenaRobustaValidator.class
                .getClassLoader().getResourceAsStream("contrasenas-comunes.txt")) {
            if (recurso == null) {
                throw new IllegalStateException("Falta contrasenas-comunes.txt en el classpath");
            }
            try (BufferedReader lector = new BufferedReader(new InputStreamReader(recurso, StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    String entrada = normalizar(linea.trim());
                    if (!entrada.isEmpty() && !entrada.startsWith("#")) {
                        lista.add(entrada);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer contrasenas-comunes.txt", e);
        }
        return lista;
    }
}
