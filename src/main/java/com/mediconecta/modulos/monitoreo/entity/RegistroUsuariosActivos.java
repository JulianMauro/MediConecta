package com.mediconecta.modulos.monitoreo.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


/**
 * Foto periodica de cuantos usuarios estan en viaje en un momento dado. La toma
 * UsuariosActivosService cada una hora a partir del contador en memoria: es historial
 * (para graficar la demanda a lo largo del tiempo), no el valor en vivo, que se consulta
 * aparte sin pasar por la base.
 */
@Entity
@Table(name = "registro_usuarios_activos")
public class RegistroUsuariosActivos {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private int cantidad;

	@Column(nullable = false)
	private Instant timestamp;

	protected RegistroUsuariosActivos() {
	}

	public RegistroUsuariosActivos(int cantidad, Instant timestamp) {
		this.cantidad = cantidad;
		this.timestamp = timestamp;
	}

	public Long getId() {
		return id;
	}

	public int getCantidad() {
		return cantidad;
	}

	public Instant getTimestamp() {
		return timestamp;
	}
}
