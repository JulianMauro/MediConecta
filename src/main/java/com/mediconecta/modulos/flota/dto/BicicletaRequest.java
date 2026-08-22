package com.mediconecta.modulos.flota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/** Alta de una bici: siempre entra a la flota por un almacen (se despliega despues con un movimiento). */
public record BicicletaRequest(

		@NotBlank(message = "el codigo es obligatorio")
		@Size(max = 30, message = "el codigo no puede superar los 30 caracteres")
		String codigo,

		@NotNull(message = "el almacen es obligatorio")
		Long almacenId
) {
}
