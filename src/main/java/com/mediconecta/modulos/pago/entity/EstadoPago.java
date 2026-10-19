package com.mediconecta.modulos.pago.entity;


public enum EstadoPago {

	PENDIENTE,
	PAGADO,
	RECHAZADO,
	/** Soporte cancelo el cobro (al anular el strike que lo genero). Ya no se puede pagar. */
	ANULADO
}
