package com.senderos.dto;

import java.util.List;

public record EncuestaResponse(
    Long id,
    String tipo,
    String titulo,
    List<PreguntaDTO> preguntas
) {
    public record PreguntaDTO(
        Long id,
        String texto,
        String tipo,
        String tipoPregunta,
        int orden,
        boolean requerida,
        List<OpcionDTO> opciones
    ) {
        /**
         * Opcion visible al cliente. La clave de correccion (esCorrecta)
         * y el valor numerico interno NO se exponen para no filtrar
         * respuestas correctas ni puntajes antes de enviar.
         */
        public record OpcionDTO(Long id, String texto, int orden) {}
    }
}
