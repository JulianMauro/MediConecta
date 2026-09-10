package com.mediconecta.modulos.pago.entity;

import java.math.BigDecimal;
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

import com.mediconecta.modulos.membresia.entity.Membresia;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.viaje.entity.Viaje;


@Entity
@Table(name = "pago")
public class Pago {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	/** Null cuando el pago es de una membresia recurrente no ligada a un viaje puntual. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "viaje_id")
	private Viaje viaje;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "membresia_id")
	private Membresia membresia;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ConceptoPago concepto;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal monto;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoPago estado;

	@Column(name = "fecha_creacion", nullable = false)
	private Instant fechaCreacion;

	@Column(name = "fecha_resolucion")
	private Instant fechaResolucion;

	/** Id de la operacion en la pasarela de pago externa. */
	@Column(name = "referencia_pasarela", length = 100)
	private String referenciaPasarela;

	protected Pago() {
	}

	public Pago(Usuario usuario, Viaje viaje, Membresia membresia, ConceptoPago concepto, BigDecimal monto) {
		this.usuario = usuario;
		this.viaje = viaje;
		this.membresia = membresia;
		this.concepto = concepto;
		this.monto = monto;
		this.estado = EstadoPago.PENDIENTE;
		this.fechaCreacion = Instant.now();
	}

	/**
	 * Confirma el pago. Devuelve true solo si ESTE llamado hizo la transicion.
	 *
	 * La guarda no es defensiva de mas: cuando entre Mercado Pago, la confirmacion
	 * va a llegar por webhook, y MP no garantiza entrega unica — documenta que las
	 * notificaciones pueden repetirse y llegar desordenadas, y las reintenta hasta
	 * recibir un 2xx. Sin esto, cada reintento sobre un pago de MEMBRESIA generaba
	 * una suscripcion nueva. Tambien cubre el doble clic y el reintento de red.
	 *
	 * Que devuelva boolean en vez de tirar excepcion es a proposito: un reintento
	 * no es un error, es el caso normal. Quien llama decide que hacer con el.
	 */
	public boolean confirmar(String referenciaPasarela) {
		if (this.estado == EstadoPago.PAGADO) {
			return false;
		}
		this.estado = EstadoPago.PAGADO;
		this.referenciaPasarela = referenciaPasarela;
		this.fechaResolucion = Instant.now();
		return true;
	}

	/** Un pago ya resuelto no vuelve atras: rechazar solo aplica a los PENDIENTE. */
	public boolean rechazar() {
		if (this.estado != EstadoPago.PENDIENTE) {
			return false;
		}
		this.estado = EstadoPago.RECHAZADO;
		this.fechaResolucion = Instant.now();
		return true;
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

	public Membresia getMembresia() {
		return membresia;
	}

	public ConceptoPago getConcepto() {
		return concepto;
	}

	public BigDecimal getMonto() {
		return monto;
	}

	public EstadoPago getEstado() {
		return estado;
	}

	public Instant getFechaCreacion() {
		return fechaCreacion;
	}

	public Instant getFechaResolucion() {
		return fechaResolucion;
	}

	public String getReferenciaPasarela() {
		return referenciaPasarela;
	}
}
