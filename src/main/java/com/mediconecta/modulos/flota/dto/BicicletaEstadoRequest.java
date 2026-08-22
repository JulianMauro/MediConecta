package com.mediconecta.modulos.flota.dto;

import jakarta.validation.constraints.NotNull;

import com.mediconecta.modulos.flota.entity.EstadoBicicleta;


public record BicicletaEstadoRequest(

		@NotNull(message = "el estado es obligatorio")
		EstadoBicicleta estado
) {
}
