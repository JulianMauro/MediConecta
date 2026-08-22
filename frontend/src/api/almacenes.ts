import { api } from './client';
import type { AlmacenRequest, AlmacenResponse } from '../types/dto';

export const almacenesApi = {
	listar: () => api.get<AlmacenResponse[]>('/api/almacenes'),
	obtener: (id: number) => api.get<AlmacenResponse>(`/api/almacenes/${id}`),
	crear: (req: AlmacenRequest) => api.post<AlmacenResponse>('/api/almacenes', req),
};
