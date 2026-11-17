package com.senderos.repository;

import com.senderos.entity.Leccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeccionRepository extends JpaRepository<Leccion, Long> {

    List<Leccion> findByActivaTrueOrderByOrdenAsc();

    Optional<Leccion> findByIdAndActivaTrue(Long id);

    /** Primera lección activa cuyo orden sea estrictamente mayor al dado (siguiente en el recorrido). */
    Optional<Leccion> findFirstByActivaTrueAndOrdenGreaterThanOrderByOrdenAsc(Integer orden);
}
