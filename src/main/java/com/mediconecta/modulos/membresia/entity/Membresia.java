package com.mediconecta.modulos.membresia.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


/**
 * Catalogo de planes. Todo plan hay que contratarlo y pagarlo antes de poder viajar:
 * no hay tarifa "pay as you go" automatica. Mientras el plan esta vigente, limita
 * cuantos viajes por dia y cuanto tiempo de espera hay entre uno y el siguiente.
 */
@Entity
@Table(name = "membresia")
public class Membresia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoMembresia tipo;

	/** Minutos de viaje permitidos antes de considerarse tiempo excedido. */
	@Column(name = "tiempo_permitido_minutos", nullable = false)
	private int tiempoPermitidoMinutos;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal precio;

	/** Cobro por cada minuto que un viaje se pasa de tiempoPermitidoMinutos. */
	@Column(name = "tarifa_minuto_extra", nullable = false, precision = 10, scale = 2)
	private BigDecimal tarifaMinutoExtra;

	/** Cuantos viajes puede iniciar el usuario por dia mientras este plan esta vigente. */
	@Column(name = "viajes_por_dia", nullable = false)
	private int viajesPorDia;

	/** Minutos de espera obligatorios entre que se devuelve una bici y se puede sacar otra. */
	@Column(name = "tiempo_espera_minutos", nullable = false)
	private int tiempoEsperaMinutos;

	@Column(nullable = false)
	private boolean activa;

	protected Membresia() {
	}

	public Membresia(String nombre, TipoMembresia tipo, int tiempoPermitidoMinutos, BigDecimal precio,
			BigDecimal tarifaMinutoExtra, int viajesPorDia, int tiempoEsperaMinutos) {
		this.nombre = nombre;
		this.tipo = tipo;
		this.tiempoPermitidoMinutos = tiempoPermitidoMinutos;
		this.precio = precio;
		this.tarifaMinutoExtra = tarifaMinutoExtra;
		this.viajesPorDia = viajesPorDia;
		this.tiempoEsperaMinutos = tiempoEsperaMinutos;
		this.activa = true;
	}

	public void desactivar() {
		this.activa = false;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public TipoMembresia getTipo() {
		return tipo;
	}

	public int getTiempoPermitidoMinutos() {
		return tiempoPermitidoMinutos;
	}

	public BigDecimal getPrecio() {
		return precio;
	}

	public BigDecimal getTarifaMinutoExtra() {
		return tarifaMinutoExtra;
	}

	public int getViajesPorDia() {
		return viajesPorDia;
	}

	public int getTiempoEsperaMinutos() {
		return tiempoEsperaMinutos;
	}

	public boolean isActiva() {
		return activa;
	}
}
