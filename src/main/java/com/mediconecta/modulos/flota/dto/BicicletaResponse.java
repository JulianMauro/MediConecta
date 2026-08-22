package com.mediconecta.modulos.flota.dto;

import com.mediconecta.modulos.flota.entity.Bicicleta;
import com.mediconecta.modulos.flota.entity.EstadoBicicleta;

public record BicicletaResponse(
		Long id,
		String codigo,
		EstadoBicicleta estado,
		Long anclajeId,
		Long almacenId
) {

	public static BicicletaResponse desde(Bicicleta bicicleta) {
		return new BicicletaResponse(
				bicicleta.getId(),
				bicicleta.getCodigo(),
				bicicleta.getEstado(),
				bicicleta.getAnclaje() != null ? bicicleta.getAnclaje().getId() : null,
				bicicleta.getAlmacen() != null ? bicicleta.getAlmacen().getId() : null);
	}
}
