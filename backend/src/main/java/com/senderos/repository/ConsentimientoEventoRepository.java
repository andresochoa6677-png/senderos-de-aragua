package com.senderos.repository;

import com.senderos.entity.ConsentimientoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsentimientoEventoRepository extends JpaRepository<ConsentimientoEvento, Long> {
    List<ConsentimientoEvento> findByViviendaIdOrderByFechaEventoDesc(Long viviendaId);
}
