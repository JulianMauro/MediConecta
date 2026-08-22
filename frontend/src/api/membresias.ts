import { api } from './client';
import type { MembresiaRequest, MembresiaResponse, SuscripcionRequest, SuscripcionResponse, PagoResponse } from '../types/dto';

export const membresiasApi = {
	listar: () => api.get<MembresiaResponse[]>('/api/membresias'),
	obtener: (id: number) => api.get<MembresiaResponse>(`/api/membresias/${id}`),
	crear: (req: MembresiaRequest) => api.post<MembresiaResponse>('/api/membresias', req),
	desactivar: (id: number) => api.del<void>(`/api/membresias/${id}`),
};

export const suscripcionesApi = {
	listarPropias: () => api.get<SuscripcionResponse[]>('/api/suscripciones'),
	contratar: (req: SuscripcionRequest) => api.post<PagoResponse>('/api/suscripciones', req),
	cancelar: (id: number) => api.del<void>(`/api/suscripciones/${id}`),
};
