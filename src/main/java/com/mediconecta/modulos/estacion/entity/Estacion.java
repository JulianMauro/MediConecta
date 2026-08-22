package com.mediconecta.modulos.estacion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "estacion")
public class Estacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(nullable = false, length = 255)
	private String direccion;

	private Double latitud;

	private Double longitud;

	/** Cantidad total de anclajes que tiene la estacion. */
	@Column(nullable = false)
	private int capacidad;

	@Column(nullable = false)
	private boolean activa;

	protected Estacion() {
	}

	public Estacion(String nombre, String direccion, Double latitud, Double longitud, int capacidad) {
		this.nombre = nombre;
		this.direccion = direccion;
		this.latitud = latitud;
		this.longitud = longitud;
		this.capacidad = capacidad;
		this.activa = true;
	}

	public void desactivar() {
		this.activa = false;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getDireccion() {
		return direccion;
	}

	public Double getLatitud() {
		return latitud;
	}

	public Double getLongitud() {
		return longitud;
	}

	public int getCapacidad() {
		return capacidad;
	}

	public boolean isActiva() {
		return activa;
	}
}
