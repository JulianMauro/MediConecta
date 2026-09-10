import { api } from './client';
import type {
	AnclajeRequest,
	AnclajeResponse,
	EstacionRequest,
	EstacionResponse,
	EstacionUpdateRequest,
} from '../types/dto';

export const estacionesApi = {
	listar: () => api.get<EstacionResponse[]>('/api/estaciones'),
	obtener: (id: number) => api.get<EstacionResponse>(`/api/estaciones/${id}`),
	crear: (req: EstacionRequest) => api.post<EstacionResponse>('/api/estaciones', req),
	actualizar: (id: number, req: EstacionUpdateRequest) =>
		api.put<EstacionResponse>(`/api/estaciones/${id}`, req),
	/** Recalcula lat/lng desde la direccion guardada. Falla con 422 si no se pueden resolver. */
	geocodificar: (id: number) => api.post<EstacionResponse>(`/api/estaciones/${id}/geocodificar`, undefined),
	desactivar: (id: number) => api.del<void>(`/api/estaciones/${id}`),

	listarAnclajes: (estacionId: number) => api.get<AnclajeResponse[]>(`/api/estaciones/${estacionId}/anclajes`),
	agregarAnclaje: (estacionId: number, req: AnclajeRequest) =>
		api.post<AnclajeResponse>(`/api/estaciones/${estacionId}/anclajes`, req),
	marcarFueraServicio: (anclajeId: number) => api.patch<AnclajeResponse>(`/api/anclajes/${anclajeId}/fuera-servicio`),
	habilitarAnclaje: (anclajeId: number) => api.patch<AnclajeResponse>(`/api/anclajes/${anclajeId}/habilitar`),
};
