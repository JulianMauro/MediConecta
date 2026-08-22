package com.mediconecta.modulos.notificacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.notificacion.entity.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

	List<Notificacion> findByUsuarioIdAndLeidaFalse(Long usuarioId);

	List<Notificacion> findByUsuarioIdOrderByFechaEnvioDesc(Long usuarioId);
}
