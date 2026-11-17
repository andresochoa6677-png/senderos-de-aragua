package com.senderos.repository;

import com.senderos.entity.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    /** Aportes de una vivienda, del más reciente al más antiguo. */
    List<Comentario> findByViviendaIdOrderByCreadoEnDescIdDesc(Long viviendaId);

    /** Historial completo para el equipo investigador (ruta administrativa). */
    List<Comentario> findAllByOrderByCreadoEnDescIdDesc();
}
