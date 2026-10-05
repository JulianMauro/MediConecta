package com.mediconecta.modulos.tickets.entity;

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


/**
 * Incidencia que reporta un cliente (ej. no pudo anclar la bici, duda durante el viaje).
 * Nace ABIERTO y sin admin asignado: el primero que lo toma pasa a EN_CHAT si hace falta
 * hablar con el usuario, o lo resuelve directo. RESUELTO es el estado final en ambos casos.
 */
@Entity
@Table(name = "ticket")
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	/** Viaje que origino el ticket. Null si no vino de un viaje puntual. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "viaje_id")
	private Viaje viaje;

	/** Admin que tomo el ticket. Null mientras esta ABIERTO sin asignar. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "admin_id")
	private Usuario admin;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CategoriaTicket categoria;

	@Column(nullable = false, length = 500)
	private String motivo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoTicket estado;

	@Column(name = "fecha_creacion", nullable = false)
	private Instant fechaCreacion;

	@Column(name = "fecha_cierre")
	private Instant fechaCierre;

	/** La marca el TicketListener al consumir el mensaje de tickets.nuevos: prueba que la cola funciono. */
	@Column(name = "fecha_notificacion_admin")
	private Instant fechaNotificacionAdmin;

	protected Ticket() {
	}

	public Ticket(Usuario usuario, Viaje viaje, CategoriaTicket categoria, String motivo) {
		this.usuario = usuario;
		this.viaje = viaje;
		this.categoria = categoria;
		this.motivo = motivo;
		this.estado = EstadoTicket.ABIERTO;
		this.fechaCreacion = Instant.now();
	}

	public void marcarNotificado() {
		this.fechaNotificacionAdmin = Instant.now();
	}

	public void tomar(Usuario admin) {
		this.admin = admin;
		this.estado = EstadoTicket.EN_CHAT;
	}

	public void resolver() {
		this.estado = EstadoTicket.RESUELTO;
		this.fechaCierre = Instant.now();
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

	public Usuario getAdmin() {
		return admin;
	}

	public CategoriaTicket getCategoria() {
		return categoria;
	}

	public String getMotivo() {
		return motivo;
	}

	public EstadoTicket getEstado() {
		return estado;
	}

	public Instant getFechaCreacion() {
		return fechaCreacion;
	}

	public Instant getFechaCierre() {
		return fechaCierre;
	}

	public Instant getFechaNotificacionAdmin() {
		return fechaNotificacionAdmin;
	}
}
