package com.mediconecta.modulos.viaje.entity;

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

import com.mediconecta.modulos.estacion.entity.Anclaje;
import com.mediconecta.modulos.flota.entity.Bicicleta;
import com.mediconecta.modulos.membresia.entity.Membresia;
import com.mediconecta.modulos.membresia.entity.SuscripcionUsuario;
import com.mediconecta.modulos.usuario.entity.Usuario;


@Entity
@Table(name = "viaje")
public class Viaje {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bicicleta_id", nullable = false)
	private Bicicleta bicicleta;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "anclaje_origen_id", nullable = false)
	private Anclaje anclajeOrigen;

	/** Null mientras el viaje sigue en curso. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "anclaje_destino_id")
	private Anclaje anclajeDestino;

	/** Catalogo del plan bajo el cual corrio el viaje (sus reglas: minutos incluidos, tarifa extra). */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "membresia_id", nullable = false)
	private Membresia membresia;

	/**
	 * La contratacion puntual que "pago" este viaje. Es lo que permite que el limite de
	 * viajes por dia y la espera entre viajes se midan por plan contratado, no por usuario
	 * a secas: un plan nuevo no hereda el historial de viajes del plan anterior.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "suscripcion_id", nullable = false)
	private SuscripcionUsuario suscripcion;

	@Column(name = "fecha_inicio", nullable = false)
	private Instant fechaInicio;

	@Column(name = "fecha_fin")
	private Instant fechaFin;

	/** Se calcula al cerrar el viaje comparando la duracion contra membresia.tiempoPermitidoMinutos. */
	@Column(name = "excedio_tiempo", nullable = false)
	private boolean excedioTiempo;

	protected Viaje() {
	}

	public Viaje(Usuario usuario, Bicicleta bicicleta, Anclaje anclajeOrigen, SuscripcionUsuario suscripcion) {
		this.usuario = usuario;
		this.bicicleta = bicicleta;
		this.anclajeOrigen = anclajeOrigen;
		this.suscripcion = suscripcion;
		this.membresia = suscripcion.getMembresia();
		this.fechaInicio = Instant.now();
		this.excedioTiempo = false;
	}

	public void finalizar(Anclaje anclajeDestino) {
		this.anclajeDestino = anclajeDestino;
		this.fechaFin = Instant.now();
		this.excedioTiempo = duracionMinutos() > membresia.getTiempoPermitidoMinutos();
	}

	public long duracionMinutos() {
		Instant fin = fechaFin != null ? fechaFin : Instant.now();
		return java.time.Duration.between(fechaInicio, fin).toMinutes();
	}

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public Bicicleta getBicicleta() {
		return bicicleta;
	}

	public Anclaje getAnclajeOrigen() {
		return anclajeOrigen;
	}

	public Anclaje getAnclajeDestino() {
		return anclajeDestino;
	}

	public Membresia getMembresia() {
		return membresia;
	}

	public SuscripcionUsuario getSuscripcion() {
		return suscripcion;
	}

	public Instant getFechaInicio() {
		return fechaInicio;
	}

	public Instant getFechaFin() {
		return fechaFin;
	}

	public boolean isExcedioTiempo() {
		return excedioTiempo;
	}
}
