import { api } from './client';
import type { BicicletaEstadoRequest, BicicletaRequest, BicicletaResponse } from '../types/dto';

export const bicicletasApi = {
	listar: () => api.get<BicicletaResponse[]>('/api/bicicletas'),
	obtener: (id: number) => api.get<BicicletaResponse>(`/api/bicicletas/${id}`),
	crear: (req: BicicletaRequest) => api.post<BicicletaResponse>('/api/bicicletas', req),
	cambiarEstado: (id: number, req: BicicletaEstadoRequest) =>
		api.patch<BicicletaResponse>(`/api/bicicletas/${id}/estado`, req),
};
