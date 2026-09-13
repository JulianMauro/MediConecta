import { useCallback, useEffect, useState } from 'react';
import { strikesApi } from '../api/strikes';
import type { StrikeResponse } from '../types/dto';

/**
 * Strikes del usuario.
 * Vive en un hook para que "Mi cuenta" comparta los mismos datos entre la tabla de
 * strikes y el contador de notificaciones, y los recargue cuando se salda un pago.
 */
export function useStrikes() {
	const [strikes, setStrikes] = useState<StrikeResponse[]>([]);

	const recargar = useCallback(() => {
		return strikesApi.listarPropios().then(setStrikes);
	}, []);

	useEffect(() => {
		recargar();
	}, [recargar]);

	return { strikes, recargar };
}
