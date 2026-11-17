package com.senderos.repository;

import com.senderos.entity.Vivienda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ViviendaRepository extends JpaRepository<Vivienda, Long> {
    Optional<Vivienda> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
