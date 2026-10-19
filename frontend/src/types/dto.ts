/**
 * Tipos que reflejan uno a uno los DTO/entity del backend (com.mediconecta.modulos.*).
 * Mantenerlos sincronizados con los record de Java: son el contrato real de la API.
 */

// ---- usuario ----

export type Rol = 'ADMIN' | 'CLIENTE';

export interface UsuarioResponse {
	id: number;
	nombre: string;
	apellido: string;
	email: string;
	dni: string | null;
	telefono: string | null;
	rol: Rol;
	activo: boolean;
	fechaCreacion: string;
}

export interface BloqueoCuentaResponse {
	id: number;
	usuarioId: number;
	fechaBloqueo: string;
	motivo: string;
	activo: boolean;
	fechaDesbloqueo: string | null;
}

// ---- auth ----

export interface LoginRequest {
	email: string;
	password: string;
}

export interface RegisterRequest {
	nombre: string;
	apellido: string;
	email: string;
	password: string;
	dni?: string;
	telefono?: string;
}

export interface AuthResponse {
	accessToken: string;
	refreshToken: string;
	tokenType: string;
	expiraEnSegundos: number;
	usuario: UsuarioResponse;
}

// ---- estacion ----

export type EstadoDock = 'LIBRE' | 'OCUPADO' | 'FUERA_SERVICIO';

export interface EstacionRequest {
	nombre: string;
	direccion: string;
	latitud?: number | null;
	longitud?: number | null;
	capacidad: number;
}

/** Datos editables. Sin coordenadas: son derivadas de la direccion (las recalcula el back). */
export interface EstacionUpdateRequest {
	nombre: string;
	direccion: string;
	capacidad: number;
}

export interface EstacionResponse {
	id: number;
	nombre: string;
	direccion: string;
	latitud: number | null;
	longitud: number | null;
	capacidad: number;
	activa: boolean;
}

export interface AnclajeRequest {
	numero: number;
}

export interface AnclajeResponse {
	id: number;
	estacionId: number;
	numero: number;
	estado: EstadoDock;
}

// ---- almacen ----

export interface AlmacenRequest {
	nombre: string;
	direccion: string;
	capacidad: number;
}

export interface AlmacenResponse {
	id: number;
	nombre: string;
	direccion: string;
	capacidad: number;
}

// ---- flota ----

export type EstadoBicicleta = 'DISPONIBLE' | 'EN_VIAJE' | 'DESACTIVADA' | 'EN_REPARACION';

export interface BicicletaRequest {
	codigo: string;
	almacenId: number;
}

export interface BicicletaEstadoRequest {
	estado: EstadoBicicleta;
}

export interface BicicletaResponse {
	id: number;
	codigo: string;
	estado: EstadoBicicleta;
	anclajeId: number | null;
	almacenId: number | null;
}

// ---- membresia ----

export type TipoMembresia = 'INDIVIDUAL' | 'SEMANAL' | 'MENSUAL';
export type EstadoSuscripcion = 'ACTIVA' | 'VENCIDA' | 'CANCELADA';

export interface MembresiaRequest {
	nombre: string;
	tipo: TipoMembresia;
	tiempoPermitidoMinutos: number;
	precio: number;
	tarifaMinutoExtra: number;
	viajesPorDia: number;
	tiempoEsperaMinutos: number;
}

export interface MembresiaResponse {
	id: number;
	nombre: string;
	tipo: TipoMembresia;
	duracionDias: number;
	tiempoPermitidoMinutos: number;
	precio: number;
	tarifaMinutoExtra: number;
	viajesPorDia: number;
	tiempoEsperaMinutos: number;
	activa: boolean;
}

export interface SuscripcionRequest {
	membresiaId: number;
}

export interface SuscripcionResponse {
	id: number;
	usuarioId: number;
	membresiaId: number;
	fechaInicio: string;
	fechaFin: string | null;
	estado: EstadoSuscripcion;
}

// ---- viaje ----

export interface ViajeIniciarRequest {
	codigoBicicleta: string;
}

export interface ViajeFinalizarRequest {
	anclajeDestinoId: number;
}

export interface ViajeResponse {
	id: number;
	usuarioId: number;
	codigoBicicleta: string;
	anclajeOrigenId: number;
	anclajeDestinoId: number | null;
	membresiaId: number;
	fechaInicio: string;
	fechaFin: string | null;
	excedioTiempo: boolean;
	duracionMinutos: number;
}

// ---- strike ----

export type EstadoStrike = 'ACTIVO' | 'SALDADO' | 'ANULADO';

export interface StrikeResponse {
	id: number;
	usuarioId: number;
	viajeId: number;
	fechaGeneracion: string;
	estado: EstadoStrike;
	pagoId: number | null;
}

// ---- notificacion ----

export type TipoNotificacion = 'TIEMPO_EXTRA' | 'STRIKE' | 'BLOQUEO' | 'SOPORTE';

export interface NotificacionResponse {
	id: number;
	usuarioId: number;
	viajeId: number | null;
	tipo: TipoNotificacion;
	mensaje: string;
	fechaEnvio: string;
	leida: boolean;
}

// ---- pago ----

export type EstadoPago = 'PENDIENTE' | 'PAGADO' | 'RECHAZADO' | 'ANULADO';
export type ConceptoPago = 'TIEMPO_EXTRA' | 'MEMBRESIA';

export interface PagoConfirmarRequest {
	referenciaPasarela: string;
}

export interface PagoResponse {
	id: number;
	usuarioId: number;
	viajeId: number | null;
	membresiaId: number | null;
	concepto: ConceptoPago;
	monto: number;
	estado: EstadoPago;
	fechaCreacion: string;
	fechaResolucion: string | null;
	referenciaPasarela: string | null;
}

// ---- logistica ----

export interface MovimientoBicisRequest {
	origenEstacionId?: number | null;
	origenAlmacenId?: number | null;
	destinoEstacionId?: number | null;
	destinoAlmacenId?: number | null;
	bicicletaIds: number[];
}

export interface MovimientoBicisResponse {
	id: number;
	adminId: number;
	fecha: string;
	origenEstacionId: number | null;
	origenAlmacenId: number | null;
	destinoEstacionId: number | null;
	destinoAlmacenId: number | null;
	bicicletaIds: number[];
}

// ---- monitoreo ----

export interface UsuariosActivosResponse {
	cantidad: number;
	timestamp: string;
}

// ---- tickets ----
export type EstadoTicket = 'ABIERTO' | 'EN_CHAT' | 'RESUELTO';
export type CategoriaTicket = 'ANCLAJE' | 'BICICLETA' | 'PAGO' | 'CUENTA' | 'OTRO';

export interface TicketRequest {
	viajeId: number | null;
	categoria: CategoriaTicket;
	motivo: string;
}

export interface TicketResponse {
	id: number;
	usuarioId: number;
	viajeId: number | null;
	adminId: number | null;
	categoria: CategoriaTicket;
	motivo: string;
	estado: EstadoTicket;
	fechaCreacion: string;
	fechaCierre: string | null;
}

// ---- chat P2P ----
export interface ChatMensajeResponse {
	id: number;
	ticketId: number;
	remitenteId: number;
	remitenteRol: Rol;
	contenido: string;
	fechaEnvio: string;
	fechaEntrega: string | null;
}

// ---- soporte (admin resolviendo problemas de un usuario desde su ticket) ----
export type TipoAccionSoporte =
	| 'CERRAR_VIAJE'
	| 'CORREGIR_VIAJE'
	| 'ANULAR_STRIKE'
	| 'CONFIRMAR_PAGO'
	| 'DESBLOQUEAR_CUENTA'
	| 'ANCLAJE_FUERA_SERVICIO';

export interface ViajeSoporteResponse {
	id: number;
	codigoBicicleta: string;
	estacionOrigen: string;
	anclajeOrigenNumero: number;
	/** null si sigue en curso o si la bici termino en un almacen. */
	estacionDestino: string | null;
	anclajeDestinoNumero: number | null;
	fechaInicio: string;
	fechaFin: string | null;
	duracionMinutos: number;
	minutosPermitidos: number;
	excedioTiempo: boolean;
	excesoCondonado: boolean;
}

export interface StrikeSoporteResponse {
	id: number;
	viajeId: number;
	fechaGeneracion: string;
	estado: EstadoStrike;
	pagoId: number | null;
	monto: number | null;
	estadoPago: EstadoPago | null;
}

export interface AccionSoporteResponse {
	id: number;
	ticketId: number;
	adminId: number;
	usuarioId: number;
	tipo: TipoAccionSoporte;
	detalle: string;
	motivo: string;
	fecha: string;
}

export interface PlanVigente {
	suscripcionId: number;
	nombre: string;
	minutosPorViaje: number;
	viajesPorDia: number;
	vence: string;
}

export interface ResumenUsuarioResponse {
	ticketId: number;
	viajeDelTicketId: number | null;
	usuario: UsuarioResponse;
	viajeEnCurso: ViajeSoporteResponse | null;
	ultimosViajes: ViajeSoporteResponse[];
	plan: PlanVigente | null;
	strikesActivos: number;
	strikesParaBloquear: number;
	strikes: StrikeSoporteResponse[];
	pagosPendientes: PagoResponse[];
	bloqueo: BloqueoCuentaResponse | null;
	acciones: AccionSoporteResponse[];
}

export interface ResultadoSoporteResponse {
	accion: AccionSoporteResponse;
	resumen: ResumenUsuarioResponse;
}

/** Un anclaje (anclajeDestinoId) o un almacen (almacenId + estadoBicicleta), no ambos. */
export interface CerrarViajeRequest {
	anclajeDestinoId: number | null;
	almacenId: number | null;
	estadoBicicleta: 'EN_REPARACION' | 'DESACTIVADA' | null;
	/** null = ahora. */
	fechaFin: string | null;
	condonarExceso: boolean;
	motivo: string;
}
