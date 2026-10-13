import { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { EVENTO_CAMBIO, fondo, tarjeta, type Opcion } from '../../lib/estilo';

type Preferencia = typeof fondo;

/** Un boton que cicla entre las opciones de una preferencia. */
function BotonEstilo({ preferencia, titulo }: { preferencia: Preferencia; titulo: string }) {
	// Arranca leyendo lo guardado y no el valor por defecto: main.tsx ya lo aplico
	// al documento, y si el boton asumiera otra cosa mostraria una etiqueta que no
	// se corresponde con lo que se esta viendo.
	const [valor, setValor] = useState<string>(preferencia.leer);

	// Si la cambia el otro boton de la misma preferencia, este se pone al dia.
	useEffect(() => {
		function alCambiar(e: Event) {
			const { atributo, valor: nuevo } = (e as CustomEvent<{ atributo: string; valor: string }>).detail;
			if (atributo === preferencia.atributo) setValor(nuevo);
		}
		window.addEventListener(EVENTO_CAMBIO, alCambiar);
		return () => window.removeEventListener(EVENTO_CAMBIO, alCambiar);
	}, [preferencia]);

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
	const { usuario } = useAuth();
	/*
	 * Con sesion el panel flotante se oculta (ver index.css): tapaba contenido, y
	 * en el celular ademas chocaba con la barra de pestañas. Las mismas dos
	 * opciones estan en "Mi cuenta" > Ajustes, que es donde una app guarda su
	 * apariencia. Sin sesion sigue flotando: el login es donde mas se ve el fondo.
	 */
	return (
		<div className={usuario ? 'estilo-toggle estilo-toggle-con-sesion' : 'estilo-toggle'}>
			<BotonEstilo preferencia={fondo} titulo="Fondo" />
			<BotonEstilo preferencia={tarjeta} titulo="Tarjetas" />
		</div>
	);
}

/** Las mismas dos opciones, en el flujo de la pagina en vez de flotando. */
export function EstiloOpciones() {
	return (
		<div className="estilo-opciones">
			<BotonEstilo preferencia={fondo} titulo="Fondo" />
			<BotonEstilo preferencia={tarjeta} titulo="Tarjetas" />
		</div>
	);
}
