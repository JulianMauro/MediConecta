import type { ReactNode } from 'react';

/**
 * Iconos de la barra de pestañas del celular y de las acciones de calle.
 *
 * Dibujados a mano en SVG y no con glifos ni emoji: un caracter cambia de forma,
 * peso y alineacion segun la fuente que resuelva cada telefono. Todos comparten
 * la misma grilla de 24px, el mismo trazo de 1.75 y puntas redondeadas, que es
 * lo que los hace leerse como una familia.
 *
 * Son siempre decorativos (aria-hidden): el nombre de la accion lo da el texto
 * que los acompaña, nunca el dibujo solo.
 */
function Svg({ children }: { children: ReactNode }) {
	return (
		<svg
			className="icono"
			width="24"
			height="24"
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			strokeWidth="1.75"
			strokeLinecap="round"
			strokeLinejoin="round"
			aria-hidden="true"
			focusable="false"
		>
			{children}
		</svg>
	);
}

/** Inicio: la bici, porque la pantalla de inicio es la que la desbloquea. */
export function IconoBici() {
	return (
		<Svg>
			<circle cx="5.5" cy="16" r="3.5" />
			<circle cx="18.5" cy="16" r="3.5" />
			<path d="M5.5 16 9 8.5h6.2L18.5 16" />
			<path d="M9 8.5 12.2 16H5.5" />
			<path d="m12.2 16 3-7.5" />
			<path d="M7.6 6h3" />
			<path d="M15.2 8.5 14.4 5.5h2.4" />
		</Svg>
	);
}

export function IconoMapa() {
	return (
		<Svg>
			<path d="M19 10.2c0 5.2-7 10.3-7 10.3s-7-5.1-7-10.3a7 7 0 0 1 14 0Z" />
			<circle cx="12" cy="10" r="2.6" />
		</Svg>
	);
}

/** Estaciones: el listado de la red, una fila por estacion con su barra de disponibilidad. */
export function IconoEstaciones() {
	return (
		<Svg>
			<path d="M4 6h3M4 12h3M4 18h3" />
			<path d="M10 6h10M10 12h10M10 18h10" />
		</Svg>
	);
}

/** Planes: un pase, que es lo que se contrata. */
export function IconoPlanes() {
	return (
		<Svg>
			<rect x="3" y="5.5" width="18" height="13" rx="2.5" />
			<path d="M3 10h18" />
			<path d="M7 14.5h4" />
		</Svg>
	);
}

/** Tickets: la conversacion con el administrador. */
export function IconoTickets() {
	return (
		<Svg>
			<path d="M20 12a7.5 7.5 0 0 1-11 6.6L4.5 20l1.3-4.1A7.5 7.5 0 1 1 20 12Z" />
			<path d="M9 11h6M9 14h3.5" />
		</Svg>
	);
}

export function IconoCuenta() {
	return (
		<Svg>
			<circle cx="12" cy="8" r="3.75" />
			<path d="M4.5 20a7.5 7.5 0 0 1 15 0" />
		</Svg>
	);
}

/** Visor de QR: las cuatro esquinas del encuadre y una linea de lectura. */
export function IconoEscanear() {
	return (
		<Svg>
			<path d="M4 8.5V6a2 2 0 0 1 2-2h2.5M15.5 4H18a2 2 0 0 1 2 2v2.5M20 15.5V18a2 2 0 0 1-2 2h-2.5M8.5 20H6a2 2 0 0 1-2-2v-2.5" />
			<path d="M7.5 12h9" />
		</Svg>
	);
}

export function IconoSalir() {
	return (
		<Svg>
			<path d="M10 4.5H6.5a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2H10" />
			<path d="M14.5 8 18.5 12l-4 4M18.5 12H9.5" />
		</Svg>
	);
}

export function IconoCerrar() {
	return (
		<Svg>
			<path d="m6.5 6.5 11 11M17.5 6.5l-11 11" />
		</Svg>
	);
}
