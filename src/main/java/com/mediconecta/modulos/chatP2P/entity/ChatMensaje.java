package com.mediconecta.modulos.chatP2P.entity;

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

import com.mediconecta.modulos.tickets.entity.Ticket;
import com.mediconecta.modulos.usuario.entity.Usuario;


/**
 * Historial del chat de un ticket. La entrega en vivo va por la queue chat.p2p; esta
 * tabla es para poder releer la conversacion entera. fechaEntrega se completa cuando
 * el destinatario consume el mensaje de la queue.
 */
@Entity
@Table(name = "chat_mensaje")
public class ChatMensaje {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "remitente_id", nullable = false)
	private Usuario remitente;

	@Column(nullable = false, length = 1000)
	private String contenido;

	@Column(name = "fecha_envio", nullable = false)
	private Instant fechaEnvio;

	@Column(name = "fecha_entrega")
	private Instant fechaEntrega;

	protected ChatMensaje() {
	}

	public ChatMensaje(Ticket ticket, Usuario remitente, String contenido) {
		this.ticket = ticket;
		this.remitente = remitente;
		this.contenido = contenido;
		this.fechaEnvio = Instant.now();
	}

	public void marcarEntregado(Instant fecha) {
		this.fechaEntrega = fecha;
	}

	public Long getId() {
		return id;
	}

	public Ticket getTicket() {
		return ticket;
	}

	public Usuario getRemitente() {
		return remitente;
	}

	public String getContenido() {
		return contenido;
	}

	public Instant getFechaEnvio() {
		return fechaEnvio;
	}

	public Instant getFechaEntrega() {
		return fechaEntrega;
	}
}
