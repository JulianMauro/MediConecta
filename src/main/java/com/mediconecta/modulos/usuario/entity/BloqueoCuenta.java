package com.mediconecta.modulos.usuario.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


/** Historial de bloqueos de una cuenta. Se crea al acumular 3 strikes activos. */
@Entity
@Table(name = "bloqueo_cuenta")
public class BloqueoCuenta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(name = "fecha_bloqueo", nullable = false)
	private Instant fechaBloqueo;

	@Column(nullable = false, length = 255)
	private String motivo;

	@Column(nullable = false)
	private boolean activo;

	@Column(name = "fecha_desbloqueo")
	private Instant fechaDesbloqueo;

	/** Admin que levanto el bloqueo manualmente. Null mientras siga activo. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "admin_desbloqueo_id")
	private Usuario adminDesbloqueo;

	protected BloqueoCuenta() {
	}

	public BloqueoCuenta(Usuario usuario, String motivo) {
		this.usuario = usuario;
		this.motivo = motivo;
		this.fechaBloqueo = Instant.now();
		this.activo = true;
	}

	public void desbloquear(Usuario admin) {
		this.activo = false;
		this.fechaDesbloqueo = Instant.now();
		this.adminDesbloqueo = admin;
	}

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public Instant getFechaBloqueo() {
		return fechaBloqueo;
	}

	public String getMotivo() {
		return motivo;
	}

	public boolean isActivo() {
		return activo;
	}

	public Instant getFechaDesbloqueo() {
		return fechaDesbloqueo;
	}

	public Usuario getAdminDesbloqueo() {
		return adminDesbloqueo;
	}
}
