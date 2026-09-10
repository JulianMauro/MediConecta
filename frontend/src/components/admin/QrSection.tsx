import { useEffect, useState } from 'react';
import { bicicletasApi } from '../../api/bicicletas';
import { Field } from '../ui/Field';
import { QrCanvas } from './QrCanvas';
import { abrirPlanillaQr } from '../../lib/planillaQr';
import type { BicicletaResponse } from '../../types/dto';

/**
 * Generadora de QR para pegar en las bicis. El QR es un método de captura del
 * frontend: codifica el código pelado de la bici (ej: "BICI-001"), lo mismo que
 * hoy se tipea para desbloquear. No se guarda nada — un QR es la codificación de
 * un string.
 */
export function QrSection() {
	const [bicicletas, setBicicletas] = useState<BicicletaResponse[]>([]);
	const [manual, setManual] = useState('');
	const [seleccion, setSeleccion] = useState('');
	const [preparando, setPreparando] = useState(false);
	const [errorPlanilla, setErrorPlanilla] = useState<string | null>(null);

	useEffect(() => {
		bicicletasApi.listar().then(setBicicletas);
	}, []);

	// Lo tipeado a mano gana sobre lo elegido en el selector: sirve para fabricar
	// el QR de una bici que todavía no está cargada en el sistema.
	const codigo = manual.trim() || seleccion;

	async function imprimirPlanilla() {
		setErrorPlanilla(null);
		setPreparando(true);
		try {
			await abrirPlanillaQr(bicicletas.map((b) => b.codigo));
		} catch {
			setErrorPlanilla(
				'No se pudo abrir la planilla. Revisá que el navegador no esté bloqueando ventanas emergentes.',
			);
		} finally {
			setPreparando(false);
		}
	}

	return (
		<section className="tarjeta">
			<div className="tarjeta-encabezado">
				<div>
					<h2>Generar QR</h2>
					<p className="ayuda">
						El QR codifica el código pelado de la bici (ej: <code>BICI-001</code>), lo mismo que se tipea
						para desbloquear. Generá la planilla, imprimila y pegá cada sticker en su bici.
					</p>
				</div>
			</div>

			<div className="qr-generador">
				<div className="qr-generador-controles">
					<Field label="Código manual" helpText="Para una bici que todavía no está cargada en el sistema.">
						<input value={manual} onChange={(e) => setManual(e.target.value)} placeholder="BICI-001" />
					</Field>
					<Field
						label="…o elegí una bici"
						helpText="Evita errores de tipeo. Si escribís un código arriba, esto se ignora."
					>
						<select
							value={seleccion}
							onChange={(e) => setSeleccion(e.target.value)}
							disabled={manual.trim() !== ''}
						>
							<option value="">Elegí una bici</option>
							{bicicletas.map((b) => (
								<option key={b.id} value={b.codigo}>
									{b.codigo}
								</option>
							))}
						</select>
					</Field>
				</div>

				<div className="qr-preview">
					{codigo ? (
						<>
							<QrCanvas valor={codigo} />
							<p className="qr-preview-codigo">{codigo}</p>
						</>
					) : (
						<p className="ayuda">Tipeá un código o elegí una bici para ver el QR.</p>
					)}
				</div>
			</div>

			<div className="fila">
				<button type="button" onClick={imprimirPlanilla} disabled={bicicletas.length === 0 || preparando}>
					{preparando
						? 'Preparando…'
						: `Imprimir planilla (${bicicletas.length} ${bicicletas.length === 1 ? 'bici' : 'bicis'})`}
				</button>
			</div>
			{errorPlanilla && <p className="error">{errorPlanilla}</p>}
		</section>
	);
}
