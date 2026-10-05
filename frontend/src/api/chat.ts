import { api } from './client';
import type { ChatMensajeResponse } from '../types/dto';

export const chatApi = {
	enviar: (ticketId: number, contenido: string) =>
		api.post<ChatMensajeResponse>(`/api/chat/${ticketId}/mensajes`, { contenido }),
	/** Consume de la queue chat.p2p lo pendiente para el usuario logueado: cada mensaje llega una sola vez. */
	nuevos: (ticketId: number) => api.get<ChatMensajeResponse[]>(`/api/chat/${ticketId}/mensajes/nuevos`),
	historial: (ticketId: number) => api.get<ChatMensajeResponse[]>(`/api/chat/${ticketId}/mensajes`),
};
