package com.mediconecta.modulos.strike.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.strike.entity.EstadoStrike;
import com.mediconecta.modulos.strike.entity.Strike;

public interface StrikeRepository extends JpaRepository<Strike, Long> {

	/** Cuenta los strikes activos para decidir si corresponde bloquear la cuenta. */
	List<Strike> findByUsuarioIdAndEstado(Long usuarioId, EstadoStrike estado);

	List<Strike> findByUsuarioId(Long usuarioId);

	/** La usa el pago de tiempo extra para saldar el strike que lo origino, al confirmarse. */
	Optional<Strike> findByPagoId(Long pagoId);
}
