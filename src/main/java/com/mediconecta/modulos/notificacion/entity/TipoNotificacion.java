package com.mediconecta.modulos.notificacion.entity;


public enum TipoNotificacion {

	TIEMPO_EXTRA,
	STRIKE,
	BLOQUEO,
	/** Algo que hizo un admin desde un ticket (cerrar el viaje, anular un strike, desbloquear...). */
	SOPORTE
}
