package com.senderos.repository;

import com.senderos.entity.Encuesta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EncuestaRepository extends JpaRepository<Encuesta, Long> {
    Optional<Encuesta> findByTipoIgnoreCase(String tipo);
    Optional<Encuesta> findByTipoIgnoreCaseAndActivaTrue(String tipo);
}
