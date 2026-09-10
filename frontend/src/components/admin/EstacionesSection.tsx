import { useEffect, useState } from 'react';
import { estacionesApi } from '../../api/estaciones';
import { ApiError } from '../../api/client';
import { Modal } from '../ui/Modal';
import { Field } from '../ui/Field';
import type { AnclajeResponse, EstacionResponse } from '../../types/dto';

const FORM_VACIO = { nombre: '', direccion: '', capacidad: 10 };

interface Props {
	onCambio?: () => void;
}

export function EstacionesSection({ onCambio }: Props) {
	const [estaciones, setEstaciones] = useState<EstacionResponse[]>([]);
	// Un solo modal para alta y edicion: el formulario es identico, lo unico que
	// cambia es el titulo, el texto del boton y a que endpoint pega al guardar.
	const [modoModal, setModoModal] = useState<'crear' | 'editar' | null>(null);
	const [editandoId, setEditandoId] = useState<number | null>(null);
	const [form, setForm] = useState(FORM_VACIO);
	const [error, setError] = useState<string | null>(null);
	const [guardando, setGuardando] = useState(false);

	const [expandidaId, setExpandidaId] = useState<number | null>(null);
	const [anclajes, setAnclajes] = useState<AnclajeResponse[]>([]);

	const [modalAnclajeAbierto, setModalAnclajeAbierto] = useState(false);
	const [numeroAnclaje, setNumeroAnclaje] = useState(1);
	const [errorAnclaje, setErrorAnclaje] = useState<string | null>(null);

	function recargar() {
		estacionesApi.listar().then(setEstaciones);
	}

	useEffect(recargar, []);

	function abrirCrear() {
		setForm(FORM_VACIO);
		setEditandoId(null);
		setError(null);
		setModoModal('crear');
	}

	function abrirEditar(est: EstacionResponse) {
		setForm({ nombre: est.nombre, direccion: est.direccion, capacidad: est.capacidad });
		setEditandoId(est.id);
		setError(null);
		setModoModal('editar');
	}

	async function guardar() {
		setError(null);
		setGuardando(true);
		try {
			if (editandoId != null) {
				// El update no manda coordenadas: si cambio la direccion, el backend
				// las recalcula solo (y las borra si no las puede resolver).
				await estacionesApi.actualizar(editandoId, form);
			} else {
				await estacionesApi.crear({ ...form, latitud: null, longitud: null });
			}
			setModoModal(null);
			recargar();
			onCambio?.();
		} catch (err) {
			const accion = editandoId != null ? 'guardar los cambios' : 'crear la estación';
			setError(err instanceof ApiError ? err.message : `no se pudo ${accion}`);
		} finally {
			setGuardando(false);
		}
	}

	/** Reintenta resolver las coordenadas de una estacion ya guardada. */
	async function recalcularCoordenadas(id: number) {
		setError(null);
		setGuardando(true);
		try {
			await estacionesApi.geocodificar(id);
			recargar();
		} catch (err) {
			setError(err instanceof ApiError ? err.message : 'no se pudieron obtener las coordenadas');
		} finally {
			setGuardando(false);
		}
	}

	async function expandir(id: number) {
		if (expandidaId === id) {
			setExpandidaId(null);
			return;
		}
		setExpandidaId(id);
		setAnclajes(await estacionesApi.listarAnclajes(id));
	}

	function abrirModalAnclaje() {
		setNumeroAnclaje(anclajes.length + 1);
		setErrorAnclaje(null);
		setModalAnclajeAbierto(true);
	}

	async function agregarAnclaje() {
		if (expandidaId == null) return;
		setErrorAnclaje(null);
		try {
			await estacionesApi.agregarAnclaje(expandidaId, { numero: numeroAnclaje });
			setAnclajes(await estacionesApi.listarAnclajes(expandidaId));
			setModalAnclajeAbierto(false);
			onCambio?.();
		} catch (err) {
			setErrorAnclaje(err instanceof ApiError ? err.message : 'no se pudo crear el anclaje');
		}
	}

	async function toggleFueraServicio(anclaje: AnclajeResponse) {
		if (expandidaId == null) return;
		if (anclaje.estado === 'FUERA_SERVICIO') {
			await estacionesApi.habilitarAnclaje(anclaje.id);
		} else {
			await estacionesApi.marcarFueraServicio(anclaje.id);
		}
		setAnclajes(await estacionesApi.listarAnclajes(expandidaId));
	}

	const estacionExpandida = estaciones.find((e) => e.id === expandidaId);
	const capacidadCompleta = estacionExpandida != null && anclajes.length >= estacionExpandida.capacidad;

	return (
		<section className="tarjeta">
			<div className="tarjeta-encabezado">
				<div>
					<h2>Estaciones</h2>
					<p className="ayuda">Dónde los clientes retiran y devuelven bicis.</p>
				</div>
				<button onClick={abrirCrear}>+ Nueva estación</button>
			</div>

			<ul className="lista-expandible">
				{estaciones.map((est) => (
					<li key={est.id}>
						<button className="fila-clickeable" onClick={() => expandir(est.id)}>
							<span>
								<strong>{est.nombre}</strong> — {est.direccion}
							</span>
							<span className="pill">{est.capacidad} anclajes máx.</span>
						</button>
						{expandidaId === est.id && (
							<div className="subpanel">
								<div className="subpanel-encabezado">
									<p className="ayuda">
										{anclajes.length} de {est.capacidad} anclajes creados
										{capacidadCompleta && ' — llegaste al máximo para esta estación.'}
									</p>
									<button className="boton-secundario boton-chico" onClick={() => abrirEditar(est)}>
										Editar estación
									</button>
									<button className="boton-chico" onClick={abrirModalAnclaje} disabled={capacidadCompleta}>
										+ Agregar anclaje
									</button>
								</div>

								{/* Sin coordenadas la estacion no se puede pintar en el mapa: se avisa
								    y se ofrece el reintento, en vez de que el mapa aparezca vacio sin
								    explicacion. */}
								<p className="ayuda">
									{est.latitud != null && est.longitud != null ? (
										<>
											Coordenadas: {est.latitud.toFixed(5)}, {est.longitud.toFixed(5)}
										</>
									) : (
										<>Sin coordenadas — no aparece en el mapa. </>
									)}{' '}
									<button
										className="boton-texto boton-chico"
										onClick={() => recalcularCoordenadas(est.id)}
										disabled={guardando}
									>
										Recalcular desde la dirección
									</button>
								</p>
								<ul className="chips">
									{anclajes.map((a) => (
										<li key={a.id} className={`chip chip-${a.estado.toLowerCase()}`}>
											#{a.numero} · {a.estado}
											<button className="boton-chico" onClick={() => toggleFueraServicio(a)}>
												{a.estado === 'FUERA_SERVICIO' ? 'Habilitar' : 'Fuera de servicio'}
											</button>
										</li>
									))}
									{anclajes.length === 0 && <li className="ayuda">Todavía no tiene anclajes.</li>}
								</ul>
							</div>
						)}
					</li>
				))}
				{estaciones.length === 0 && <li className="ayuda">Todavía no creaste ninguna estación.</li>}
			</ul>

			{modoModal && (
				<Modal
					title={modoModal === 'editar' ? 'Editar estación' : 'Nueva estación'}
					description={
						modoModal === 'editar'
							? 'Si cambiás la dirección, las coordenadas del mapa se recalculan solas.'
							: 'Un punto físico donde se instalan anclajes para dejar y retirar bicis.'
					}
					onClose={() => setModoModal(null)}
					footer={
						<>
							<button className="boton-secundario" onClick={() => setModoModal(null)}>
								Cancelar
							</button>
							<button onClick={guardar} disabled={!form.nombre || !form.direccion || guardando}>
								{guardando
									? 'Guardando...'
									: modoModal === 'editar'
										? 'Guardar cambios'
										: 'Crear estación'}
							</button>
						</>
					}
				>
					<Field label="Nombre" required helpText="Cómo se va a mostrar esta estación en toda la app.">
						<input value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} autoFocus />
					</Field>
					<Field label="Dirección" required helpText="Ubicación física de la estación.">
						<input value={form.direccion} onChange={(e) => setForm({ ...form, direccion: e.target.value })} />
					</Field>
					<Field
						label="Capacidad"
						required
						helpText="Cantidad máxima de anclajes que va a poder tener esta estación. Una vez llegado a este número, no vas a poder agregar más — elegilo pensando en el espacio real disponible."
					>
						<input
							type="number"
							min={1}
							value={form.capacidad}
							onChange={(e) => setForm({ ...form, capacidad: Number(e.target.value) })}
						/>
					</Field>
					{error && <p className="error">{error}</p>}
				</Modal>
			)}

			{modalAnclajeAbierto && estacionExpandida && (
				<Modal
					title={`Agregar anclaje a "${estacionExpandida.nombre}"`}
					description="Un anclaje es un punto individual (dock) donde se traba una bici puntual."
					onClose={() => setModalAnclajeAbierto(false)}
					footer={
						<>
							<button className="boton-secundario" onClick={() => setModalAnclajeAbierto(false)}>
								Cancelar
							</button>
							<button onClick={agregarAnclaje}>Agregar</button>
						</>
					}
				>
					<Field
						label="Número de anclaje"
						required
						helpText="Identifica al anclaje dentro de esta estación (no se puede repetir en la misma estación)."
					>
						<input
							type="number"
							min={1}
							value={numeroAnclaje}
							onChange={(e) => setNumeroAnclaje(Number(e.target.value))}
							autoFocus
						/>
					</Field>
					{errorAnclaje && <p className="error">{errorAnclaje}</p>}
				</Modal>
			)}
		</section>
	);
}
