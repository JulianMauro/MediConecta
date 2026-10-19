import { api } from './client';
import type { CerrarViajeRequest, ResultadoSoporteResponse, ResumenUsuarioResponse } from '../types/dto';

/** Todo cuelga del ticket: el backend opera siempre sobre su dueño, y solo si el admin es el que lo tomo. */
const base = (ticketId: number) => `/api/soporte/tickets/${ticketId}`;

export const soporteApi = {
	resumen: (ticketId: number) => api.get<ResumenUsuarioResponse>(`${base(ticketId)}/usuario`),
	cerrarViaje: (ticketId: number, req: CerrarViajeRequest) =>
		api.post<ResultadoSoporteResponse>(`${base(ticketId)}/cerrar-viaje`, req),
	corregirViaje: (ticketId: number, viajeId: number, fechaFin: string, motivo: string) =>
		api.post<ResultadoSoporteResponse>(`${base(ticketId)}/viajes/${viajeId}/corregir`, { fechaFin, motivo }),
	anularStrike: (ticketId: number, strikeId: number, motivo: string) =>
		api.post<ResultadoSoporteResponse>(`${base(ticketId)}/strikes/${strikeId}/anular`, { motivo }),
	confirmarPago: (ticketId: number, pagoId: number, referenciaPasarela: string, motivo: string) =>
		api.post<ResultadoSoporteResponse>(`${base(ticketId)}/pagos/${pagoId}/confirmar`, { referenciaPasarela, motivo }),
	desbloquear: (ticketId: number, motivo: string) =>
		api.post<ResultadoSoporteResponse>(`${base(ticketId)}/desbloquear`, { motivo }),
	anclajeFueraDeServicio: (ticketId: number, anclajeId: number, motivo: string) =>
		api.post<ResultadoSoporteResponse>(`${base(ticketId)}/anclajes/${anclajeId}/fuera-servicio`, { motivo }),
};
