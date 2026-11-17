package com.senderos.repository;

import com.senderos.entity.RegistroAcceso;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegistroAccesoRepository extends JpaRepository<RegistroAcceso, Long> {
    List<RegistroAcceso> findByViviendaIdOrderByFechaHoraDesc(Long viviendaId, Pageable pageable);

    long countByViviendaIdAndModulo(Long viviendaId, String modulo);

    @Query("select ra.modulo, count(ra) from RegistroAcceso ra where ra.vivienda.id = :viviendaId group by ra.modulo order by count(ra) desc")
    List<Object[]> conteoPorModulo(@Param("viviendaId") Long viviendaId);
}
