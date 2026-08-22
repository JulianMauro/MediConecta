package com.mediconecta.modulos.membresia.entity;


/** Define cuanto dura la suscripcion una vez contratada y pagada. */
public enum TipoMembresia {

	INDIVIDUAL(1),
	SEMANAL(7),
	MENSUAL(30);

	private final int duracionDias;

	TipoMembresia(int duracionDias) {
		this.duracionDias = duracionDias;
	}

	public int duracionDias() {
		return duracionDias;
	}
}
