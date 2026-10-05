#!/usr/bin/env bash
# Carga datos de prueba por la API (respeta las validaciones del sistema):
#   - 1 almacen, 3 estaciones con 6 anclajes cada una
#   - 12 bicis: 4 ancladas en cada estacion (quedan 2 anclajes libres por estacion)
#   - en Plaza de Mayo un anclaje FUERA_SERVICIO, para reproducir "no puedo anclar la bici"
#   - 2 clientes de prueba (para el chat de tickets hace falta un cliente y el admin)
#
# Requiere la app corriendo en localhost:8080. Uso: bash docker/datos-prueba.sh
# Si los datos ya estan cargados no hace nada.

API=${API:-http://localhost:8080/api}
ADMIN_EMAIL=${ADMIN_EMAIL:-admin@mediconecta.com}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-admin1234}
CLIENTE_PASSWORD=${CLIENTE_PASSWORD:-PruebaPB2026}
CLIENTES=("cliente1@peoplebikes.test" "cliente2@peoplebikes.test")

set -e
json() { python -c "import sys,json; d=json.load(sys.stdin); print(eval(sys.argv[1]))" "$1"; }
post() { curl -sf -X "${3:-POST}" "$API$1" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" ${2:+-d "$2"}; }

TOKEN=$(curl -sf -X POST "$API/auth/login" -H "Content-Type: application/json" \
	-d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" | json "d['accessToken']")

if curl -sf "$API/bicicletas" -H "Authorization: Bearer $TOKEN" | json "any(b['codigo']=='PB-001' for b in d)" | grep -q True; then
	echo "Los datos de prueba ya estan cargados (existe la bici PB-001). No se hizo nada."
	exit 0
fi

ALMACEN=$(post /almacenes '{"nombre":"Deposito Central","direccion":"Av. Velez Sarsfield 1500, CABA","capacidad":50}' | json "d['id']")
echo "almacen Deposito Central (id $ALMACEN)"

ESTACIONES=(
	'{"nombre":"Plaza de Mayo","direccion":"Av. Rivadavia 100, CABA","latitud":-34.6083,"longitud":-58.3712,"capacidad":6}'
	'{"nombre":"Obelisco","direccion":"Av. Corrientes 1000, CABA","latitud":-34.6037,"longitud":-58.3816,"capacidad":6}'
	'{"nombre":"Plaza Italia","direccion":"Av. Santa Fe 4000, CABA","latitud":-34.5813,"longitud":-58.4207,"capacidad":6}'
)
EST_IDS=()
for body in "${ESTACIONES[@]}"; do
	ID=$(post /estaciones "$body" | json "d['id']")
	EST_IDS+=("$ID")
	for n in 1 2 3 4 5 6; do post "/estaciones/$ID/anclajes" "{\"numero\":$n}" > /dev/null; done
	echo "estacion $(echo "$body" | json "d['nombre']") (id $ID) con 6 anclajes"
done

BICI_IDS=()
for i in $(seq -w 1 12); do
	BICI_IDS+=("$(post /bicicletas "{\"codigo\":\"PB-0$i\",\"almacenId\":$ALMACEN}" | json "d['id']")")
done
echo "12 bicis creadas en el almacen (PB-001 a PB-012)"

for e in 0 1 2; do
	IDS=$(IFS=,; echo "${BICI_IDS[*]:$((e * 4)):4}")
	post /movimientos-bicis "{\"origenAlmacenId\":$ALMACEN,\"destinoEstacionId\":${EST_IDS[$e]},\"bicicletaIds\":[$IDS]}" > /dev/null
done
echo "4 bicis ancladas en cada estacion"

# Un anclaje libre de Plaza de Mayo queda fuera de servicio.
ROTO=$(curl -sf "$API/estaciones/${EST_IDS[0]}/anclajes" -H "Authorization: Bearer $TOKEN" \
	| json "[a['id'] for a in d if a['estado']=='LIBRE'][-1]")
post "/anclajes/$ROTO/fuera-servicio" "" PATCH > /dev/null
echo "anclaje $ROTO de Plaza de Mayo FUERA_SERVICIO"

for i in "${!CLIENTES[@]}"; do
	curl -s -o /dev/null -X POST "$API/auth/register" -H "Content-Type: application/json" \
		-d "{\"nombre\":\"Cliente\",\"apellido\":\"Prueba $((i + 1))\",\"email\":\"${CLIENTES[$i]}\",\"password\":\"$CLIENTE_PASSWORD\"}"
done
echo "clientes de prueba: ${CLIENTES[*]} (password en CLIENTE_PASSWORD, ver arriba en este script)"
