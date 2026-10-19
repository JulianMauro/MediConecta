package com.mediconecta.modulos.soporte.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.soporte.entity.AccionSoporte;

public interface AccionSoporteRepository extends JpaRepository<AccionSoporte, Long> {

	List<AccionSoporte> findByTicketIdOrderByFechaAsc(Long ticketId);
}
