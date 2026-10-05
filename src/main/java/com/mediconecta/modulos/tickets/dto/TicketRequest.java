package com.mediconecta.modulos.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.mediconecta.modulos.tickets.entity.CategoriaTicket;


public record TicketRequest(

		/** Opcional: null si el problema no vino de un viaje puntual. */
		Long viajeId,

		@NotNull(message = "la categoria es obligatoria")
		CategoriaTicket categoria,

		@NotBlank(message = "el motivo es obligatorio")
		String motivo
) {
}
