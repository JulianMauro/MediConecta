import { useEffect, useId, useState, type FormEvent, type ReactNode } from 'react';
import { Modal } from '../ui/Modal';
import { soporteApi } from '../../api/soporte';
import { estacionesApi } from '../../api/estaciones';
import { almacenesApi } from '../../api/almacenes';
import { ApiError } from '../../api/client';
import type {
	AlmacenResponse,
	AnclajeResponse,
	EstacionResponse,
	ResultadoSoporteResponse,
	ResumenUsuarioResponse,
	ViajeSoporteResponse,
} from '../../types/dto';

type Ejecutar = (accion: () => Promise<ResultadoSoporteResponse>) => Promise<boolean>;

const ETIQUETA_ACCION: Record<string, string> = {
	CERRAR_VIAJE: 'Cerró el viaje',
	CORREGIR_VIAJE: 'Corrigió un viaje',
	ANULAR_STRIKE: 'Anuló un strike',
	CONFIRMAR_PAGO: 'Confirmó un pago',
	DESBLOQUEAR_CUENTA: 'Desbloqueó la cuenta',
	ANCLAJE_FUERA_SERVICIO: 'Anclaje fuera de servicio',
};

function mensajeDeError(e: unknown) {
	return e instanceof ApiError ? e.message : 'No se pudo conectar con el servidor';
}

function hora(iso: string) {
	return new Date(iso).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' });
}

function pesos(monto: number) {
	return `$${Number(monto).toLocaleString('es-AR')}`;
}

/** Fecha en el formato de <input type="datetime-local">, en la hora local del navegador. */
function aInputLocal(fecha: Date) {
	const local = new Date(fecha.getTime() - fecha.getTimezoneOffset() * 60_000);
	return local.toISOString().slice(0, 16);
}

/**
 * Accion con su motivo: cada una va plegada en un <details> para que el modal muestre
 * primero el estado del usuario y no un muro de formularios. El motivo es obligatorio
 * (el backend tambien lo exige) y le llega al usuario en el chat junto con lo que se hizo.
 */
function AccionForm({
	titulo,
	boton,
	onEnviar,
	children,
	listo = true,
}: {
	titulo: string;
	boton: string;
	onEnviar: (motivo: string) => Promise<boolean>;
	children?: ReactNode;
	/** false mientras falta completar un dato propio de la accion (ej. elegir un anclaje). */
	listo?: boolean;
}) {
	const [motivo, setMotivo] = useState('');
	const [enviando, setEnviando] = useState(false);
	const motivoId = useId();

	async function enviar(e: FormEvent) {
		e.preventDefault();
		setEnviando(true);
		const ok = await onEnviar(motivo.trim());
		setEnviando(false);
		if (ok) setMotivo('');
	}

	return (
		<details className="soporte-accion">
			<summary>{titulo}</summary>
			<form onSubmit={enviar}>
				{children}
				<div className="campo">
					<label htmlFor={motivoId}>Motivo (se lo mandamos al usuario por el chat)</label>
					<textarea
						id={motivoId}
						value={motivo}
						maxLength={300}
						rows={2}
						required
						onChange={(e) => setMotivo(e.target.value)}
					/>
				</div>
				<button type="submit" className="boton-chico" disabled={enviando || !listo || !motivo.trim()}>
					{boton}
				</button>
			</form>
		</details>
	);
}

/** Estacion y despues uno de sus anclajes LIBRE. Devuelve el id del anclaje elegido (o null). */
function SelectorAnclaje({ estaciones, onCambio }: { estaciones: EstacionResponse[]; onCambio: (id: number | null) => void }) {
	const [estacionId, setEstacionId] = useState('');
	const [anclajes, setAnclajes] = useState<AnclajeResponse[]>([]);
	const [anclajeId, setAnclajeId] = useState('');
	const estacionSel = useId();
	const anclajeSel = useId();

	useEffect(() => {
		setAnclajeId('');
		onCambio(null);
		if (!estacionId) {
			setAnclajes([]);
			return;
		}
		estacionesApi
			.listarAnclajes(Number(estacionId))
			.then((as) => setAnclajes(as.filter((a) => a.estado === 'LIBRE').sort((a, b) => a.numero - b.numero)))
			.catch(() => setAnclajes([]));
		// onCambio llega inline desde el padre: no se lo pone de dependencia para no recargar en cada render.
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [estacionId]);

	return (
		<div className="soporte-fila-campos">
			<div className="campo">
				<label htmlFor={estacionSel}>Estación</label>
				<select id={estacionSel} value={estacionId} onChange={(e) => setEstacionId(e.target.value)}>
					<option value="">Elegí una estación</option>
					{estaciones.map((e) => (
						<option key={e.id} value={e.id}>
							{e.nombre}
						</option>
					))}
				</select>
			</div>
			<div className="campo">
				<label htmlFor={anclajeSel}>Anclaje libre</label>
				<select
					id={anclajeSel}
					value={anclajeId}
					disabled={!estacionId}
					onChange={(e) => {
						setAnclajeId(e.target.value);
						onCambio(e.target.value ? Number(e.target.value) : null);
					}}
				>
					<option value="">{estacionId && anclajes.length === 0 ? 'No hay anclajes libres' : 'Elegí un anclaje'}</option>
					{anclajes.map((a) => (
						<option key={a.id} value={a.id}>
							Anclaje {a.numero}
						</option>
					))}
				</select>
			</div>
		</div>
	);
}

function CerrarViaje({
	ticketId,
	viaje,
	estaciones,
	almacenes,
	ejecutar,
}: {
	ticketId: number;
	viaje: ViajeSoporteResponse;
	estaciones: EstacionResponse[];
	almacenes: AlmacenResponse[];
	ejecutar: Ejecutar;
}) {
	const [destino, setDestino] = useState<'anclaje' | 'almacen'>('anclaje');
	const [anclajeId, setAnclajeId] = useState<number | null>(null);
	const [almacenId, setAlmacenId] = useState('');
	const [estadoBici, setEstadoBici] = useState<'EN_REPARACION' | 'DESACTIVADA'>('EN_REPARACION');
	const [usarAhora, setUsarAhora] = useState(true);
	const [fin, setFin] = useState(() => aInputLocal(new Date()));
	const [condonar, setCondonar] = useState(false);
	const ids = { almacen: useId(), estado: useId(), fin: useId() };

	const listo = destino === 'anclaje' ? anclajeId !== null : almacenId !== '';

	return (
		<AccionForm
			titulo="Cerrar el viaje"
			boton="Cerrar viaje"
			listo={listo && (usarAhora || fin !== '')}
			onEnviar={(motivo) =>
				ejecutar(() =>
					soporteApi.cerrarViaje(ticketId, {
						anclajeDestinoId: destino === 'anclaje' ? anclajeId : null,
						almacenId: destino === 'almacen' ? Number(almacenId) : null,
						estadoBicicleta: destino === 'almacen' ? estadoBici : null,
						fechaFin: usarAhora ? null : new Date(fin).toISOString(),
						condonarExceso: condonar,
						motivo,
					}),
				)
			}
		>
			<fieldset className="soporte-opciones">
				<legend>¿Dónde quedó la bici?</legend>
				<label>
					<input type="radio" checked={destino === 'anclaje'} onChange={() => setDestino('anclaje')} /> En un anclaje
				</label>
				<label>
					<input type="radio" checked={destino === 'almacen'} onChange={() => setDestino('almacen')} /> No volvió a
					ningún anclaje (rota o perdida)
				</label>
			</fieldset>

			{destino === 'anclaje' ? (
				<SelectorAnclaje estaciones={estaciones} onCambio={setAnclajeId} />
			) : (
				<div className="soporte-fila-campos">
					<div className="campo">
						<label htmlFor={ids.almacen}>Almacén que la retira</label>
						<select id={ids.almacen} value={almacenId} onChange={(e) => setAlmacenId(e.target.value)}>
							<option value="">Elegí un almacén</option>
							{almacenes.map((a) => (
								<option key={a.id} value={a.id}>
									{a.nombre}
								</option>
							))}
						</select>
					</div>
					<div className="campo">
						<label htmlFor={ids.estado}>Estado de la bici</label>
						<select
							id={ids.estado}
							value={estadoBici}
							onChange={(e) => setEstadoBici(e.target.value as 'EN_REPARACION' | 'DESACTIVADA')}
						>
							<option value="EN_REPARACION">En reparación (rota)</option>
							<option value="DESACTIVADA">Desactivada (robo o accidente)</option>
						</select>
					</div>
				</div>
			)}

			<fieldset className="soporte-opciones">
				<legend>Hora de fin</legend>
				<label>
					<input type="radio" checked={usarAhora} onChange={() => setUsarAhora(true)} /> Ahora
				</label>
				<label>
					<input type="radio" checked={!usarAhora} onChange={() => setUsarAhora(false)} /> Antes: la hora en que llegó
				</label>
				{!usarAhora && (
					<div className="campo">
						<label htmlFor={ids.fin}>Llegó a las</label>
						<input
							id={ids.fin}
							type="datetime-local"
							value={fin}
							min={aInputLocal(new Date(viaje.fechaInicio))}
							max={aInputLocal(new Date())}
							onChange={(e) => setFin(e.target.value)}
						/>
					</div>
				)}
			</fieldset>

			<label className="soporte-check">
				<input type="checkbox" checked={condonar} onChange={(e) => setCondonar(e.target.checked)} /> Si se pasó del
				tiempo, no cobrarle ni sumarle strike
			</label>
		</AccionForm>
	);
}

function CorregirViaje({ ticketId, viaje, ejecutar }: { ticketId: number; viaje: ViajeSoporteResponse; ejecutar: Ejecutar }) {
	const [fin, setFin] = useState(() => aInputLocal(new Date(viaje.fechaFin ?? viaje.fechaInicio)));
	const finId = useId();

	return (
		<AccionForm
			titulo="Corregir la hora de fin"
			boton="Corregir"
			listo={fin !== ''}
			onEnviar={(motivo) => ejecutar(() => soporteApi.corregirViaje(ticketId, viaje.id, new Date(fin).toISOString(), motivo))}
		>
			<p className="ayuda">
				Solo se puede acortar. Si deja de pasarse del tiempo, se anula el strike; si se pasa menos, baja el cobro pendiente.
			</p>
			<div className="campo">
				<label htmlFor={finId}>Terminó en realidad a las</label>
				<input
					id={finId}
					type="datetime-local"
					value={fin}
					min={aInputLocal(new Date(viaje.fechaInicio))}
					max={viaje.fechaFin ? aInputLocal(new Date(viaje.fechaFin)) : undefined}
					onChange={(e) => setFin(e.target.value)}
				/>
			</div>
		</AccionForm>
	);
}

function DatosViaje({ viaje, delTicket }: { viaje: ViajeSoporteResponse; delTicket: boolean }) {
	const pasado = viaje.duracionMinutos - viaje.minutosPermitidos;
	return (
		<p className="soporte-dato">
			<strong>
				Viaje #{viaje.id} · bici {viaje.codigoBicicleta}
			</strong>
			{delTicket && <span className="pill pill-categoria">El del ticket</span>}
			<br />
			Desde {viaje.estacionOrigen} (anclaje {viaje.anclajeOrigenNumero}) a las {hora(viaje.fechaInicio)}
			{viaje.fechaFin &&
				(viaje.estacionDestino
					? ` · hasta ${viaje.estacionDestino} (anclaje ${viaje.anclajeDestinoNumero})`
					: ' · terminó sin anclaje (la retiró logística)')}
			<br />
			<span className={pasado > 0 && !viaje.excesoCondonado ? 'texto-alerta' : ''}>
				{viaje.duracionMinutos} min de {viaje.minutosPermitidos} incluidos
				{pasado > 0 && (viaje.excesoCondonado ? ` · se pasó ${pasado} min, perdonado por soporte` : ` · se pasó ${pasado} min`)}
			</span>
		</p>
	);
}

/**
 * Lo que el admin puede resolverle al usuario de un ticket. Arriba el estado (para entender
 * el problema antes de tocar nada), abajo solo las acciones que aplican a ese estado.
 * Toda accion devuelve el resumen ya actualizado, asi que no hace falta recargar.
 */
export function GestionUsuarioModal({
	ticketId,
	onCerrar,
	onAccion,
}: {
	ticketId: number;
	onCerrar: () => void;
	/** Se llama despues de cada accion: el backend dejo un mensaje en el chat. */
	onAccion: () => void;
}) {
	const [resumen, setResumen] = useState<ResumenUsuarioResponse | null>(null);
	const [estaciones, setEstaciones] = useState<EstacionResponse[]>([]);
	const [almacenes, setAlmacenes] = useState<AlmacenResponse[]>([]);
	const [error, setError] = useState<string | null>(null);
	const [hecho, setHecho] = useState<string | null>(null);
	const referenciaId = useId();
	const [referencias, setReferencias] = useState<Record<number, string>>({});

	useEffect(() => {
		soporteApi.resumen(ticketId).then(setResumen).catch((e) => setError(mensajeDeError(e)));
		estacionesApi.listar().then((es) => setEstaciones(es.filter((e) => e.activa))).catch(() => setEstaciones([]));
		almacenesApi.listar().then(setAlmacenes).catch(() => setAlmacenes([]));
	}, [ticketId]);

	const ejecutar: Ejecutar = async (accion) => {
		setError(null);
		setHecho(null);
		try {
			const resultado = await accion();
			setResumen(resultado.resumen);
			setHecho(resultado.accion.detalle);
			onAccion();
			return true;
		} catch (e) {
			setError(mensajeDeError(e));
			return false;
		}
	};

	const u = resumen?.usuario;

	return (
		<Modal
			ancho
			title={u ? `${u.nombre} ${u.apellido}` : 'Gestionar usuario'}
			description="Lo que hagas acá queda registrado y le llega al usuario por el chat del ticket."
			onClose={onCerrar}
		>
			{error && <p className="aviso aviso-alerta">{error}</p>}
			{hecho && <p className="aviso aviso-ok">Listo: {hecho}</p>}
			{!resumen ? (
				!error && <p className="ayuda">Cargando…</p>
			) : (
				<div className="soporte">
					<section className="soporte-seccion">
						<p className="soporte-dato">
							{u?.email}
							{u?.telefono && ` · ${u.telefono}`}
							{u?.dni && ` · DNI ${u.dni}`}
						</p>
						<div className="soporte-chips">
							<span className={resumen.bloqueo ? 'pill pill-desactivada' : 'pill pill-disponible'}>
								{resumen.bloqueo ? 'Cuenta bloqueada' : 'Cuenta activa'}
							</span>
							<span className={resumen.plan ? 'pill pill-disponible' : 'pill pill-desactivada'}>
								{resumen.plan ? `${resumen.plan.nombre} · vence ${hora(resumen.plan.vence)}` : 'Sin plan vigente'}
							</span>
							<span className={resumen.strikesActivos > 0 ? 'pill pill-en_reparacion' : 'pill pill-disponible'}>
								{resumen.strikesActivos}/{resumen.strikesParaBloquear} strikes activos
							</span>
						</div>
					</section>

					<section className="soporte-seccion">
						<h3>Viaje en curso</h3>
						{resumen.viajeEnCurso ? (
							<>
								<DatosViaje
									viaje={resumen.viajeEnCurso}
									delTicket={resumen.viajeEnCurso.id === resumen.viajeDelTicketId}
								/>
								<CerrarViaje
									key={resumen.viajeEnCurso.id}
									ticketId={ticketId}
									viaje={resumen.viajeEnCurso}
									estaciones={estaciones}
									almacenes={almacenes}
									ejecutar={ejecutar}
								/>
							</>
						) : (
							<p className="ayuda">No tiene la bici: no hay un viaje en curso.</p>
						)}
					</section>

					{resumen.bloqueo && (
						<section className="soporte-seccion">
							<h3>Bloqueo</h3>
							<p className="soporte-dato">
								Bloqueada desde {hora(resumen.bloqueo.fechaBloqueo)}: {resumen.bloqueo.motivo}.
								{resumen.strikesActivos >= resumen.strikesParaBloquear &&
									' Sigue con strikes suficientes para bloquearse: si la desbloqueás, el próximo exceso la vuelve a bloquear.'}
							</p>
							<AccionForm
								titulo="Desbloquear la cuenta"
								boton="Desbloquear"
								onEnviar={(motivo) => ejecutar(() => soporteApi.desbloquear(ticketId, motivo))}
							/>
						</section>
					)}

					<section className="soporte-seccion">
						<h3>Strikes</h3>
						{resumen.strikes.length === 0 ? (
							<p className="ayuda">No tiene strikes.</p>
						) : (
							<ul className="soporte-lista">
								{resumen.strikes.map((s) => (
									<li key={s.id}>
										<p className="soporte-dato">
											<strong>Viaje #{s.viajeId}</strong> · {hora(s.fechaGeneracion)} ·{' '}
											<span className={s.estado === 'ACTIVO' ? 'texto-alerta' : ''}>{s.estado}</span>
											{s.monto !== null && ` · cobro de ${pesos(s.monto)} (${s.estadoPago})`}
										</p>
										{s.estado === 'ACTIVO' && (
											<AccionForm
												titulo="Anular este strike"
												boton="Anular strike y su cobro"
												onEnviar={(motivo) => ejecutar(() => soporteApi.anularStrike(ticketId, s.id, motivo))}
											>
												<p className="ayuda">
													El strike deja de contar y el cobro de tiempo extra se cancela.
													{resumen.bloqueo &&
														resumen.strikesActivos - 1 < resumen.strikesParaBloquear &&
														' Con esto baja de ' + resumen.strikesParaBloquear + ' y la cuenta se desbloquea.'}
												</p>
											</AccionForm>
										)}
									</li>
								))}
							</ul>
						)}
					</section>

					<section className="soporte-seccion">
						<h3>Pagos pendientes</h3>
						{resumen.pagosPendientes.length === 0 ? (
							<p className="ayuda">No tiene pagos pendientes.</p>
						) : (
							<ul className="soporte-lista">
								{resumen.pagosPendientes.map((p) => (
									<li key={p.id}>
										<p className="soporte-dato">
											<strong>Pago #{p.id}</strong> · {p.concepto === 'MEMBRESIA' ? 'plan' : 'tiempo extra'} ·{' '}
											{pesos(p.monto)} · {hora(p.fechaCreacion)}
										</p>
										<AccionForm
											titulo="Confirmar el pago a mano"
											boton="Confirmar pago"
											listo={Boolean(referencias[p.id]?.trim())}
											onEnviar={(motivo) =>
												ejecutar(() =>
													soporteApi.confirmarPago(ticketId, p.id, referencias[p.id].trim(), motivo),
												)
											}
										>
											<p className="ayuda">
												{p.concepto === 'MEMBRESIA'
													? 'Le activa el plan.'
													: 'Salda el strike. No desbloquea la cuenta: eso es aparte.'}
											</p>
											<div className="campo">
												<label htmlFor={`${referenciaId}-${p.id}`}>Referencia del comprobante</label>
												<input
													id={`${referenciaId}-${p.id}`}
													value={referencias[p.id] ?? ''}
													maxLength={100}
													onChange={(e) => setReferencias((r) => ({ ...r, [p.id]: e.target.value }))}
												/>
											</div>
										</AccionForm>
									</li>
								))}
							</ul>
						)}
					</section>

					<section className="soporte-seccion">
						<h3>Últimos viajes</h3>
						{resumen.ultimosViajes.length === 0 ? (
							<p className="ayuda">Todavía no devolvió ninguna bici.</p>
						) : (
							<ul className="soporte-lista">
								{resumen.ultimosViajes.map((v) => (
									<li key={v.id}>
										<DatosViaje viaje={v} delTicket={v.id === resumen.viajeDelTicketId} />
										<CorregirViaje key={v.fechaFin} ticketId={ticketId} viaje={v} ejecutar={ejecutar} />
									</li>
								))}
							</ul>
						)}
					</section>

					<section className="soporte-seccion">
						<h3>Anclaje roto</h3>
						<AnclajeFueraDeServicio ticketId={ticketId} estaciones={estaciones} ejecutar={ejecutar} />
					</section>

					{resumen.acciones.length > 0 && (
						<section className="soporte-seccion">
							<h3>Lo que ya se hizo en este ticket</h3>
							<ul className="soporte-lista">
								{resumen.acciones.map((a) => (
									<li key={a.id} className="soporte-dato">
										<strong>{ETIQUETA_ACCION[a.tipo] ?? a.tipo}</strong> · {hora(a.fecha)}
										<br />
										{a.detalle}
										<br />
										<span className="ayuda">Motivo: {a.motivo}</span>
									</li>
								))}
							</ul>
						</section>
					)}
				</div>
			)}
		</Modal>
	);
}

function AnclajeFueraDeServicio({
	ticketId,
	estaciones,
	ejecutar,
}: {
	ticketId: number;
	estaciones: EstacionResponse[];
	ejecutar: Ejecutar;
}) {
	const [anclajeId, setAnclajeId] = useState<number | null>(null);
	// Se remonta tras marcar uno, para que el anclaje recien roto deje de aparecer como libre.
	const [version, setVersion] = useState(0);

	return (
		<AccionForm
			titulo="Marcar un anclaje fuera de servicio"
			boton="Marcar fuera de servicio"
			listo={anclajeId !== null}
			onEnviar={async (motivo) => {
				const ok = await ejecutar(() => soporteApi.anclajeFueraDeServicio(ticketId, anclajeId as number, motivo));
				if (ok) setVersion((v) => v + 1);
				return ok;
			}}
		>
			<p className="ayuda">Solo los anclajes libres: uno con una bici adentro primero hay que vaciarlo.</p>
			<SelectorAnclaje key={version} estaciones={estaciones} onCambio={setAnclajeId} />
		</AccionForm>
	);
}
