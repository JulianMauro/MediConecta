/**
 * Concordancia de numero para los textos de la interfaz.
 * Evita los "1 estaciones" y los "estación(es)" entre parentesis, que se leen mal
 * y peor todavia en un lector de pantalla.
 */
export function plural(cantidad: number, singular: string, plural: string): string {
	return `${cantidad} ${cantidad === 1 ? singular : plural}`;
}
