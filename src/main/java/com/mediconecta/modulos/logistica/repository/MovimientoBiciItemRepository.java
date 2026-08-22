package com.mediconecta.modulos.logistica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.logistica.entity.MovimientoBiciItem;

public interface MovimientoBiciItemRepository extends JpaRepository<MovimientoBiciItem, Long> {

	List<MovimientoBiciItem> findByMovimientoId(Long movimientoId);
}
