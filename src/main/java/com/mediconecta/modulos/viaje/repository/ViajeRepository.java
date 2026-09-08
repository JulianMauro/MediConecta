package com.mediconecta.modulos.viaje.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.viaje.entity.Viaje;

public interface ViajeRepository extends JpaRepository<Viaje, Long> {

	/** Viaje en curso de un usuario (fechaFin null). Sirve para saber si ya tiene una bici afuera. */
	Optional<Viaje> findByUsuarioIdAndFechaFinIsNull(Long usuarioId);

	List<Viaje> findByUsuarioId(Long usuarioId);

	/**
	 * Cuenta viajes iniciados desde una fecha (ej. el arranque del dia de hoy) bajo una
	 * suscripcion puntual, para el limite "viajes por dia". Se escanea por suscripcion,
	 * no por usuario: un plan nuevo no hereda los viajes que ya hizo un plan anterior.
	 */
	long countBySuscripcionIdAndFechaInicioAfter(Long suscripcionId, Instant desde);

	/**
	 * El ultimo viaje que el usuario devolvio bajo una suscripcion puntual, para calcular
	 * el tiempo de espera entre viajes de ese mismo plan.
	 */
	Optional<Viaje> findFirstBySuscripcionIdAndFechaFinIsNotNullOrderByFechaFinDesc(Long suscripcionId);

	/** Viajes en curso ahora mismo: seedea el contador en memoria de UsuariosActivosService al arrancar. */
	long countByFechaFinIsNull();
}
