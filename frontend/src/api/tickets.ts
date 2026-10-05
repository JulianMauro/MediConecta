import { api } from './client';
import type { CategoriaTicket, TicketRequest, TicketResponse } from '../types/dto';

/** Sin categoria trae todos; con categoria filtra en el backend. */
const filtro = (categoria?: CategoriaTicket | '') => (categoria ? `?categoria=${categoria}` : '');

export const ticketsApi = {
	crear: (req: TicketRequest) => api.post<TicketResponse>('/api/tickets', req),
	listarPropios: (categoria?: CategoriaTicket | '') => api.get<TicketResponse[]>(`/api/tickets${filtro(categoria)}`),
	listarAbiertos: (categoria?: CategoriaTicket | '') =>
		api.get<TicketResponse[]>(`/api/tickets/abiertos${filtro(categoria)}`),
	listarAsignados: (categoria?: CategoriaTicket | '') =>
		api.get<TicketResponse[]>(`/api/tickets/asignados${filtro(categoria)}`),
	tomar: (id: number) => api.patch<TicketResponse>(`/api/tickets/${id}/tomar`),
	resolver: (id: number) => api.patch<TicketResponse>(`/api/tickets/${id}/resolver`),
};
