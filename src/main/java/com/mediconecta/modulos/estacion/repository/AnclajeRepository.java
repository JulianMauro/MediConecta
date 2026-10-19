package com.mediconecta.modulos.estacion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import com.mediconecta.modulos.estacion.entity.Anclaje;
import com.mediconecta.modulos.estacion.entity.EstadoDock;

public interface AnclajeRepository extends JpaRepository<Anclaje, Long> {

	List<Anclaje> findByEstacionIdAndEstado(Long estacionId, EstadoDock estado);

	boolean existsByEstacionIdAndNumero(Long estacionId, int numero);

	long countByEstacionId(Long estacionId);

	/** Bloquea el anclaje de destino: dos viajes que cierran a la vez no pueden ocupar el mismo. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Anclaje a where a.id = :id")
	Optional<Anclaje> findParaOcupar(Long id);
}
