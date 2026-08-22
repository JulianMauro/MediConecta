package com.mediconecta.modulos.logistica.entity;

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

import com.mediconecta.modulos.almacen.entity.Almacen;
import com.mediconecta.modulos.estacion.entity.Estacion;
import com.mediconecta.modulos.usuario.entity.Usuario;


/**
 * Traslado en lote de bicis entre estacion/almacen, registrado por un admin
 * (simula el camion de redistribucion). El detalle de que bicis se movieron
 * vive en MovimientoBiciItem.
 *
 * Origen y destino son polimorficos entre Estacion y Almacen: se modelan con
 * dos pares de FK nullable (mismo criterio que Bicicleta.anclaje/almacen).
 * El invariante "exactamente uno de origenEstacion/origenAlmacen" y
 * "exactamente uno de destinoEstacion/destinoAlmacen" lo garantiza el service.
 */
@Entity
@Table(name = "movimiento_bicis")
public class MovimientoBicis {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "admin_id", nullable = false)
	private Usuario admin;

	@Column(nullable = false)
	private Instant fecha;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "origen_estacion_id")
	private Estacion origenEstacion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "origen_almacen_id")
	private Almacen origenAlmacen;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "destino_estacion_id")
	private Estacion destinoEstacion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "destino_almacen_id")
	private Almacen destinoAlmacen;

	protected MovimientoBicis() {
	}

	public MovimientoBicis(Usuario admin, Estacion origenEstacion, Almacen origenAlmacen,
			Estacion destinoEstacion, Almacen destinoAlmacen) {
		this.admin = admin;
		this.fecha = Instant.now();
		this.origenEstacion = origenEstacion;
		this.origenAlmacen = origenAlmacen;
		this.destinoEstacion = destinoEstacion;
		this.destinoAlmacen = destinoAlmacen;
	}

	public Long getId() {
		return id;
	}

	public Usuario getAdmin() {
		return admin;
	}

	public Instant getFecha() {
		return fecha;
	}

	public Estacion getOrigenEstacion() {
		return origenEstacion;
	}

	public Almacen getOrigenAlmacen() {
		return origenAlmacen;
	}

	public Estacion getDestinoEstacion() {
		return destinoEstacion;
	}

	public Almacen getDestinoAlmacen() {
		return destinoAlmacen;
	}
}
