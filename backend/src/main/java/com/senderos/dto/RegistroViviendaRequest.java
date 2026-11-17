package com.senderos.dto;

import com.senderos.security.ContrasenaRobusta;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de registro de residentes. El residente ESCOGE su vivienda
 * por bloque y número (censo oficial BLOQUE-NUMERO: A-1, G-5, Ñ-2…);
 * el backend valida que exista en el catálogo y que esté disponible.
 */
public record RegistroViviendaRequest(
    @NotBlank @Size(max = 80) String nombre,
    @NotBlank @Email @Size(max = 100) String email,
    @NotBlank @Size(min = 10, max = 72) @ContrasenaRobusta String password,
    @AssertTrue boolean aceptaTratamientoDatos,
    @NotBlank @Pattern(regexp = "[A-ZÑ]{1,2}", message = "El bloque debe ser A-Z, RR o Ñ")
    String bloque,
    @NotNull @Min(1) @Max(18) Integer numero
) {}
