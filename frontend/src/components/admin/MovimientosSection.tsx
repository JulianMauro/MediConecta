import { useEffect, useState } from 'react';
import { estacionesApi } from '../../api/estaciones';
import { almacenesApi } from '../../api/almacenes';
import { bicicletasApi } from '../../api/bicicletas';
import { movimientosApi } from '../../api/movimientos';
import { ApiError } from '../../api/client';
import { Modal } from '../ui/Modal';
import { Field } from '../ui/Field';
import type { AlmacenResponse, BicicletaResponse, EstacionResponse } from '../../types/dto';

type TipoUbicacion = 'ESTACION' | 'ALMACEN';

interface Props {
	onMovido?: () => void;
	/** Se incrementa desde afuera cuando se crea una estacion/almacen nueva, para refetchear los selects. */
	refrescarSenal?: number;
}

/** Arma el request de MovimientoBicis: origen/destino son polimorficos (estacion o almacen), igual que en el backend. */
export function MovimientosSection({ onMovido, refrescarSenal }: Props) {
	const [modalAbierto, setModalAbierto] = useState(false);
	const [estaciones, setEstaciones] = useState<EstacionResponse[]>([]);
	const [almacenes, setAlmacenes] = useState<AlmacenResponse[]>([]);
	const [bicicletas, setBicicletas] = useState<BicicletaResponse[]>([]);

	const [origenTipo, setOrigenTipo] = useState<TipoUbicacion>('ALMACEN');
	const [origenId, setOrigenId] = useState<number | ''>('');
	const [destinoTipo, setDestinoTipo] = useState<TipoUbicacion>('ESTACION');
	const [destinoId, setDestinoId] = useState<number | ''>('');
	const [bicicletasEnOrigen, setBicicletasEnOrigen] = useState<BicicletaResponse[]>([]);
	const [seleccionadas, setSeleccionadas] = useState<number[]>([]);
	const [error, setError] = useState<string | null>(null);
	const [guardando, setGuardando] = useState(false);

	function cargarListas() {
		estacionesApi.listar().then(setEstaciones);
		almacenesApi.listar().then(setAlmacenes);
		bicicletasApi.listar().then(setBicicletas);
	}

	useEffect(cargarListas, [refrescarSenal]);

	function abrirModal() {
		setOrigenTipo('ALMACEN');
		setOrigenId('');
		setDestinoTipo('ESTACION');
		setDestinoId('');
		setSeleccionadas([]);
		setError(null);
		cargarListas();
		setModalAbierto(true);
	}

	// Recalcula que bicis estan efectivamente en el origen elegido, para no mandar ids invalidos.
	useEffect(() => {
		async function calcular() {
			if (origenId === '') {
				setBicicletasEnOrigen([]);
				return;
			}
			if (origenTipo === 'ALMACEN') {
				setBicicletasEnOrigen(bicicletas.filter((b) => b.almacenId === origenId));
				return;
			}
			const anclajesDeLaEstacion = await estacionesApi.listarAnclajes(origenId);
			const idsAnclajes = new Set(anclajesDeLaEstacion.map((a) => a.id));
			setBicicletasEnOrigen(bicicletas.filter((b) => b.anclajeId != null && idsAnclajes.has(b.anclajeId)));
		}
		calcular();
		setSeleccionadas([]);
	}, [origenTipo, origenId, bicicletas]);

	function toggleSeleccion(id: number) {
		setSeleccionadas((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
	}

	async function mover() {
		if (origenId === '' || destinoId === '' || seleccionadas.length === 0) return;
		setError(null);
		setGuardando(true);
		try {
			await movimientosApi.registrar({
				origenEstacionId: origenTipo === 'ESTACION' ? origenId : null,
				origenAlmacenId: origenTipo === 'ALMACEN' ? origenId : null,
				destinoEstacionId: destinoTipo === 'ESTACION' ? destinoId : null,
				destinoAlmacenId: destinoTipo === 'ALMACEN' ? destinoId : null,
				bicicletaIds: seleccionadas,
			});
			setModalAbierto(false);
			onMovido?.();
		} catch (err) {
			setError(err instanceof ApiError ? err.message : 'no se pudo registrar el movimiento');
		} finally {
			setGuardando(false);
		}
	}

	function selectorDe(tipo: TipoUbicacion, valor: number | '', onChange: (v: number | '') => void) {
		const opciones = tipo === 'ESTACION' ? estaciones : almacenes;
		return (
			<select value={valor} onChange={(e) => onChange(e.target.value ? Number(e.target.value) : '')}>
				<option value="">Elegí {tipo === 'ESTACION' ? 'una estación' : 'un almacén'}</option>
				{opciones.map((o) => (
					<option key={o.id} value={o.id}>
						{o.nombre}
					</option>
				))}
			</select>
		);
	}

	return (
		<section className="tarjeta">
			<div className="tarjeta-encabezado">
				<div>
					<h2>Mover bicis</h2>
					<p className="ayuda">Traslado en lote entre almacenes y estaciones (simula el camión de redistribución).</p>
				</div>
				<button onClick={abrirModal}>+ Mover bicis</button>
			</div>

			{modalAbierto && (
				<Modal
					title="Mover bicis en lote"
					description="Elegí de dónde salen y a dónde llegan; quedará registrado quién hizo el traslado y qué bicis se movieron."
					onClose={() => setModalAbierto(false)}
					footer={
						<>
							<button className="boton-secundario" onClick={() => setModalAbierto(false)}>
								Cancelar
							</button>
							<button onClick={mover} disabled={origenId === '' || destinoId === '' || seleccionadas.length === 0 || guardando}>
								{guardando ? 'Moviendo...' : `Mover ${seleccionadas.length > 0 ? `(${seleccionadas.length})` : ''}`}
							</button>
						</>
					}
				>
					<div className="formulario-2col">
						<Field label="Tipo de origen" required helpText="De dónde salen las bicis.">
							<select value={origenTipo} onChange={(e) => setOrigenTipo(e.target.value as TipoUbicacion)}>
								<option value="ALMACEN">Almacén</option>
								<option value="ESTACION">Estación</option>
							</select>
						</Field>
						<Field label={origenTipo === 'ESTACION' ? 'Estación de origen' : 'Almacén de origen'} required>
							{selectorDe(origenTipo, origenId, setOrigenId)}
						</Field>
					</div>
					<div className="formulario-2col">
						<Field label="Tipo de destino" required helpText="A dónde llegan las bicis.">
							<select value={destinoTipo} onChange={(e) => setDestinoTipo(e.target.value as TipoUbicacion)}>
								<option value="ESTACION">Estación</option>
								<option value="ALMACEN">Almacén</option>
							</select>
						</Field>
						<Field label={destinoTipo === 'ESTACION' ? 'Estación de destino' : 'Almacén de destino'} required>
							{selectorDe(destinoTipo, destinoId, setDestinoId)}
						</Field>
					</div>

					{origenId !== '' && (
						<Field label="Bicis a mover" required helpText="Solo se listan las bicis que están efectivamente en el origen elegido.">
							<ul className="chips">
								{bicicletasEnOrigen.map((b) => (
									<li key={b.id} className={`chip ${seleccionadas.includes(b.id) ? 'chip-seleccionado' : ''}`}>
										<label>
											<input
												type="checkbox"
												checked={seleccionadas.includes(b.id)}
												onChange={() => toggleSeleccion(b.id)}
											/>
											{b.codigo}
										</label>
									</li>
								))}
								{bicicletasEnOrigen.length === 0 && <li className="ayuda">No hay bicis ahí.</li>}
							</ul>
						</Field>
					)}

					{error && <p className="error">{error}</p>}
				</Modal>
			)}
		</section>
	);
}
