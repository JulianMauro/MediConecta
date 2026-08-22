package com.mediconecta.modulos.strike.entity;

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

import com.mediconecta.modulos.pago.entity.Pago;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.viaje.entity.Viaje;


/**
 * Se genera cuando un viaje excede el tiempo permitido. No prescribe con el tiempo:
 * queda ACTIVO hasta que se salda pagando el tiempo extra (ver Pago). Al acumular
 * 3 strikes ACTIVO se dispara un BloqueoCuenta.
 */
@Entity
@Table(name = "strike")
public class Strike {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "viaje_id", nullable = false)
	private Viaje viaje;

	@Column(name = "fecha_generacion", nullable = false)
	private Instant fechaGeneracion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoStrike estado;

	/** Pago de tiempo extra que, al pasar a PAGADO, salda este strike. Null hasta que se genera el cobro. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pago_id")
	private Pago pago;

	protected Strike() {
	}

	public Strike(Usuario usuario, Viaje viaje) {
		this.usuario = usuario;
		this.viaje = viaje;
		this.fechaGeneracion = Instant.now();
		this.estado = EstadoStrike.ACTIVO;
	}

	public void asociarPago(Pago pago) {
		this.pago = pago;
	}

	public void saldar() {
		this.estado = EstadoStrike.SALDADO;
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

	public Instant getFechaGeneracion() {
		return fechaGeneracion;
	}

	public EstadoStrike getEstado() {
		return estado;
	}

	public Pago getPago() {
		return pago;
	}
}
