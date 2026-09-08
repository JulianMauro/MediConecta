interface Props {
	/** Número a mostrar. Se trunca y se toma su valor absoluto: el odómetro no representa signo. */
	value: number;
	className?: string;
}

const GLIFOS = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];

/**
 * Una sola columna de dígito: una tira vertical con los 10 glifos apilados, corrida
 * con `translateY` hasta el que corresponde. El corrimiento tiene `transition` en CSS
 * (ver .odometro-tira en index.css), así que cuando cambia `valor` el dígito no salta:
 * se desliza, como el rodillo de un contador mecánico.
 *
 * Nota: al haber solo 10 glifos (0-9), un acarreo que hace bajar el dígito (ej. 9→0 al
 * pasar de 19 a 20) desliza "hacia atrás" en vez de seguir de largo hacia adelante.
 * Es la única simplificación frente a un odómetro físico real; para un contador que
 * cambia de a poco no se nota.
 */
function Digito({ valor }: { valor: number }) {
	return (
		<span className="odometro-col" aria-hidden="true">
			<span className="odometro-tira" style={{ transform: `translateY(-${valor}em)` }}>
				{GLIFOS.map((g) => (
					<i key={g}>{g}</i>
				))}
			</span>
		</span>
	);
}

/**
 * Número que se anima con el efecto "rodillo analógico" al cambiar de valor, en vez
 * de reemplazarse de golpe. El texto real queda aparte para lectores de pantalla:
 * los dígitos visuales son puramente decorativos (aria-hidden).
 */
export function Odometer({ value, className }: Props) {
	const texto = Math.abs(Math.trunc(value)).toString();

	return (
		<span className={`odometro ${className ?? ''}`}>
			<span className="solo-lectores">{texto}</span>
			{texto.split('').map((c, i) => (
				<Digito key={i} valor={Number(c)} />
			))}
		</span>
	);
}
