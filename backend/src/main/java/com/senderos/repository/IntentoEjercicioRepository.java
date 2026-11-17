package com.senderos.repository;

import com.senderos.entity.IntentoEjercicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IntentoEjercicioRepository extends JpaRepository<IntentoEjercicio, Long> {

    /** Intentos de la vivienda para un ejercicio, del más reciente al más antiguo. */
    @Query("""
            select i from IntentoEjercicio i
            where i.vivienda.id = :viviendaId and i.ejercicio.id = :ejercicioId
            order by i.creadoEn desc
            """)
    List<IntentoEjercicio> findByViviendaAndEjercicio(@Param("viviendaId") Long viviendaId,
                                                       @Param("ejercicioId") Long ejercicioId);

    /** Todos los intentos de la vivienda, del más reciente al más antiguo. */
    @Query("""
            select i from IntentoEjercicio i
            join fetch i.ejercicio e
            join fetch e.leccion
            where i.vivienda.id = :viviendaId
            order by i.creadoEn desc
            """)
    List<IntentoEjercicio> findByViviendaOrderByCreadoDesc(@Param("viviendaId") Long viviendaId);

    /** Ids (sin repetir) de los ejercicios de una lección que la vivienda ya resolvió correctamente. */
    @Query("""
            select distinct i.ejercicio.id from IntentoEjercicio i
            where i.vivienda.id = :viviendaId and i.correcto = true
              and i.ejercicio.leccion.id = :leccionId and i.ejercicio.activo = true
            """)
    List<Long> findEjerciciosCorrectosDeLeccion(@Param("viviendaId") Long viviendaId,
                                                @Param("leccionId") Long leccionId);
}
