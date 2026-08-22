package com.mediconecta.modulos.membresia.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.mediconecta.modulos.membresia.entity.TipoMembresia;


public record MembresiaRequest(

		@NotBlank(message = "el nombre es obligatorio")
		@Size(max = 100, message = "el nombre no puede superar los 100 caracteres")
		String nombre,

		@NotNull(message = "el tipo es obligatorio")
		TipoMembresia tipo,

		@Positive(message = "el tiempo permitido debe ser mayor a 0")
		int tiempoPermitidoMinutos,

		@NotNull(message = "el precio es obligatorio")
		@DecimalMin(value = "0.0", inclusive = true, message = "el precio no puede ser negativo")
		BigDecimal precio,

		@NotNull(message = "la tarifa por minuto extra es obligatoria")
		@DecimalMin(value = "0.0", inclusive = true, message = "la tarifa por minuto extra no puede ser negativa")
		BigDecimal tarifaMinutoExtra,

		@Positive(message = "los viajes por dia deben ser mayor a 0")
		int viajesPorDia,

		@PositiveOrZero(message = "el tiempo de espera no puede ser negativo")
		int tiempoEsperaMinutos
) {
}
