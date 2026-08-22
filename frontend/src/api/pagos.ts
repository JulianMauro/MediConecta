import { api } from './client';
import type { PagoConfirmarRequest, PagoResponse } from '../types/dto';

export const pagosApi = {
	listarPropios: () => api.get<PagoResponse[]>('/api/pagos'),
	obtener: (id: number) => api.get<PagoResponse>(`/api/pagos/${id}`),
	confirmar: (id: number, req: PagoConfirmarRequest) => api.post<PagoResponse>(`/api/pagos/${id}/confirmar`, req),
	rechazar: (id: number) => api.post<PagoResponse>(`/api/pagos/${id}/rechazar`),
};
