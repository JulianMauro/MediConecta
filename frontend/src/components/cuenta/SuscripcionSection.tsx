import { useEffect, useState } from 'react';
import { membresiasApi, suscripcionesApi } from '../../api/membresias';
import { ApiError } from '../../api/client';
import type { MembresiaResponse, SuscripcionResponse } from '../../types/dto';

const ESTADO_LABEL: Record<SuscripcionResponse['estado'], string> = {
	ACTIVA: 'Activo',
	VENCIDA: 'Vencido',
	CANCELADA: 'Cancelado',
};

export function SuscripcionSection() {
	const [membresias, setMembresias] = useState<MembresiaResponse[]>([]);
	const [suscripciones, setSuscripciones] = useState<SuscripcionResponse[]>([]);
	const [mensaje, setMensaje] = useState<string | null>(null);
	const [error, setError] = useState<string | null>(null);
	const [contratando, setContratando] = useState<number | null>(null);

	function recargar() {
		membresiasApi.listar().then((lista) => setMembresias(lista.filter((m) => m.activa)));
		suscripcionesApi.listarPropias().then(setSuscripciones);
	}

	useEffect(recargar, []);

	const ahora = Date.now();
	const planVigente = suscripciones.find((s) => s.estado === 'ACTIVA' && (!s.fechaFin || new Date(s.fechaFin).getTime() > ahora));

	async function contratar(membresiaId: number) {
		setError(null);
		setMensaje(null);
		setContratando(membresiaId);
		try {
			const pago = await suscripcionesApi.contratar({ membresiaId });
			setMensaje(`Se generó el pago #${pago.id} por $${pago.monto}. Confirmalo en "Pagos" (más abajo) para poder sacar una bici.`);
			recargar();
		} catch (err) {
			setError(err instanceof ApiError ? err.message : 'no se pudo contratar');
		} finally {
			setContratando(null);
		}
	}

	async function cancelar(id: number) {
		await suscripcionesApi.cancelar(id);
		recargar();
	}

	return (
		<section className="tarjeta">
			<h2>Suscripción</h2>
			<p className="ayuda">
				Sin un plan vigente no podés sacar una bici. Un plan te da minutos por viaje, una cantidad de viajes por día,
				y una espera mínima entre viaje y viaje — todo eso hasta que el plan vence.
			</p>

			{planVigente ? (
				<p className="aviso aviso-ok">
					Tenés un plan vigente hasta {planVigente.fechaFin ? new Date(planVigente.fechaFin).toLocaleString() : '-'}.
					Cancelalo si querés contratar otro distinto.
				</p>
			) : (
				<div className="grilla-planes">
					{membresias.map((m) => (
						<div key={m.id} className="plan-card">
							<h3>{m.nombre}</h3>
							<p className="plan-precio">${m.precio}</p>
							<ul className="plan-detalle">
								<li>Vigente {m.duracionDias} día(s) desde que lo pagás</li>
								<li>{m.tiempoPermitidoMinutos} min incluidos por viaje</li>
								<li>Hasta {m.viajesPorDia} viaje(s) por día</li>
								<li>{m.tiempoEsperaMinutos > 0 ? `${m.tiempoEsperaMinutos} min de espera entre viajes` : 'Sin espera entre viajes'}</li>
								<li>${m.tarifaMinutoExtra} por cada minuto extra</li>
							</ul>
							<button onClick={() => contratar(m.id)} disabled={contratando === m.id}>
								{contratando === m.id ? 'Generando pago...' : 'Contratar'}
							</button>
						</div>
					))}
					{membresias.length === 0 && (
						<p className="ayuda">Todavía no hay planes disponibles para contratar — hablalo con un admin.</p>
					)}
				</div>
			)}

			{mensaje && <p className="aviso aviso-ok">{mensaje}</p>}
			{error && <p className="error">{error}</p>}

			{suscripciones.length > 0 && (
				<table className="tabla">
					<thead>
						<tr>
							<th>Plan</th>
							<th>Estado</th>
							<th>Desde</th>
							<th>Hasta</th>
							<th></th>
						</tr>
					</thead>
					<tbody>
						{suscripciones.map((s) => (
							<tr key={s.id}>
								<td>#{s.membresiaId}</td>
								<td>{ESTADO_LABEL[s.estado]}</td>
								<td>{new Date(s.fechaInicio).toLocaleDateString()}</td>
								<td>{s.fechaFin ? new Date(s.fechaFin).toLocaleString() : '-'}</td>
								<td>
									{s.estado === 'ACTIVA' && (
										<button className="boton-chico" onClick={() => cancelar(s.id)}>
											Cancelar
										</button>
									)}
								</td>
							</tr>
						))}
					</tbody>
				</table>
			)}
		</section>
	);
}
