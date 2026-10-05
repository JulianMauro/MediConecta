package com.mediconecta.modulos.chatP2P.dto;

import java.time.Instant;

import com.mediconecta.modulos.chatP2P.entity.ChatMensaje;
import com.mediconecta.modulos.chatP2P.messaging.ChatMensajeJms;
import com.mediconecta.modulos.usuario.entity.Rol;

public record ChatMensajeResponse(
		Long id,
		Long ticketId,
		Long remitenteId,
		Rol remitenteRol,
		String contenido,
		Instant fechaEnvio,
		Instant fechaEntrega
) {

	public static ChatMensajeResponse desde(ChatMensaje mensaje) {
		return new ChatMensajeResponse(
				mensaje.getId(),
				mensaje.getTicket().getId(),
				mensaje.getRemitente().getId(),
				mensaje.getRemitente().getRol(),
				mensaje.getContenido(),
				mensaje.getFechaEnvio(),
				mensaje.getFechaEntrega());
	}

	/** Lo recibido de la queue se arma desde el body del mensaje JMS, no desde la base. */
	public static ChatMensajeResponse recibido(ChatMensajeJms mensaje, Instant fechaEntrega) {
		return new ChatMensajeResponse(
				mensaje.mensajeId(),
				mensaje.ticketId(),
				mensaje.remitenteId(),
				mensaje.remitenteRol(),
				mensaje.contenido(),
				mensaje.timestamp(),
				fechaEntrega);
	}
}
