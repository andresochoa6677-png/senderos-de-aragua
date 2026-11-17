package com.senderos.repository;

import com.senderos.entity.ProgresoLeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProgresoLeccionRepository extends JpaRepository<ProgresoLeccion, Long> {

    /** Progreso de una vivienda, ordenado por el orden de la leccion. */
    @Query("""
            select p from ProgresoLeccion p
            join fetch p.leccion l
            where p.vivienda.id = :viviendaId
            order by l.orden asc
            """)
    List<ProgresoLeccion> findByViviendaOrderByOrden(@Param("viviendaId") Long viviendaId);

    @Query("""
            select p from ProgresoLeccion p
            join fetch p.leccion l
            where p.vivienda.id = :viviendaId and l.id = :leccionId
            """)
    Optional<ProgresoLeccion> findByViviendaAndLeccion(@Param("viviendaId") Long viviendaId,
                                                       @Param("leccionId") Long leccionId);
}
