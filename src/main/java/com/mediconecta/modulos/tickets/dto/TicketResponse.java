package com.mediconecta.modulos.tickets.dto;

import java.time.Instant;

import com.mediconecta.modulos.tickets.entity.CategoriaTicket;
import com.mediconecta.modulos.tickets.entity.EstadoTicket;
import com.mediconecta.modulos.tickets.entity.Ticket;

public record TicketResponse(
		Long id,
		Long usuarioId,
		Long viajeId,
		Long adminId,
		CategoriaTicket categoria,
		String motivo,
		EstadoTicket estado,
		Instant fechaCreacion,
		Instant fechaCierre
) {

	public static TicketResponse desde(Ticket ticket) {
		return new TicketResponse(
				ticket.getId(),
				ticket.getUsuario().getId(),
				ticket.getViaje() != null ? ticket.getViaje().getId() : null,
				ticket.getAdmin() != null ? ticket.getAdmin().getId() : null,
				ticket.getCategoria(),
				ticket.getMotivo(),
				ticket.getEstado(),
				ticket.getFechaCreacion(),
				ticket.getFechaCierre());
	}
}
