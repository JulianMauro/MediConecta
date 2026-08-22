package com.mediconecta.modulos.estacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.estacion.entity.Anclaje;
import com.mediconecta.modulos.estacion.entity.EstadoDock;

public interface AnclajeRepository extends JpaRepository<Anclaje, Long> {

	List<Anclaje> findByEstacionIdAndEstado(Long estacionId, EstadoDock estado);

	boolean existsByEstacionIdAndNumero(Long estacionId, int numero);

	long countByEstacionId(Long estacionId);
}
