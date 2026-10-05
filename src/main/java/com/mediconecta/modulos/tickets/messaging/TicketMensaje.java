package com.mediconecta.modulos.tickets.messaging;

import java.time.Instant;

import com.mediconecta.modulos.tickets.entity.CategoriaTicket;


/** Body JSON que viaja por la queue tickets.nuevos (TextMessage via Jackson). */
public record TicketMensaje(
		Long ticketId,
		Long usuarioId,
		Long viajeId,
		CategoriaTicket categoria,
		String motivo,
		Instant timestamp
) {
}
