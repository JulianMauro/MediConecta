package com.mediconecta.modulos.notificacion.entity;

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
import com.mediconecta.modulos.viaje.entity.Viaje;


@Entity
@Table(name = "notificacion")
public class Notificacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	/** Viaje que origino la notificacion. Null para avisos que no vienen de un viaje puntual (ej. bloqueo). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "viaje_id")
	private Viaje viaje;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoNotificacion tipo;

	@Column(nullable = false, length = 500)
	private String mensaje;

	@Column(name = "fecha_envio", nullable = false)
	private Instant fechaEnvio;

	@Column(nullable = false)
	private boolean leida;

	protected Notificacion() {
	}

	public Notificacion(Usuario usuario, Viaje viaje, TipoNotificacion tipo, String mensaje) {
		this.usuario = usuario;
		this.viaje = viaje;
		this.tipo = tipo;
		this.mensaje = mensaje;
		this.fechaEnvio = Instant.now();
		this.leida = false;
	}

	public void marcarLeida() {
		this.leida = true;
	}

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public Viaje getViaje() {
		return viaje;
	}

	public TipoNotificacion getTipo() {
		return tipo;
	}

	public String getMensaje() {
		return mensaje;
	}

	public Instant getFechaEnvio() {
		return fechaEnvio;
	}

	public boolean isLeida() {
		return leida;
	}
}
