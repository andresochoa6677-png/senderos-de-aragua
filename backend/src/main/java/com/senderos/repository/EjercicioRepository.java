package com.senderos.repository;

import com.senderos.entity.Ejercicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EjercicioRepository extends JpaRepository<Ejercicio, Long> {

    List<Ejercicio> findByLeccionIdAndActivoTrueOrderByOrdenAsc(Long leccionId);

    List<Ejercicio> findByActivoTrueOrderByLeccionOrdenAscOrdenAsc();
}
