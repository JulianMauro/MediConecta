package com.mediconecta.modulos.chatP2P.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.chatP2P.entity.ChatMensaje;

public interface ChatMensajeRepository extends JpaRepository<ChatMensaje, Long> {

	List<ChatMensaje> findByTicketIdOrderByFechaEnvioAsc(Long ticketId);
}
