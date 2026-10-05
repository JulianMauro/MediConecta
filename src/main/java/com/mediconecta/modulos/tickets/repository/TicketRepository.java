package com.mediconecta.modulos.tickets.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.tickets.entity.EstadoTicket;
import com.mediconecta.modulos.tickets.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	List<Ticket> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

	/** Cola de trabajo del admin: los que todavia no tomo nadie. */
	List<Ticket> findByEstadoOrderByFechaCreacionAsc(EstadoTicket estado);

	List<Ticket> findByAdminIdOrderByFechaCreacionDesc(Long adminId);
}
