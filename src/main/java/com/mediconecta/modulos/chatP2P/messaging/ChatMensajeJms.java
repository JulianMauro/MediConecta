package com.mediconecta.modulos.chatP2P.messaging;

import java.time.Instant;

import com.mediconecta.modulos.usuario.entity.Rol;


/**
 * Body JSON que viaja por la queue chat.p2p (TextMessage via Jackson). ticketId y
 * destinatarioId viajan ademas como propiedades JMS: el selector filtra por propiedades,
 * no puede mirar adentro del body.
 */
public record ChatMensajeJms(
		Long mensajeId,
		Long ticketId,
		Long remitenteId,
		Rol remitenteRol,
		Long destinatarioId,
		String contenido,
		Instant timestamp
) {
}
