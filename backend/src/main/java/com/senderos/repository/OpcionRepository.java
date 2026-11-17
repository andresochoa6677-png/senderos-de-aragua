package com.senderos.repository;

import com.senderos.entity.Opcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpcionRepository extends JpaRepository<Opcion, Long> {
    List<Opcion> findByPreguntaIdOrderByOrdenAsc(Long preguntaId);
}
