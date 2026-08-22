package com.mediconecta.modulos.notificacion.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.notificacion.dto.NotificacionResponse;
import com.mediconecta.modulos.notificacion.entity.Notificacion;
import com.mediconecta.modulos.notificacion.entity.TipoNotificacion;
import com.mediconecta.modulos.notificacion.repository.NotificacionRepository;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.viaje.entity.Viaje;


@Service
public class NotificacionService {

	private final NotificacionRepository notificacionRepository;

	public NotificacionService(NotificacionRepository notificacionRepository) {
		this.notificacionRepository = notificacionRepository;
	}

	/** Alta interna: la disparan otros modulos (strike, viaje), no hay endpoint de creacion manual. */
	@Transactional
	public Notificacion crear(Usuario usuario, Viaje viaje, TipoNotificacion tipo, String mensaje) {
		return notificacionRepository.save(new Notificacion(usuario, viaje, tipo, mensaje));
	}

	@Transactional(readOnly = true)
	public List<NotificacionResponse> listarPropias(Long usuarioId) {
		return notificacionRepository.findByUsuarioIdOrderByFechaEnvioDesc(usuarioId).stream()
				.map(NotificacionResponse::desde)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<NotificacionResponse> listarNoLeidas(Long usuarioId) {
		return notificacionRepository.findByUsuarioIdAndLeidaFalse(usuarioId).stream()
				.map(NotificacionResponse::desde)
				.toList();
	}

	@Transactional
	public NotificacionResponse marcarLeida(Long usuarioId, Long id) {
		Notificacion notificacion = notificacionRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "notificacion no encontrada"));

		if (!notificacion.getUsuario().getId().equals(usuarioId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "no podes operar sobre la notificacion de otro usuario");
		}

		notificacion.marcarLeida();
		return NotificacionResponse.desde(notificacion);
	}
}
