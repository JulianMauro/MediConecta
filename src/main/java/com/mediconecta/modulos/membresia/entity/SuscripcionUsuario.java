package com.mediconecta.modulos.membresia.entity;

import java.time.Instant;

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

import com.mediconecta.modulos.usuario.entity.Usuario;


/**
 * Contratacion puntual de un plan por parte de un usuario. Los viajes que corren bajo
 * esta suscripcion la referencian directo (Viaje.suscripcion): asi el limite de viajes
 * por dia y la espera entre viajes se miden por contratacion, no por usuario a secas
 * (si contratas un plan nuevo, no arrastra el historial del plan anterior).
 */
@Entity
@Table(name = "suscripcion_usuario")
public class SuscripcionUsuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "membresia_id", nullable = false)
	private Membresia membresia;

	@Column(name = "fecha_inicio", nullable = false)
	private Instant fechaInicio;

	@Column(name = "fecha_fin")
	private Instant fechaFin;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoSuscripcion estado;

	protected SuscripcionUsuario() {
	}

	public SuscripcionUsuario(Usuario usuario, Membresia membresia, Instant fechaInicio, Instant fechaFin) {
		this.usuario = usuario;
		this.membresia = membresia;
		this.fechaInicio = fechaInicio;
		this.fechaFin = fechaFin;
		this.estado = EstadoSuscripcion.ACTIVA;
	}

	public void cancelar() {
		this.estado = EstadoSuscripcion.CANCELADA;
	}

	public void vencer() {
		this.estado = EstadoSuscripcion.VENCIDA;
	}

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public Membresia getMembresia() {
		return membresia;
	}

	public Instant getFechaInicio() {
		return fechaInicio;
	}

	public Instant getFechaFin() {
		return fechaFin;
	}

	public EstadoSuscripcion getEstado() {
		return estado;
	}
}
