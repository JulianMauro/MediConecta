import { api } from './client';
import type { UsuariosActivosResponse } from '../types/dto';

export const monitoreoApi = {
	obtenerUsuariosActivos: () => api.get<UsuariosActivosResponse>('/api/monitoreo/usuarios-activos'),
	listarHistorial: () => api.get<UsuariosActivosResponse[]>('/api/monitoreo/usuarios-activos/historial'),
};
