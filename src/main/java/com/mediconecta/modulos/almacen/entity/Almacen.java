package com.mediconecta.modulos.almacen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


/** Deposito donde se guardan y reparan bicis desactivadas. Gestion exclusiva de administradores. */
@Entity
@Table(name = "almacen")
public class Almacen {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(nullable = false, length = 255)
	private String direccion;

	@Column(nullable = false)
	private int capacidad;

	protected Almacen() {
	}

	public Almacen(String nombre, String direccion, int capacidad) {
		this.nombre = nombre;
		this.direccion = direccion;
		this.capacidad = capacidad;
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

	public int getCapacidad() {
		return capacidad;
	}
}
