package com.senderos.dto;

import com.senderos.security.ContrasenaRobusta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerPasswordRequest(
    @NotBlank String token,
    @NotBlank @Size(min = 10, max = 72) @ContrasenaRobusta String nuevaPassword
) {}
