package com.mediconecta.modulos.membresia.service;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.membresia.entity.EstadoSuscripcion;
import com.mediconecta.modulos.membresia.entity.Membresia;
import com.mediconecta.modulos.membresia.entity.SuscripcionUsuario;
import com.mediconecta.modulos.membresia.dto.SuscripcionResponse;
import com.mediconecta.modulos.membresia.repository.MembresiaRepository;
import com.mediconecta.modulos.membresia.repository.SuscripcionUsuarioRepository;
import com.mediconecta.modulos.pago.dto.PagoResponse;
import com.mediconecta.modulos.pago.entity.ConceptoPago;
import com.mediconecta.modulos.pago.entity.Pago;
import com.mediconecta.modulos.pago.service.PagoService;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;


@Service
public class SuscripcionUsuarioService {

	private final SuscripcionUsuarioRepository suscripcionUsuarioRepository;
	private final MembresiaRepository membresiaRepository;
	private final UsuarioRepository usuarioRepository;
	private final PagoService pagoService;

	public SuscripcionUsuarioService(SuscripcionUsuarioRepository suscripcionUsuarioRepository,
			MembresiaRepository membresiaRepository, UsuarioRepository usuarioRepository, PagoService pagoService) {
		this.suscripcionUsuarioRepository = suscripcionUsuarioRepository;
		this.membresiaRepository = membresiaRepository;
		this.usuarioRepository = usuarioRepository;
		this.pagoService = pagoService;
	}

	/**
	 * Contrata un plan (incluido INDIVIDUAL: tambien hay que pagarlo antes de poder viajar).
	 * No activa la suscripcion todavia: genera el pago pendiente y recien cuando
	 * PagoService.confirmar lo salda se crea la suscripcion ACTIVA.
	 */
	@Transactional
	public PagoResponse contratar(Long usuarioId, Long membresiaId) {
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new IllegalStateException("usuario autenticado no encontrado"));

		Membresia membresia = membresiaRepository.findById(membresiaId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "membresia no encontrada"));

		if (!membresia.isActiva()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "la membresia no esta activa");
		}
		if (suscripcionUsuarioRepository
				.findByUsuarioIdAndEstadoAndFechaFinAfter(usuarioId, EstadoSuscripcion.ACTIVA, Instant.now())
				.isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "ya tenes un plan vigente");
		}

		Pago pago = pagoService.crear(usuario, null, membresia, ConceptoPago.MEMBRESIA, membresia.getPrecio());
		return PagoResponse.desde(pago);
	}

	@Transactional(readOnly = true)
	public List<SuscripcionResponse> listarPropias(Long usuarioId) {
		return suscripcionUsuarioRepository.findByUsuarioId(usuarioId).stream()
				.map(SuscripcionResponse::desde)
				.toList();
	}

	@Transactional
	public void cancelar(Long usuarioId, Long suscripcionId) {
		SuscripcionUsuario suscripcion = suscripcionUsuarioRepository.findById(suscripcionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "suscripcion no encontrada"));

		if (!suscripcion.getUsuario().getId().equals(usuarioId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "no podes cancelar la suscripcion de otro usuario");
		}
		suscripcion.cancelar();
	}
}
