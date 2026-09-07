import { useEffect, useState } from 'react';
import { viajesApi } from '../../api/viajes';
import { estacionesApi } from '../../api/estaciones';
import { membresiasApi } from '../../api/membresias';
import { ApiError } from '../../api/client';
import type { AnclajeResponse, EstacionResponse, MembresiaResponse, ViajeResponse } from '../../types/dto';

interface Props {
	viaje: ViajeResponse;
	onDevuelta: (viaje: ViajeResponse) => void;
}

function formatearDuracion(segundos: number): string {
	const minutos = Math.floor(segundos / 60);
	const seg = segundos % 60;
	return `${minutos.toString().padStart(2, '0')}:${seg.toString().padStart(2, '0')}`;
}

/**
 * No hay hardware real que "reciba" la bici: simular la devolucion es elegir una
 * estacion + un anclaje libre de esa estacion y confirmar. Es la unica forma de
 * cerrar el viaje sin un dock fisico de por medio.
 */
export function BikeModeCard({ viaje, onDevuelta }: Props) {
	const [segundosTranscurridos, setSegundosTranscurridos] = useState(0);
	const [membresia, setMembresia] = useState<MembresiaResponse | null>(null);

	const [estaciones, setEstaciones] = useState<EstacionResponse[]>([]);
	const [estacionId, setEstacionId] = useState<number | ''>('');
	const [anclajes, setAnclajes] = useState<AnclajeResponse[]>([]);
	const [anclajeId, setAnclajeId] = useState<number | ''>('');
	const [error, setError] = useState<string | null>(null);
	const [devolviendo, setDevolviendo] = useState(false);

	useEffect(() => {
		const inicio = new Date(viaje.fechaInicio).getTime();
		const actualizar = () => setSegundosTranscurridos(Math.max(0, Math.floor((Date.now() - inicio) / 1000)));
		actualizar();
		const intervalo = setInterval(actualizar, 1000);
		return () => clearInterval(intervalo);
	}, [viaje.fechaInicio]);

	useEffect(() => {
		membresiasApi.obtener(viaje.membresiaId).then(setMembresia).catch(() => setMembresia(null));
		estacionesApi.listar().then(setEstaciones).catch(() => setEstaciones([]));
	}, [viaje.membresiaId]);

	useEffect(() => {
		if (estacionId === '') {
			setAnclajes([]);
			return;
		}
		estacionesApi
			.listarAnclajes(estacionId)
			.then((lista) => setAnclajes(lista.filter((a) => a.estado === 'LIBRE')))
			.catch(() => setAnclajes([]));
	}, [estacionId]);

	const excedido = membresia != null && segundosTranscurridos > membresia.tiempoPermitidoMinutos * 60;

	async function devolver() {
		if (anclajeId === '') return;
		setError(null);
		setDevolviendo(true);
		try {
			const actualizado = await viajesApi.finalizar({ anclajeDestinoId: anclajeId });
			onDevuelta(actualizado);
		} catch (err) {
			setError(err instanceof ApiError ? err.message : 'no se pudo devolver la bici');
		} finally {
			setDevolviendo(false);
		}
	}

	return (
		<div className="tarjeta modo-bici">
			<h2>Viaje en curso</h2>
			<dl className="detalle">
				<dt>Viaje</dt>
				<dd>#{viaje.id}</dd>
				<dt>Bici</dt>
				<dd>{viaje.codigoBicicleta}</dd>
			</dl>

			{/*
			  El contador visual se actualiza cada segundo, pero se oculta de los lectores de pantalla:
			  anunciar un valor por segundo haria la pagina inusable. En su lugar hay una region viva
			  cuyo texto solo cambia cuando cambia el minuto, asi se anuncia una vez por minuto.
			*/}
			<div className={`contador ${excedido ? 'contador-excedido' : ''}`} aria-hidden="true">
				{formatearDuracion(segundosTranscurridos)}
			</div>
			<p className="solo-lectores" aria-live="polite">
				{Math.floor(segundosTranscurridos / 60)} minutos de viaje
			</p>

			{membresia && (
				<p className="ayuda">Tiempo incluido: {membresia.tiempoPermitidoMinutos} min</p>
			)}
			{excedido && (
				<p className="aviso aviso-alerta" role="alert">
					Ya superaste el tiempo permitido: se te va a cobrar el extra al devolver la bici.
				</p>
			)}

			<div className="formulario">
				<label>
					Estación de destino
					<select value={estacionId} onChange={(e) => setEstacionId(e.target.value ? Number(e.target.value) : '')}>
						<option value="">Elegí una estación</option>
						{estaciones.map((e) => (
							<option key={e.id} value={e.id}>
								{e.nombre}
							</option>
						))}
					</select>
				</label>
				<label>
					Anclaje libre
					<select value={anclajeId} onChange={(e) => setAnclajeId(e.target.value ? Number(e.target.value) : '')}>
						<option value="">Elegí un anclaje</option>
						{anclajes.map((a) => (
							<option key={a.id} value={a.id}>
								#{a.numero}
							</option>
						))}
					</select>
				</label>
				{estacionId !== '' && anclajes.length === 0 && <p className="ayuda">No hay anclajes libres en esa estación.</p>}
				{error && (
					<p className="error" role="alert">
						{error}
					</p>
				)}
				<button type="button" onClick={devolver} disabled={anclajeId === '' || devolviendo}>
					{devolviendo ? 'Devolviendo...' : 'Devolver bici'}
				</button>
			</div>
		</div>
	);
}
