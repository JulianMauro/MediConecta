/**
 * Preferencias visuales de la app: que fondo y que estilo de tarjeta se usan.
 *
 * Las dos se aplican como atributos data-* del <html> y no como clases de un
 * componente, por dos motivos: las capas del fondo son pseudo-elementos de body
 * (fuera del arbol de React), y el estilo de tarjeta tiene que alcanzar a todas
 * las .tarjeta de la app sin tocar ni uno de los componentes que la usan.
 */

export type Opcion = { valor: string; etiqueta: string };

/**
 * Fabrica una preferencia: leerla, aplicarla y recordarla.
 *
 * Devuelve funciones y no una clase porque main.tsx necesita aplicar el valor
 * guardado antes de montar React — si esto viviera en un hook, el primer cuadro
 * se pintaria con el valor por defecto y recien despues saltaria al elegido.
 */
function crearPreferencia(atributo: string, clave: string, opciones: Opcion[]) {
	const porDefecto = opciones[0].valor;
	const valores = opciones.map((o) => o.valor);

	function leer(): string {
		try {
			const guardado = localStorage.getItem(clave);
			// Se valida contra la lista: un valor viejo o editado a mano no puede
			// dejar la app con un atributo que ningun CSS entiende.
			if (guardado && valores.includes(guardado)) return guardado;
		} catch {
			/* localStorage puede tirar excepcion, no solo devolver null: pasa en modo
			   privado de algunos navegadores y con el almacenamiento bloqueado. */
		}
		return porDefecto;
	}

	function aplicar(valor: string): void {
		document.documentElement.setAttribute(`data-${atributo}`, valor);
		try {
			localStorage.setItem(clave, valor);
		} catch {
			/* si no se puede guardar, el cambio igual vale para esta sesion */
		}
	}

	function siguiente(valor: string): Opcion {
		const i = opciones.findIndex((o) => o.valor === valor);
		return opciones[(i + 1) % opciones.length];
	}

	function actual(valor: string): Opcion {
		return opciones.find((o) => o.valor === valor) ?? opciones[0];
	}

	return { opciones, leer, aplicar, siguiente, actual };
}

/* El primero de cada lista es el predeterminado. */

export const fondo = crearPreferencia('fondo', 'peoplebikes:fondo', [
	{ valor: 'rayas', etiqueta: 'Rayas' },
	{ valor: 'puntos', etiqueta: 'Puntos' },
	{ valor: 'prisma', etiqueta: 'Prisma' },
]);

export const tarjeta = crearPreferencia('tarjeta', 'peoplebikes:tarjeta', [
	{ valor: 'normal', etiqueta: 'Normal' },
	{ valor: 'ticket', etiqueta: 'Ticket' },
]);

/** Deja el documento con lo ya elegido. Se llama antes del primer render. */
export function aplicarEstiloGuardado(): void {
	fondo.aplicar(fondo.leer());
	tarjeta.aplicar(tarjeta.leer());
}
