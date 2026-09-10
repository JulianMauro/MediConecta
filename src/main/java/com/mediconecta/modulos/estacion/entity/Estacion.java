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

	/** Datos editables desde el panel. La capacidad la valida el service contra los anclajes ya creados. */
	public void actualizarDatos(String nombre, String direccion, int capacidad) {
		this.nombre = nombre;
		this.direccion = direccion;
		this.capacidad = capacidad;
	}

	/**
	 * Borra las coordenadas.
	 *
	 * Se usa cuando cambia la direccion y no se pueden resolver las nuevas: dejar
	 * las viejas seria peor que no tener ninguna, porque la estacion aparaceria en
	 * el mapa en un lugar donde ya no esta.
	 */
	public void limpiarCoordenadas() {
		this.latitud = null;
		this.longitud = null;
	}

	/**
	 * Completa las coordenadas resueltas a partir de la direccion.
	 *
	 * Existe como metodo propio y no como setter porque la direccion y las
	 * coordenadas tienen que describir el mismo lugar: se asignan juntas, nunca
	 * una sin la otra.
	 */
	public void asignarCoordenadas(double latitud, double longitud) {
		this.latitud = latitud;
		this.longitud = longitud;
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
