import { useCallback, useEffect, useId, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { useAuth } from '../context/AuthContext';
import { ticketsApi } from '../api/tickets';
import { chatApi } from '../api/chat';
import { viajesApi } from '../api/viajes';
import { ApiError } from '../api/client';
import { GestionUsuarioModal } from '../components/soporte/GestionUsuarioModal';
import type { CategoriaTicket, ChatMensajeResponse, EstadoTicket, TicketResponse, ViajeResponse } from '../types/dto';

// Lo nuevo del chat se trae de la queue chat.p2p cada pocos segundos; la lista de tickets, mas espaciado.
const POLL_CHAT_MS = 3000;
const POLL_TICKETS_MS = 5000;

const ETIQUETA_ESTADO: Record<EstadoTicket, string> = {
	ABIERTO: 'Esperando admin',
	EN_CHAT: 'En atención',
	RESUELTO: 'Resuelto',
};

const ETIQUETA_CATEGORIA: Record<CategoriaTicket, string> = {
	ANCLAJE: 'Problema al anclar o devolver',
	BICICLETA: 'Bici con falla',
	PAGO: 'Cobro o pago',
	CUENTA: 'Cuenta o plan',
	OTRO: 'Otro',
};

const CATEGORIAS = Object.keys(ETIQUETA_CATEGORIA) as CategoriaTicket[];

function FiltroCategoria({ valor, onCambio }: { valor: CategoriaTicket | ''; onCambio: (c: CategoriaTicket | '') => void }) {
	const id = useId();
	return (
		<div className="campo campo-inline">
			<label htmlFor={id}>Categoría</label>
			<select id={id} value={valor} onChange={(e) => onCambio(e.target.value as CategoriaTicket | '')}>
				<option value="">Todas</option>
				{CATEGORIAS.map((c) => (
					<option key={c} value={c}>
						{ETIQUETA_CATEGORIA[c]}
					</option>
				))}
			</select>
		</div>
	);
}

function mensajeDeError(e: unknown) {
	return e instanceof ApiError ? e.message : 'No se pudo conectar con el servidor';
}

function hora(iso: string) {
	return new Date(iso).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' });
}

/** Ejecuta fn ya y despues cada ms, mientras el componente este montado. */
function usePolling(fn: () => void, ms: number, activo = true) {
	const ref = useRef(fn);
	useEffect(() => {
		ref.current = fn;
	});
	useEffect(() => {
		if (!activo) return;
		ref.current();
		const id = window.setInterval(() => ref.current(), ms);
		return () => window.clearInterval(id);
	}, [ms, activo]);
}

function EstadoPill({ estado }: { estado: EstadoTicket }) {
	return <span className={`pill pill-ticket-${estado.toLowerCase()}`}>{ETIQUETA_ESTADO[estado]}</span>;
}

function ListaTickets({
	tickets,
	seleccionado,
	onSeleccionar,
	vacio,
	accion,
}: {
	tickets: TicketResponse[];
	seleccionado: number | null;
	onSeleccionar?: (t: TicketResponse) => void;
	vacio: string;
	accion?: (t: TicketResponse) => ReactNode;
}) {
	if (tickets.length === 0) return <p className="ayuda">{vacio}</p>;
	return (
		<ul className="lista-tickets">
			{tickets.map((t) => (
				<li key={t.id} className={t.id === seleccionado ? 'ticket-item ticket-item-activo' : 'ticket-item'}>
					<button
						type="button"
						className="ticket-item-boton"
						onClick={() => onSeleccionar?.(t)}
						disabled={!onSeleccionar}
						aria-pressed={t.id === seleccionado}
					>
						<span className="ticket-item-cabecera">
							<strong>Ticket #{t.id}</strong>
							<EstadoPill estado={t.estado} />
						</span>
						<span className="pill pill-categoria">{ETIQUETA_CATEGORIA[t.categoria]}</span>
						<span className="ticket-item-motivo">{t.motivo}</span>
						<span className="ticket-item-meta">
							{hora(t.fechaCreacion)}
							{t.viajeId && ` · viaje #${t.viajeId}`}
							{` · usuario #${t.usuarioId}`}
						</span>
					</button>
					{accion?.(t)}
				</li>
			))}
		</ul>
	);
}

/** Historial desde la base al abrir; despues, lo nuevo se consume de la queue. Se deduplica por id. */
function ChatPanel({
	ticket,
	onResolver,
}: {
	ticket: TicketResponse;
	onResolver?: () => Promise<void>;
}) {
	const { usuario } = useAuth();
	const [mensajes, setMensajes] = useState<ChatMensajeResponse[]>([]);
	const [texto, setTexto] = useState('');
	const [error, setError] = useState<string | null>(null);
	const [enviando, setEnviando] = useState(false);
	const [gestionando, setGestionando] = useState(false);
	const mensajesRef = useRef<HTMLDivElement>(null);
	const panelRef = useRef<HTMLElement>(null);
	const inputId = useId();
	const abierto = ticket.estado === 'EN_CHAT';
	// Solo el admin recibe onResolver: es el mismo criterio para poder gestionar al usuario.
	const esAdmin = Boolean(onResolver);

	/*
	 * En el celular la lista y el chat van en una sola columna: al tocar un ticket
	 * el chat se abria varias pantallas mas abajo y parecia que no habia pasado
	 * nada. Ahi se lo trae a la vista. Corre al montar, y ChatPanel se monta de
	 * nuevo con cada ticket elegido (key={ticket.id}). En escritorio el chat ya
	 * esta al lado de la lista, asi que no se mueve nada.
	 */
	useEffect(() => {
		if (!window.matchMedia('(max-width: 720px)').matches) return;
		const suave = !window.matchMedia('(prefers-reduced-motion: reduce)').matches;
		panelRef.current?.scrollIntoView({ block: 'start', behavior: suave ? 'smooth' : 'auto' });
	}, []);

	const agregar = useCallback((nuevos: ChatMensajeResponse[]) => {
		if (nuevos.length === 0) return;
		setMensajes((actuales) => {
			const vistos = new Set(actuales.map((m) => m.id));
			const sumar = nuevos.filter((m) => !vistos.has(m.id));
			return sumar.length ? [...actuales, ...sumar] : actuales;
		});
	}, []);

	// ChatPanel se monta con key={ticket.id}: al cambiar de ticket el estado arranca de cero.
	useEffect(() => {
		chatApi.historial(ticket.id).then(setMensajes).catch((e) => setError(mensajeDeError(e)));
	}, [ticket.id]);

	usePolling(
		() => {
			chatApi.nuevos(ticket.id).then(agregar).catch((e) => setError(mensajeDeError(e)));
		},
		POLL_CHAT_MS,
		abierto,
	);

	/*
	 * Al ultimo mensaje se llega moviendo SOLO la caja del chat. Antes era un
	 * scrollIntoView sobre el final de la lista, y ese mueve tambien la pagina:
	 * al abrir un ticket o al llegar un mensaje, toda la pantalla saltaba.
	 */
	useEffect(() => {
		const caja = mensajesRef.current;
		if (caja) caja.scrollTop = caja.scrollHeight;
	}, [mensajes.length]);

	async function enviar(e: FormEvent) {
		e.preventDefault();
		const contenido = texto.trim();
		if (!contenido) return;
		setEnviando(true);
		setError(null);
		try {
			agregar([await chatApi.enviar(ticket.id, contenido)]);
			setTexto('');
		} catch (err) {
			setError(mensajeDeError(err));
		} finally {
			setEnviando(false);
		}
	}

	return (
		<section ref={panelRef} className="tarjeta chat" aria-label={`Chat del ticket ${ticket.id}`}>
			<header className="chat-cabecera">
				<div>
					<h2>Ticket #{ticket.id}</h2>
					<p className="ayuda">
						{ETIQUETA_CATEGORIA[ticket.categoria]} · {ticket.motivo}
					</p>
				</div>
				<div className="chat-cabecera-acciones">
					<EstadoPill estado={ticket.estado} />
					{esAdmin && abierto && (
						<button type="button" className="boton-chico" onClick={() => setGestionando(true)}>
							Gestionar usuario
						</button>
					)}
				</div>
			</header>

			{gestionando && (
				<GestionUsuarioModal
					ticketId={ticket.id}
					onCerrar={() => setGestionando(false)}
					// Cada accion deja un mensaje en el chat a nombre del admin: se trae del historial.
					onAccion={() => {
						chatApi.historial(ticket.id).then(agregar).catch((e) => setError(mensajeDeError(e)));
					}}
				/>
			)}

			<div ref={mensajesRef} className="chat-mensajes" role="log" aria-live="polite">
				{mensajes.length === 0 && (
					<p className="ayuda centrado">
						{ticket.estado === 'ABIERTO'
							? 'Un administrador va a tomar tu ticket. Cuando lo haga, se habilita el chat.'
							: 'Todavía no hay mensajes.'}
					</p>
				)}
				{mensajes.map((m) => {
					const propio = m.remitenteId === usuario?.id;
					return (
						<div key={m.id} className={propio ? 'burbuja burbuja-propia' : 'burbuja'}>
							<span className="burbuja-autor">
								{propio ? 'Vos' : m.remitenteRol === 'ADMIN' ? 'Administrador' : 'Usuario'}
							</span>
							<p>{m.contenido}</p>
							<span className="burbuja-hora">{hora(m.fechaEnvio)}</span>
						</div>
					);
				})}
			</div>

			{error && <p className="aviso aviso-alerta">{error}</p>}

			{abierto ? (
				<form className="chat-form" onSubmit={enviar}>
					<label htmlFor={inputId} className="solo-lectores">
						Mensaje
					</label>
					<input
						id={inputId}
						value={texto}
						maxLength={1000}
						onChange={(e) => setTexto(e.target.value)}
						placeholder="Escribí un mensaje"
						autoComplete="off"
						enterKeyHint="send"
					/>
					<button type="submit" disabled={enviando || !texto.trim()}>
						Enviar
					</button>
				</form>
			) : (
				ticket.estado === 'RESUELTO' && <p className="aviso aviso-ok">Ticket resuelto: el chat quedó cerrado.</p>
			)}

			{onResolver && abierto && (
				<button type="button" className="boton-secundario" onClick={onResolver}>
					Marcar como resuelto
				</button>
			)}
		</section>
	);
}

function FormularioTicket({ onCreado }: { onCreado: (t: TicketResponse) => void }) {
	const [motivo, setMotivo] = useState('');
	const [categoria, setCategoria] = useState<CategoriaTicket | ''>('');
	const [viajeId, setViajeId] = useState('');
	const [viajes, setViajes] = useState<ViajeResponse[]>([]);
	const [error, setError] = useState<string | null>(null);
	const [enviando, setEnviando] = useState(false);
	const motivoId = useId();
	const viajeSelectId = useId();
	const categoriaId = useId();

	useEffect(() => {
		viajesApi.listarPropios().then(setViajes).catch(() => setViajes([]));
	}, []);

	async function crear(e: FormEvent) {
		e.preventDefault();
		setEnviando(true);
		setError(null);
		try {
			if (!categoria) return;
			const ticket = await ticketsApi.crear({
				categoria,
				motivo: motivo.trim(),
				viajeId: viajeId ? Number(viajeId) : null,
			});
			setMotivo('');
			setCategoria('');
			setViajeId('');
			onCreado(ticket);
		} catch (err) {
			setError(mensajeDeError(err));
		} finally {
			setEnviando(false);
		}
	}

	return (
		<form className="tarjeta formulario" onSubmit={crear}>
			<h2>Reportar un problema</h2>
			<div className="campo">
				<label htmlFor={categoriaId}>Categoría</label>
				<select
					id={categoriaId}
					value={categoria}
					required
					onChange={(e) => setCategoria(e.target.value as CategoriaTicket | '')}
				>
					<option value="" disabled>
						Elegí una categoría
					</option>
					{CATEGORIAS.map((c) => (
						<option key={c} value={c}>
							{ETIQUETA_CATEGORIA[c]}
						</option>
					))}
				</select>
			</div>
			<div className="campo">
				<label htmlFor={motivoId}>¿Qué pasó?</label>
				<textarea
					id={motivoId}
					value={motivo}
					maxLength={500}
					rows={3}
					required
					onChange={(e) => setMotivo(e.target.value)}
					placeholder="Ej: no puedo anclar la bici, el anclaje no traba"
				/>
			</div>
			<div className="campo">
				<label htmlFor={viajeSelectId}>Viaje relacionado (opcional)</label>
				<select id={viajeSelectId} value={viajeId} onChange={(e) => setViajeId(e.target.value)}>
					<option value="">Ninguno</option>
					{viajes.map((v) => (
						<option key={v.id} value={v.id}>
							#{v.id} · bici {v.codigoBicicleta} · {hora(v.fechaInicio)}
							{v.fechaFin ? '' : ' (en curso)'}
						</option>
					))}
				</select>
			</div>
			{error && <p className="aviso aviso-alerta">{error}</p>}
			<button type="submit" disabled={enviando || !motivo.trim() || !categoria}>
				Enviar ticket
			</button>
		</form>
	);
}

function VistaCliente() {
	const [tickets, setTickets] = useState<TicketResponse[]>([]);
	const [seleccionadoId, setSeleccionadoId] = useState<number | null>(null);
	const [error, setError] = useState<string | null>(null);
	const [categoria, setCategoria] = useState<CategoriaTicket | ''>('');

	const recargar = useCallback(() => {
		ticketsApi
			.listarPropios(categoria)
			.then((ts) => {
				setTickets(ts);
				setError(null);
			})
			.catch((e) => setError(mensajeDeError(e)));
	}, [categoria]);

	usePolling(recargar, POLL_TICKETS_MS);
	useEffect(recargar, [recargar]);

	const seleccionado = tickets.find((t) => t.id === seleccionadoId) ?? null;

	return (
		<div className="tickets-layout">
			<div className="tickets-columna">
				<FormularioTicket
					onCreado={(t) => {
						setSeleccionadoId(t.id);
						recargar();
					}}
				/>
				<section className="tarjeta">
					<h2>Mis tickets</h2>
					<FiltroCategoria valor={categoria} onCambio={setCategoria} />
					{error && <p className="aviso aviso-alerta">{error}</p>}
					<ListaTickets
						tickets={tickets}
						seleccionado={seleccionadoId}
						onSeleccionar={(t) => setSeleccionadoId(t.id)}
						vacio="Todavía no reportaste ningún problema."
					/>
				</section>
			</div>
			{seleccionado ? (
				<ChatPanel key={seleccionado.id} ticket={seleccionado} />
			) : (
				<p className="tarjeta ayuda centrado">Elegí un ticket para ver su estado y chatear con el administrador.</p>
			)}
		</div>
	);
}

function VistaAdmin() {
	const [abiertos, setAbiertos] = useState<TicketResponse[]>([]);
	const [asignados, setAsignados] = useState<TicketResponse[]>([]);
	const [seleccionadoId, setSeleccionadoId] = useState<number | null>(null);
	const [error, setError] = useState<string | null>(null);
	const [categoria, setCategoria] = useState<CategoriaTicket | ''>('');

	const recargar = useCallback(() => {
		Promise.all([ticketsApi.listarAbiertos(categoria), ticketsApi.listarAsignados(categoria)])
			.then(([ab, as]) => {
				setAbiertos(ab);
				setAsignados(as);
				setError(null);
			})
			.catch((e) => setError(mensajeDeError(e)));
	}, [categoria]);

	usePolling(recargar, POLL_TICKETS_MS);
	useEffect(recargar, [recargar]);

	async function tomar(id: number) {
		try {
			await ticketsApi.tomar(id);
			setSeleccionadoId(id);
			recargar();
		} catch (e) {
			setError(mensajeDeError(e));
			recargar();
		}
	}

	async function resolver(id: number) {
		try {
			await ticketsApi.resolver(id);
			recargar();
		} catch (e) {
			setError(mensajeDeError(e));
		}
	}

	const seleccionado = asignados.find((t) => t.id === seleccionadoId) ?? null;
	const enAtencion = asignados.filter((t) => t.estado === 'EN_CHAT');
	const resueltos = asignados.filter((t) => t.estado === 'RESUELTO');

	return (
		<div className="tickets-layout">
			<div className="tickets-columna">
				<div className="tarjeta">
					<FiltroCategoria valor={categoria} onCambio={setCategoria} />
				</div>
				{error && <p className="aviso aviso-alerta">{error}</p>}
				<section className="tarjeta">
					<h2>Sin tomar ({abiertos.length})</h2>
					<ListaTickets
						tickets={abiertos}
						seleccionado={null}
						vacio="No hay tickets esperando."
						accion={(t) => (
							<button type="button" className="boton-chico" onClick={() => tomar(t.id)}>
								Tomar
							</button>
						)}
					/>
				</section>
				<section className="tarjeta">
					<h2>En atención ({enAtencion.length})</h2>
					<ListaTickets
						tickets={enAtencion}
						seleccionado={seleccionadoId}
						onSeleccionar={(t) => setSeleccionadoId(t.id)}
						vacio="No estás atendiendo ningún ticket."
					/>
				</section>
				{resueltos.length > 0 && (
					<section className="tarjeta">
						<h2>Resueltos ({resueltos.length})</h2>
						<ListaTickets
							tickets={resueltos}
							seleccionado={seleccionadoId}
							onSeleccionar={(t) => setSeleccionadoId(t.id)}
							vacio=""
						/>
					</section>
				)}
			</div>
			{seleccionado ? (
				<ChatPanel key={seleccionado.id} ticket={seleccionado} onResolver={() => resolver(seleccionado.id)} />
			) : (
				<p className="tarjeta ayuda centrado">Tomá un ticket o elegí uno en atención para abrir el chat con el usuario.</p>
			)}
		</div>
	);
}

export function TicketsPage() {
	const { usuario } = useAuth();
	const esAdmin = usuario?.rol === 'ADMIN';

	return (
		<div className="contenido">
			<header className="encabezado-pagina">
				<h1>{esAdmin ? 'Tickets de usuarios' : 'Ayuda y tickets'}</h1>
				<p className="bajada">
					{esAdmin
						? 'Tomá un ticket para atenderlo: el chat con el usuario se abre hasta que lo marques como resuelto.'
						: '¿Tuviste un problema con la bici o el anclaje? Reportalo y un administrador te responde por chat.'}
				</p>
			</header>
			{esAdmin ? <VistaAdmin /> : <VistaCliente />}
		</div>
	);
}
