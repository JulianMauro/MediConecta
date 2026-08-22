package com.mediconecta.modulos.membresia.dto;

import jakarta.validation.constraints.NotNull;


public record SuscripcionRequest(

		@NotNull(message = "la membresia es obligatoria")
		Long membresiaId
) {
}
