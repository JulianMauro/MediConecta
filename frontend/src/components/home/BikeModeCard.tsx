import { useEffect, useMemo, useState, type CSSProperties } from 'react';
import { viajesApi } from '../../api/viajes';
import { membresiasApi } from '../../api/membresias';
import { ApiError } from '../../api/client';
import { useEstaciones } from '../../hooks/useEstaciones';
import { plural } from '../../lib/plural';
import type { MembresiaResponse, ViajeResponse } from '../../types/dto';

interface Props {
	viaje: ViajeResponse;
	onDevuelta: (viaje: ViajeResponse) => void;
}

function mmss(segundos: number): string {
	const m = Math.floor(Math.abs(segundos) / 60);
	const s = Math.abs(segundos) % 60;
	return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
}

/**
 * Viaje en curso.
 *
 * No hay hardware real que "reciba" la bici: devolver es elegir una estacion + un
 * anclaje libre y confirmar. Es la unica forma de cerrar el viaje sin un dock fisico.
 *
 * La pantalla esta ordenada por lo que la persona necesita mientras pedalea, que no
 * es lo mismo que lo que el sistema sabe: cuanto tiempo le QUEDA (antes se mostraba
 * el transcurrido, que obliga a hacer la resta de cabeza), con que bici esta, y
 * recien despues donde dejarla.
 */
export function BikeModeCard({ viaje, onDevuelta }: Props) {
	const [segundos, setSegundos] = useState(0);
	const [membresia, setMembresia] = useState<MembresiaResponse | null>(null);
	const { estaciones } = useEstaciones();

	const [estacionId, setEstacionId] = useState<number | ''>('');
	const [anclajeId, setAnclajeId] = useState<number | ''>('');
	const [error, setError] = useState<string | null>(null);
	const [devolviendo, setDevolviendo] = useState(false);

	useEffect(() => {
		const inicio = new Date(viaje.fechaInicio).getTime();
		const actualizar = () => setSegundos(Math.max(0, Math.floor((Date.now() - inicio) / 1000)));
		actualizar();
		const intervalo = setInterval(actualizar, 1000);
		return () => clearInterval(intervalo);
	}, [viaje.fechaInicio]);

	useEffect(() => {
		membresiasApi
			.obtener(viaje.membresiaId)
			.then(setMembresia)
			.catch(() => setMembresia(null));
	}, [viaje.membresiaId]);

	const incluidos = membresia ? membresia.tiempoPermitidoMinutos * 60 : null;
	const restante = incluidos != null ? incluidos - segundos : null;
	const excedido = restante != null && restante < 0;
	// Se corta en 1: pasado el limite el anillo queda lleno y el aviso lo toma de ahi.
	const progreso = incluidos ? Math.min(1, segundos / incluidos) : 0;

	/*
	 * Los anclajes salen del mismo hook que ya trae las estaciones con su
	 * disponibilidad, en vez de pedirlos de nuevo al elegir: asi el desplegable de
	 * estaciones puede decir cuantos lugares libres tiene cada una ANTES de elegir.
	 * Antes se elegia a ciegas y recien despues aparecia "no hay anclajes libres".
	 */
	const estacionElegida = estaciones.find((e) => e.id === estacionId) ?? null;
	const libres = useMemo(
		() => (estacionElegida ? estacionElegida.anclajes.filter((a) => a.estado === 'LIBRE') : []),
		[estacionElegida],
	);

	async function devolver() {
		if (anclajeId === '') return;
		setError(null);
		setDevolviendo(true);
		try {
			onDevuelta(await viajesApi.finalizar({ anclajeDestinoId: anclajeId }));
		} catch (err) {
			setError(err instanceof ApiError ? err.message : 'no se pudo devolver la bici');
		} finally {
			setDevolviendo(false);
		}
	}

	return (
		<div className="tarjeta modo-bici desbloqueo-entrando">
			<div className="viaje-encabezado">
				<h2>Viaje en curso</h2>
				{/* El codigo de la bici sirve para confirmar que agarraste la que desbloqueaste.
				    El id del viaje se fue: es un dato del sistema, no de la persona. */}
				<span className="viaje-bici">{viaje.codigoBicicleta}</span>
			</div>

			{/*
			  El anillo se consume barriendo el espectro: el arco pintado es el tiempo
			  gastado. Es decorativo — el dato real esta en numeros adentro y en el
			  texto de abajo, nunca solo en el color (WCAG 1.4.1).
			*/}
			<div
				className={`viaje-anillo ${excedido ? 'viaje-anillo-excedido' : ''}`}
				style={{ '--progreso': progreso } as CSSProperties}
				aria-hidden="true"
			>
				<div className="viaje-anillo-centro">
					<span className="viaje-tiempo">{restante != null ? mmss(restante) : mmss(segundos)}</span>
					<span className="viaje-rotulo">
						{restante == null ? 'en viaje' : excedido ? 'de más' : 'te quedan'}
					</span>
				</div>
			</div>

			{/*
			  El contador se redibuja cada segundo pero se oculta del lector de pantalla:
			  anunciar un valor por segundo vuelve la pagina inusable. Esta region viva
			  solo cambia su texto cuando cambia el minuto, asi se anuncia una vez por minuto.
			*/}
			<p className="solo-lectores" aria-live="polite">
				{excedido
					? `Te pasaste por ${plural(Math.floor(-(restante ?? 0) / 60), 'minuto', 'minutos')}`
					: restante != null
						? `Te ${plural(Math.floor(restante / 60), 'queda 1 minuto', 'quedan')} de viaje`
						: `${Math.floor(segundos / 60)} minutos de viaje`}
			</p>

			{membresia && !excedido && (
				<p className="ayuda viaje-pie">
					Tu plan incluye {membresia.tiempoPermitidoMinutos} min · llevás {mmss(segundos)}
				</p>
			)}

			{excedido && membresia && (
				<p className="aviso aviso-alerta" role="alert">
					Pasaste los {membresia.tiempoPermitidoMinutos} min de tu plan. Al devolverla se te cobra $
					{membresia.tarifaMinutoExtra} por cada minuto extra.
				</p>
			)}

			<div className="viaje-devolucion">
				<h3>Devolver la bici</h3>
				<div className="formulario-2col">
					<label>
						Estación
						<select
							value={estacionId}
							onChange={(e) => {
								setEstacionId(e.target.value ? Number(e.target.value) : '');
								// El anclaje elegido pertenece a la estacion anterior: se limpia.
								setAnclajeId('');
							}}
						>
							<option value="">Elegí una estación</option>
							{estaciones.map((e) => (
								<option key={e.id} value={e.id} disabled={e.anclajesLibres === 0}>
									{e.nombre} — {e.anclajesLibres === 0 ? 'sin lugar' : plural(e.anclajesLibres, 'lugar libre', 'lugares libres')}
								</option>
							))}
						</select>
					</label>
					<label>
						Anclaje
						<select
							value={anclajeId}
							onChange={(e) => setAnclajeId(e.target.value ? Number(e.target.value) : '')}
							disabled={estacionId === ''}
						>
							<option value="">{estacionId === '' ? 'Elegí una estación primero' : 'Elegí un anclaje'}</option>
							{libres.map((a) => (
								<option key={a.id} value={a.id}>
									#{a.numero}
								</option>
							))}
						</select>
					</label>
				</div>

				{estacionId !== '' && libres.length === 0 && (
					<p className="aviso aviso-alerta">
						No quedan anclajes libres en esa estación. Elegí otra: la bici solo se devuelve en un lugar libre.
					</p>
				)}
				{error && (
					<p className="error" role="alert">
						{error}
					</p>
				)}
				<button type="button" onClick={devolver} disabled={anclajeId === '' || devolviendo}>
					{devolviendo ? 'Devolviendo…' : 'Devolver bici'}
				</button>
			</div>
		</div>
	);
}
