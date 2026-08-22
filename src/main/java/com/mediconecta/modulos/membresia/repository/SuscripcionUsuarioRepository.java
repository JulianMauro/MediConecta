package com.mediconecta.modulos.membresia.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.membresia.entity.EstadoSuscripcion;
import com.mediconecta.modulos.membresia.entity.SuscripcionUsuario;

public interface SuscripcionUsuarioRepository extends JpaRepository<SuscripcionUsuario, Long> {

	/**
	 * El plan "vigente ahora": ACTIVA y todavia no vencio segun su fecha. No alcanza con
	 * mirar el estado solo, porque nada pasa una suscripcion a VENCIDA automaticamente
	 * al llegar la fecha (no hay job para eso en este POC).
	 */
	Optional<SuscripcionUsuario> findByUsuarioIdAndEstadoAndFechaFinAfter(Long usuarioId, EstadoSuscripcion estado,
			Instant ahora);

	List<SuscripcionUsuario> findByUsuarioId(Long usuarioId);
}
