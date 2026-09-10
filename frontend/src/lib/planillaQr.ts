import QRCode from 'qrcode';

/** Escapa lo que va a interpolarse como HTML. Los códigos los define un admin, pero
 *  igual no hay razón para confiar en que no traigan `<` o comillas. */
function escaparHtml(texto: string): string {
	return texto.replace(
		/[&<>"']/g,
		(c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!,
	);
}

const ESTILO_PLANILLA = `
	body { font-family: 'Segoe UI', system-ui, sans-serif; margin: 24px; color: #1f2937; }
	h1 { font-size: 18px; margin: 0 0 16px; }
	button { margin-bottom: 20px; padding: 8px 16px; font-size: 14px; }
	.grilla { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
	figure { margin: 0; padding: 12px; text-align: center; border: 1px solid #d1d5db; border-radius: 8px; break-inside: avoid; }
	figure img { width: 100%; height: auto; }
	figcaption { margin-top: 8px; font-family: ui-monospace, Consolas, monospace; font-weight: 700; }
	@media print { button { display: none; } }
`;

/**
 * Abre una ventana nueva con una grilla imprimible de códigos QR, una celda por
 * código. Va en una ventana aparte a propósito: es un documento con su propio CSS
 * de impresión, sin el chrome de la app ni su fondo de alto contraste encima.
 *
 * Lanza si el navegador bloquea la ventana emergente: el llamador avisa al usuario.
 */
export async function abrirPlanillaQr(codigos: string[]): Promise<void> {
	const ventana = window.open('', '_blank', 'width=900,height=1200');
	if (!ventana) throw new Error('ventana emergente bloqueada');

	const celdas = await Promise.all(
		codigos.map(async (codigo) => {
			// Los data URLs se generan acá y se incrustan: la ventana nueva no comparte
			// el bundle, así que no puede llamar a `qrcode` por su cuenta.
			const dataUrl = await QRCode.toDataURL(codigo, { width: 240, margin: 2 });
			const seguro = escaparHtml(codigo);
			return `<figure><img src="${dataUrl}" alt="QR de ${seguro}" /><figcaption>${seguro}</figcaption></figure>`;
		}),
	);

	const estilo = ventana.document.createElement('style');
	estilo.textContent = ESTILO_PLANILLA;
	ventana.document.head.appendChild(estilo);
	ventana.document.title = 'Planilla de QR — PeopleBikes';
	ventana.document.body.innerHTML = `
		<h1>Planilla de QR — ${codigos.length} ${codigos.length === 1 ? 'bici' : 'bicis'}</h1>
		<button type="button" onclick="window.print()">Imprimir</button>
		<div class="grilla">${celdas.join('')}</div>
	`;
}
