import { useEffect, useState } from 'react';
import { notificacionesApi } from '../../api/notificaciones';
import type { NotificacionResponse, StrikeResponse } from '../../types/dto';

// Espejo de StrikeService.STRIKES_PARA_BLOQUEAR en el backend.
const STRIKES_PARA_BLOQUEAR = 3;

interface Props {
	// Vienen de useStrikes en "Mi cuenta": los mismos datos que la tabla de strikes.
	strikes: StrikeResponse[];
}

export function NotificacionesSection({ strikes }: Props) {
	const [notificaciones, setNotificaciones] = useState<NotificacionResponse[]>([]);

	function recargar() {
		notificacionesApi.listarPropias().then(setNotificaciones);
	}

	useEffect(recargar, []);

	async function marcarLeida(id: number) {
		await notificacionesApi.marcarLeida(id);
		recargar();
	}

	const activos = strikes.filter((s) => s.estado === 'ACTIVO').length;
	const nivel = activos === 0 ? 'ok' : activos >= STRIKES_PARA_BLOQUEAR ? 'bloqueo' : 'aviso';
	const restantes = Math.max(STRIKES_PARA_BLOQUEAR - activos, 0);

	return (
		<section id="notificaciones" className="tarjeta">
			<h2>Notificaciones</h2>
			<div
				className={`contador-strikes contador-strikes-${nivel}`}
				role="status"
				aria-label={`${activos} de ${STRIKES_PARA_BLOQUEAR} strikes activos`}
			>
				<div className="contador-strikes-cifra">
					<span className="contador-strikes-numero">{activos}</span>
					<span className="contador-strikes-total">/ {STRIKES_PARA_BLOQUEAR}</span>
				</div>
				<div className="contador-strikes-detalle">
					<span className="contador-strikes-titulo">Strikes activos</span>
					<ol className="contador-strikes-barra" aria-hidden="true">
						{Array.from({ length: STRIKES_PARA_BLOQUEAR }, (_, i) => (
							<li key={i} className={i < activos ? 'lleno' : ''} />
						))}
					</ol>
					<span className="contador-strikes-ayuda">
						{activos === 0 && 'Sin penalizaciones pendientes.'}
						{nivel === 'aviso' &&
							`Te ${restantes === 1 ? 'queda' : 'quedan'} ${restantes} antes del bloqueo. Pagá el tiempo extra para saldarlos.`}
						{nivel === 'bloqueo' && 'Cuenta bloqueada. Saldá los pagos de tiempo extra para desbloquearla.'}
					</span>
				</div>
			</div>
			<ul className="lista-notificaciones">
				{notificaciones.map((n) => (
					<li key={n.id} className={n.leida ? '' : 'notificacion-no-leida'}>
						<span>{n.mensaje}</span>
						{!n.leida && (
							<button className="boton-chico" onClick={() => marcarLeida(n.id)}>
								Marcar leída
							</button>
						)}
					</li>
				))}
				{notificaciones.length === 0 && (
					<li className="tabla-vacia">Cuando el sistema tenga algo que avisarte, va a aparecer acá.</li>
				)}
			</ul>
		</section>
	);
}
