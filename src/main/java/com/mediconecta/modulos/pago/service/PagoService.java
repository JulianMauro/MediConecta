package com.mediconecta.modulos.pago.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.membresia.entity.Membresia;
import com.mediconecta.modulos.membresia.entity.SuscripcionUsuario;
import com.mediconecta.modulos.membresia.repository.SuscripcionUsuarioRepository;
import com.mediconecta.modulos.pago.dto.PagoResponse;
import com.mediconecta.modulos.pago.entity.ConceptoPago;
import com.mediconecta.modulos.pago.entity.EstadoPago;
import com.mediconecta.modulos.pago.entity.Pago;
import com.mediconecta.modulos.pago.repository.PagoRepository;
import com.mediconecta.modulos.strike.entity.Strike;
import com.mediconecta.modulos.strike.repository.StrikeRepository;
import com.mediconecta.modulos.usuario.entity.Rol;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.viaje.entity.Viaje;


@Service
public class PagoService {

	private final PagoRepository pagoRepository;
	private final StrikeRepository strikeRepository;
	private final SuscripcionUsuarioRepository suscripcionUsuarioRepository;

	public PagoService(PagoRepository pagoRepository, StrikeRepository strikeRepository,
			SuscripcionUsuarioRepository suscripcionUsuarioRepository) {
		this.pagoRepository = pagoRepository;
		this.strikeRepository = strikeRepository;
		this.suscripcionUsuarioRepository = suscripcionUsuarioRepository;
	}

	/** Punto unico de alta de pagos: lo usan viaje (tarifa/tiempo extra) y membresia (contratacion). */
	@Transactional
	public Pago crear(Usuario usuario, Viaje viaje, Membresia membresia, ConceptoPago concepto, BigDecimal monto) {
		Pago pago = new Pago(usuario, viaje, membresia, concepto, monto);
		return pagoRepository.save(pago);
	}

	@Transactional(readOnly = true)
	public List<PagoResponse> listarPropios(Long usuarioId) {
		return pagoRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId).stream()
				.map(PagoResponse::desde)
				.toList();
	}

	@Transactional(readOnly = true)
	public PagoResponse obtener(Usuario solicitante, Long id) {
		return PagoResponse.desde(buscarConPermiso(solicitante, id));
	}

	/** Simula la confirmacion que llegaria por webhook de la pasarela de pago. */
	@Transactional
	public PagoResponse confirmar(Usuario solicitante, Long id, String referenciaPasarela) {
		Pago pago = buscarConPermiso(solicitante, id);

		if (pago.getEstado() == EstadoPago.RECHAZADO) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "el pago fue rechazado: no se puede confirmar");
		}

		/*
		 * Si ya estaba pagado, se devuelve el estado actual sin volver a aplicar los
		 * efectos. Es lo que vuelve idempotente al endpoint: un webhook repetido de la
		 * pasarela no puede generar una segunda suscripcion ni saldar dos veces.
		 */
		if (!pago.confirmar(referenciaPasarela)) {
			return PagoResponse.desde(pago);
		}

		if (pago.getConcepto() == ConceptoPago.TIEMPO_EXTRA) {
			Strike strike = strikeRepository.findByPagoId(pago.getId())
					.orElseThrow(() -> new IllegalStateException("pago de tiempo extra sin strike asociado"));
			strike.saldar();
		} else if (pago.getConcepto() == ConceptoPago.MEMBRESIA) {
			// Pago de un plan: lo activa. La duracion depende del tipo (INDIVIDUAL=1 dia, SEMANAL=7, MENSUAL=30).
			Instant inicio = Instant.now();
			Instant fin = inicio.plus(pago.getMembresia().getTipo().duracionDias(), ChronoUnit.DAYS);
			suscripcionUsuarioRepository.save(
					new SuscripcionUsuario(pago.getUsuario(), pago.getMembresia(), inicio, fin));
		}

		return PagoResponse.desde(pago);
	}

	/** Si el usuario no paga el tiempo extra, el strike sigue ACTIVO: no hay reintento automatico. */
	@Transactional
	public PagoResponse rechazar(Usuario solicitante, Long id) {
		Pago pago = buscarConPermiso(solicitante, id);
		if (!pago.rechazar() && pago.getEstado() == EstadoPago.PAGADO) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "el pago ya fue confirmado: no se puede rechazar");
		}
		return PagoResponse.desde(pago);
	}

	private Pago buscarConPermiso(Usuario solicitante, Long id) {
		Pago pago = pagoRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "pago no encontrado"));

		boolean esDueño = pago.getUsuario().getId().equals(solicitante.getId());
		if (!esDueño && solicitante.getRol() != Rol.ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "no podes operar sobre el pago de otro usuario");
		}
		return pago;
	}
}
