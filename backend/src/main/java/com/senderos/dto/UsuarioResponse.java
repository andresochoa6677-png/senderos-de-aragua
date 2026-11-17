package com.senderos.dto;

import com.senderos.entity.Usuario;
import com.senderos.entity.Rol;

public record UsuarioResponse(
    Long id,
    String email,
    String nombre,
    boolean demo,
    Long viviendaId,
    String viviendaCodigo,
    String rol
) {
    public static UsuarioResponse de(Usuario usuario) {
        String rol = usuario.getRoles().stream().map(Rol::getNombre).findFirst().orElse("RESIDENTE");
        return new UsuarioResponse(
            usuario.getId(), usuario.getEmail(), usuario.getNombre(), usuario.isDemo(),
            usuario.getVivienda() != null ? usuario.getVivienda().getId() : null,
            usuario.getVivienda() != null ? usuario.getVivienda().getCodigo() : null,
            rol);
    }
}
