package com.mediconecta.modulos.logistica.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.mediconecta.modulos.flota.entity.Bicicleta;


/** Una linea de detalle de MovimientoBicis: una bici puntual dentro del lote trasladado. */
@Entity
@Table(name = "movimiento_bici_item")
public class MovimientoBiciItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "movimiento_id", nullable = false)
	private MovimientoBicis movimiento;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bicicleta_id", nullable = false)
	private Bicicleta bicicleta;

	protected MovimientoBiciItem() {
	}

	public MovimientoBiciItem(MovimientoBicis movimiento, Bicicleta bicicleta) {
		this.movimiento = movimiento;
		this.bicicleta = bicicleta;
	}

	public Long getId() {
		return id;
	}

	public MovimientoBicis getMovimiento() {
		return movimiento;
	}

	public Bicicleta getBicicleta() {
		return bicicleta;
	}
}
