package com.mediconecta.modulos.soporte.entity;

import java.time.Instant;

import com.mediconecta.modulos.tickets.entity.Ticket;
import com.mediconecta.modulos.usuario.entity.Usuario;

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

/**
 * Registro de lo que hizo un admin sobre un usuario desde un ticket: quien, que, por que y
 * cuando. Se escribe una vez y no cambia. Las entidades afectadas (viaje, strike, pago,
 * bloqueo) guardan su estado nuevo; esta es la explicacion de por que cambiaron.
 */
@Entity
@Table(name = "accion_soporte")
public class AccionSoporte {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "admin_id", nullable = false)
	private Usuario admin;

	/** El usuario afectado: siempre el dueño del ticket. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TipoAccionSoporte tipo;

	/** Lo que cambio, en palabras: es el mismo texto que se le manda al usuario por el chat. */
	@Column(nullable = false, length = 600)
	private String detalle;

	/** Por que lo hizo el admin. Obligatorio. */
	@Column(nullable = false, length = 300)
	private String motivo;

	@Column(nullable = false)
	private Instant fecha;

	protected AccionSoporte() {
	}

	public AccionSoporte(Ticket ticket, Usuario admin, TipoAccionSoporte tipo, String detalle, String motivo) {
		this.ticket = ticket;
		this.admin = admin;
		this.usuario = ticket.getUsuario();
		this.tipo = tipo;
		this.detalle = detalle;
		this.motivo = motivo;
		this.fecha = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Ticket getTicket() {
		return ticket;
	}

	public Usuario getAdmin() {
		return admin;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public TipoAccionSoporte getTipo() {
		return tipo;
	}

	public String getDetalle() {
		return detalle;
	}

	public String getMotivo() {
		return motivo;
	}

	public Instant getFecha() {
		return fecha;
	}
}
