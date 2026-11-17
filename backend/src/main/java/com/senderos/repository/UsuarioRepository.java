package com.senderos.repository;

import com.senderos.entity.Usuario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles LEFT JOIN FETCH u.vivienda WHERE u.email = :email")
    Optional<Usuario> findByEmail(@Param("email") String email);
    boolean existsByEmail(String email);
    boolean existsByViviendaId(Long viviendaId);

    @Query("select u.vivienda.id from Usuario u where u.email = :email")
    Optional<Long> findViviendaIdByEmail(@Param("email") String email);

    /**
     * Cuentas nacidas PENDIENTE que jamás verificaron su correo antes del
     * corte: candidatas a limpieza para liberar su vivienda (A-1). Se
     * cargan roles y vivienda para el borrado posterior.
     */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles LEFT JOIN FETCH u.vivienda "
        + "WHERE u.demo = false AND u.emailVerificado = false AND u.creadoEn < :corte ORDER BY u.id")
    List<Usuario> findPendientesSinVerificar(@Param("corte") Instant corte, Pageable pageable);
}
