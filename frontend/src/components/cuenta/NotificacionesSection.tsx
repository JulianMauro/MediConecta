import { useEffect, useState } from 'react';
import { notificacionesApi } from '../../api/notificaciones';
import type { NotificacionResponse } from '../../types/dto';

export function NotificacionesSection() {
	const [notificaciones, setNotificaciones] = useState<NotificacionResponse[]>([]);

	function recargar() {
		notificacionesApi.listarPropias().then(setNotificaciones);
	}

	useEffect(recargar, []);

	async function marcarLeida(id: number) {
		await notificacionesApi.marcarLeida(id);
		recargar();
	}

	return (
		<section className="tarjeta">
			<h2>Notificaciones</h2>
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
			</ul>
		</section>
	);
}
