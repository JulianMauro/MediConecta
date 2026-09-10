import { useState } from 'react';
import { fondo, tarjeta, type Opcion } from '../../lib/estilo';

type Preferencia = typeof fondo;

/** Un boton que cicla entre las opciones de una preferencia. */
function BotonEstilo({ preferencia, titulo }: { preferencia: Preferencia; titulo: string }) {
	// Arranca leyendo lo guardado y no el valor por defecto: main.tsx ya lo aplico
	// al documento, y si el boton asumiera otra cosa mostraria una etiqueta que no
	// se corresponde con lo que se esta viendo.
	const [valor, setValor] = useState<string>(preferencia.leer);

	const actual: Opcion = preferencia.actual(valor);
	const siguiente: Opcion = preferencia.siguiente(valor);

	function cambiar() {
		setValor(siguiente.valor);
		preferencia.aplicar(siguiente.valor);
	}

	return (
		<button
			type="button"
			className="boton-secundario"
			onClick={cambiar}
			// El texto visible dice el estado; quien usa un lector de pantalla
			// necesita ademas saber que hace el boton.
			aria-label={`${titulo} actual: ${actual.etiqueta}. Cambiar a ${siguiente.etiqueta}.`}
		>
			{titulo}: {actual.etiqueta}
		</button>
	);
}

/**
 * Panel para cambiar el aspecto de la app en vivo.
 *
 * Va fijo abajo a la derecha y no en la nav porque la nav solo existe con sesion
 * iniciada, y el login es justamente donde mas se ve el fondo.
 */
export function EstiloToggle() {
	return (
		<div className="estilo-toggle">
			<BotonEstilo preferencia={fondo} titulo="Fondo" />
			<BotonEstilo preferencia={tarjeta} titulo="Tarjetas" />
		</div>
	);
}
