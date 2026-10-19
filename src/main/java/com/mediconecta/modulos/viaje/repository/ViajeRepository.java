package com.mediconecta.modulos.viaje.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import com.mediconecta.modulos.viaje.entity.Viaje;

public interface ViajeRepository extends JpaRepository<Viaje, Long> {

	/** Viaje en curso de un usuario (fechaFin null). Sirve para saber si ya tiene una bici afuera. */
	Optional<Viaje> findByUsuarioIdAndFechaFinIsNull(Long usuarioId);

	/**
	 * Igual que el anterior pero bloqueando la fila (SELECT ... FOR UPDATE) hasta el fin de la
	 * transaccion. Lo usa todo cierre de viaje: si el cliente y soporte cierran a la vez, el
	 * segundo espera, y al volver a evaluar el WHERE ya no lo encuentra en curso.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select v from Viaje v where v.usuario.id = :usuarioId and v.fechaFin is null")
	Optional<Viaje> findEnCursoParaCerrar(Long usuarioId);

	/** Mismo bloqueo, para corregir un viaje ya cerrado sin pisarse con otra correccion. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select v from Viaje v where v.id = :id")
	Optional<Viaje> findParaModificar(Long id);

	List<Viaje> findByUsuarioId(Long usuarioId);

	/** Los ultimos viajes ya devueltos: el historial que soporte ve del usuario. */
	List<Viaje> findTop10ByUsuarioIdAndFechaFinIsNotNullOrderByFechaInicioDesc(Long usuarioId);

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
