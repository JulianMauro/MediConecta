import { api } from './client';
import type { NotificacionResponse } from '../types/dto';

export const notificacionesApi = {
	listarPropias: () => api.get<NotificacionResponse[]>('/api/notificaciones'),
	listarNoLeidas: () => api.get<NotificacionResponse[]>('/api/notificaciones/no-leidas'),
	marcarLeida: (id: number) => api.patch<NotificacionResponse>(`/api/notificaciones/${id}/leida`),
};
