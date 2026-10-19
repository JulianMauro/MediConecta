package com.mediconecta.modulos.strike.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import com.mediconecta.modulos.strike.entity.EstadoStrike;
import com.mediconecta.modulos.strike.entity.Strike;

public interface StrikeRepository extends JpaRepository<Strike, Long> {

	/** Cuenta los strikes activos para decidir si corresponde bloquear la cuenta. */
	List<Strike> findByUsuarioIdAndEstado(Long usuarioId, EstadoStrike estado);

	List<Strike> findByUsuarioId(Long usuarioId);

	List<Strike> findByUsuarioIdOrderByFechaGeneracionDesc(Long usuarioId);

	/** Bloquea la fila: un doble clic en "anular" no puede registrar la accion dos veces. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Strike s where s.id = :id")
	Optional<Strike> findParaModificar(Long id);

	/** La usa el pago de tiempo extra para saldar el strike que lo origino, al confirmarse. */
	Optional<Strike> findByPagoId(Long pagoId);

	/** Un viaje genera a lo sumo un strike (al cerrarse fuera de tiempo). Lo usa soporte al corregirlo. */
	Optional<Strike> findByViajeId(Long viajeId);
}
