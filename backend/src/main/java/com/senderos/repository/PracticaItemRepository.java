package com.senderos.repository;

import com.senderos.entity.PracticaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticaItemRepository extends JpaRepository<PracticaItem, Long> {

    List<PracticaItem> findByModoAndActivoTrueOrderByIdAsc(String modo);
}
