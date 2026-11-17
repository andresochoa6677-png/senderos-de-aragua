package com.senderos.repository;

import com.senderos.entity.Consentimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsentimientoRepository extends JpaRepository<Consentimiento, Long> {
    /** Estado vigente de la vivienda (una fila por vivienda, uk_consentimiento_vivienda). */
    Optional<Consentimiento> findFirstByViviendaId(Long viviendaId);

    /** ¿Existe aceptación de la versión indicada? (la puerta del onboarding). */
    boolean existsByViviendaIdAndAceptadoTrueAndVersionDocumento(Long viviendaId, String versionDocumento);

    /** Historial de decisiones (aceptaciones y rechazos) por vivienda. */
    List<Consentimiento> findByViviendaIdOrderByFechaAceptacionDesc(Long viviendaId);
}
