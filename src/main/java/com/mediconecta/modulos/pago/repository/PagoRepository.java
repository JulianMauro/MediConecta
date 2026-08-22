package com.mediconecta.modulos.pago.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.pago.entity.EstadoPago;
import com.mediconecta.modulos.pago.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {

	List<Pago> findByUsuarioIdAndEstado(Long usuarioId, EstadoPago estado);

	List<Pago> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);
}
