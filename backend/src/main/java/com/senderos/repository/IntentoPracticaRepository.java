package com.senderos.repository;

import com.senderos.entity.IntentoPractica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntentoPracticaRepository extends JpaRepository<IntentoPractica, Long> {

    long countByViviendaId(Long viviendaId);

    long countByViviendaIdAndCorrectoTrue(Long viviendaId);

    /** Últimos intentos de la vivienda, del más reciente al más antiguo (para rachas). */
    List<IntentoPractica> findTop50ByViviendaIdOrderByCreadoEnDescIdDesc(Long viviendaId);
}
