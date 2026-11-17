package com.senderos.repository;

import com.senderos.entity.Pregunta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {
    List<Pregunta> findByEncuestaIdOrderByOrdenAsc(Long encuestaId);
}
