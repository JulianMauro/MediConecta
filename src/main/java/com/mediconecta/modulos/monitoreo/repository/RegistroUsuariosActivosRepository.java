package com.mediconecta.modulos.monitoreo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.monitoreo.entity.RegistroUsuariosActivos;

public interface RegistroUsuariosActivosRepository extends JpaRepository<RegistroUsuariosActivos, Long> {

	/** Historial para graficar, mas reciente primero. */
	List<RegistroUsuariosActivos> findAllByOrderByTimestampDesc();
}
