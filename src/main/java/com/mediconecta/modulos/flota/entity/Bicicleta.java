package com.mediconecta.modulos.flota.entity;

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

import com.mediconecta.modulos.almacen.entity.Almacen;
import com.mediconecta.modulos.estacion.entity.Anclaje;


@Entity
@Table(name = "bicicleta")
public class Bicicleta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Identificador fijo grabado en la bici: es lo que el usuario escanea/tipea para desbloquearla. */
	@Column(nullable = false, unique = true, length = 30)
	private String codigo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoBicicleta estado;

	/**
	 * Ubicacion actual: o esta en un anclaje, o esta en un almacen, nunca ambos.
	 * Se modela con dos FK nullable en vez de una referencia polimorfica: para
	 * el tamano de este dominio es mas simple, y el invariante "exactamente una
	 * de las dos" lo garantiza el service, no la base.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "anclaje_id")
	private Anclaje anclaje;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "almacen_id")
	private Almacen almacen;

	protected Bicicleta() {
	}

	/** Toda bici nueva entra por el almacen: se despliega a una estacion despues, via un MovimientoBicis. */
	public Bicicleta(String codigo, Almacen almacen) {
		this.codigo = codigo;
		this.estado = EstadoBicicleta.DESACTIVADA;
		this.almacen = almacen;
	}

	public void retirarDeAnclaje() {
		this.anclaje = null;
		this.estado = EstadoBicicleta.EN_VIAJE;
	}

	public void anclarEn(Anclaje anclaje) {
		this.anclaje = anclaje;
		this.almacen = null;
		this.estado = EstadoBicicleta.DISPONIBLE;
	}

	public void moverAAlmacen(Almacen almacen, EstadoBicicleta nuevoEstado) {
		this.almacen = almacen;
		this.anclaje = null;
		this.estado = nuevoEstado;
	}

	public Long getId() {
		return id;
	}

	public String getCodigo() {
		return codigo;
	}

	public EstadoBicicleta getEstado() {
		return estado;
	}

	public Anclaje getAnclaje() {
		return anclaje;
	}

	public Almacen getAlmacen() {
		return almacen;
	}
}
