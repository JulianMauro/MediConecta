package com.mediconecta.modulos.strike.entity;


public enum EstadoStrike {

	ACTIVO,
	SALDADO,
	/** Soporte lo dio de baja (ej. el exceso fue por una falla del sistema). No cuenta para el bloqueo. */
	ANULADO
}
