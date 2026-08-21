package com.mediconecta.modulos.saludo.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "saludo")
public class Saludo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(name = "fecha_creacion", nullable = false)
	private Instant fechaCreacion;

	/** Constructor vacio: lo necesita Hibernate para instanciar la entidad. */
	protected Saludo() {
	}

	public Saludo(String nombre) {
		this.nombre = nombre;
		this.fechaCreacion = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public Instant getFechaCreacion() {
		return fechaCreacion;
	}
}
