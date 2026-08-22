package com.mediconecta.modulos.estacion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


/** Anclaje (dock) fisico dentro de una estacion, donde se traba/destraba una bicicleta. */
@Entity
@Table(name = "anclaje", uniqueConstraints = @UniqueConstraint(columnNames = { "estacion_id", "numero" }))
public class Anclaje {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estacion_id", nullable = false)
	private Estacion estacion;

	/** Numero de posicion dentro de la estacion (unico por estacion, no global). */
	@Column(nullable = false)
	private int numero;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoDock estado;

	protected Anclaje() {
	}

	public Anclaje(Estacion estacion, int numero) {
		this.estacion = estacion;
		this.numero = numero;
		this.estado = EstadoDock.LIBRE;
	}

	public void ocupar() {
		this.estado = EstadoDock.OCUPADO;
	}

	public void liberar() {
		this.estado = EstadoDock.LIBRE;
	}

	public void marcarFueraServicio() {
		this.estado = EstadoDock.FUERA_SERVICIO;
	}

	public Long getId() {
		return id;
	}

	public Estacion getEstacion() {
		return estacion;
	}

	public int getNumero() {
		return numero;
	}

	public EstadoDock getEstado() {
		return estado;
	}
}
