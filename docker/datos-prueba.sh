#!/usr/bin/env bash
# Carga datos de prueba por la API (respeta las validaciones del sistema):
#   - 1 almacen, 3 estaciones con 6 anclajes cada una
#   - 12 bicis: 4 ancladas en cada estacion (quedan 2 anclajes libres por estacion)
#   - en Plaza de Mayo un anclaje FUERA_SERVICIO, para reproducir "no puedo anclar la bici"
#   - 2 clientes de prueba (para el chat de tickets hace falta un cliente y el admin)
#   - escenarios de soporte: un cliente por cada problema que el admin resuelve desde el
#     modal "Gestionar usuario", cada uno con su ticket ya tomado por el admin (ver abajo)
#
# Requiere la app corriendo en localhost:8080 y el contenedor de Postgres (para atrasar la
# hora de inicio de algunos viajes: es la unica forma de tener strikes sin esperar 45 minutos).
# Uso: bash docker/datos-prueba.sh   (en Windows, con el bash de Git, no el de WSL)
# Cada parte se carga una sola vez: si ya esta, la saltea.

API=${API:-http://localhost:8080/api}
ADMIN_EMAIL=${ADMIN_EMAIL:-admin@mediconecta.com}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-admin1234}
CLIENTE_PASSWORD=${CLIENTE_PASSWORD:-PruebaPB2026}
CLIENTES=("cliente1@peoplebikes.test" "cliente2@peoplebikes.test")
PG_CONTAINER=${PG_CONTAINER:-mediconecta-postgres}
POSTGRES_USER=${POSTGRES_USER:-mediconecta}
POSTGRES_DB=${POSTGRES_DB:-mediconecta_db}

set -e
json() { python -c "import sys,json; d=json.load(sys.stdin); print(eval(sys.argv[1]))" "$1"; }
post() { curl -sf -X "${3:-POST}" "$API$1" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" ${2:+-d "$2"}; }
login() { curl -sf -X POST "$API/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"$1\",\"password\":\"$2\"}" | json "d['accessToken']"; }

TOKEN=$(login "$ADMIN_EMAIL" "$ADMIN_PASSWORD")

if curl -sf "$API/bicicletas" -H "Authorization: Bearer $TOKEN" | json "any(b['codigo']=='PB-001' for b in d)" | grep -q True; then
	echo "La flota base ya esta cargada (existe la bici PB-001): se saltea."
else
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
fi

# ==========================================================================
# Escenarios de soporte: un cliente por problema, con su ticket EN_CHAT tomado por el admin.
# Entrando como admin a Tickets > En atencion, cada ticket tiene el boton "Gestionar usuario".
# ==========================================================================

if login soporte.anclaje@peoplebikes.test "$CLIENTE_PASSWORD" > /dev/null 2>&1; then
	echo "Los escenarios de soporte ya estan cargados (existe soporte.anclaje@peoplebikes.test): se saltean."
	exit 0
fi

sql() { docker exec "$PG_CONTAINER" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -qAtc "$1" > /dev/null; }
como() { curl -sf -X "$2" "$API$3" -H "Authorization: Bearer $1" -H "Content-Type: application/json" ${4:+-d "$4"}; }

PLAN=$(post /membresias '{"nombre":"Plan Prueba Soporte","tipo":"MENSUAL","tiempoPermitidoMinutos":30,"precio":5000,"tarifaMinutoExtra":50,"viajesPorDia":10,"tiempoEsperaMinutos":0}' | json "d['id']")
echo "plan de prueba (id $PLAN): 30 min por viaje, \$50 por minuto extra"

# cliente NOMBRE EMAIL -> token. Lo registra con el password de prueba.
cliente() {
	curl -sf -o /dev/null -X POST "$API/auth/register" -H "Content-Type: application/json" \
		-d "{\"nombre\":\"$1\",\"apellido\":\"Soporte\",\"email\":\"$2\",\"password\":\"$CLIENTE_PASSWORD\",\"telefono\":\"11-5555-0000\"}"
	login "$2" "$CLIENTE_PASSWORD"
}
# Contrata el plan y lo paga (simula la pasarela).
con_plan() {
	local pago
	pago=$(como "$1" POST /suscripciones "{\"membresiaId\":$PLAN}" | json "d['id']")
	como "$1" POST "/pagos/$pago/confirmar" '{"referenciaPasarela":"SEED"}' > /dev/null
}
# Saca la primera bici disponible y atrasa el inicio N minutos. Imprime el id del viaje.
sacar_bici() {
	local codigo viaje
	codigo=$(post /bicicletas "" GET | json "[b['codigo'] for b in d if b['estado']=='DISPONIBLE' and b.get('anclajeId')][0]")
	viaje=$(como "$1" POST /viajes/iniciar "{\"codigoBicicleta\":\"$codigo\"}" | json "d['id']")
	sql "update viaje set fecha_inicio = now() - interval '$2 minutes' where id = $viaje"
	echo "$viaje"
}
# Devuelve la bici en el primer anclaje libre de cualquier estacion.
devolver() {
	local anclaje
	anclaje=$(for e in $(post /estaciones "" GET | json "' '.join(str(x['id']) for x in d if x['activa'])"); do
		post "/estaciones/$e/anclajes" "" GET | json "' '.join(str(a['id']) for a in d if a['estado']=='LIBRE')"
	done | tr ' ' '\n' | grep -m1 .)
	como "$1" POST /viajes/finalizar "{\"anclajeDestinoId\":$anclaje}" > /dev/null
}
# ticket TOKEN CATEGORIA VIAJE_ID|null MOTIVO MENSAJE -> lo crea, el admin lo toma y el cliente escribe.
ticket() {
	local id
	id=$(como "$1" POST /tickets "{\"categoria\":\"$2\",\"viajeId\":$3,\"motivo\":\"$4\"}" | json "d['id']")
	post "/tickets/$id/tomar" "" PATCH > /dev/null
	como "$1" POST "/chat/$id/mensajes" "{\"contenido\":\"$5\"}" > /dev/null
	echo "$id"
}

# 1. Viaje en curso y el anclaje no traba: cerrar el viaje (con hora anterior) y marcar el anclaje roto.
T=$(cliente Ana soporte.anclaje@peoplebikes.test); con_plan "$T"
V=$(sacar_bici "$T" 40)
ID=$(ticket "$T" ANCLAJE "$V" "Llegue a la estacion hace rato y el anclaje no traba la bici" \
	"Hola, llegue a los 20 minutos de viaje pero ningun anclaje me traba la bici. Me sigue corriendo el tiempo.")
echo "ticket $ID: soporte.anclaje -> viaje #$V en curso hace 40 min (cerrar con hora anterior)"

# 2. Viaje en curso y la bici se rompio en el camino: cerrar sin anclaje, la bici va al almacen.
T=$(cliente Bruno soporte.bicirota@peoplebikes.test); con_plan "$T"
V=$(sacar_bici "$T" 15)
ID=$(ticket "$T" BICICLETA "$V" "Se me salio la cadena y no puedo seguir" \
	"Estoy a mitad de camino con la bici rota, no puedo llevarla a ninguna estacion.")
echo "ticket $ID: soporte.bicirota -> viaje #$V en curso (cerrar sin anclaje, bici EN_REPARACION)"

# 3. Ya devolvio la bici y le cobraron tiempo extra por una falla: anular el strike o corregir el viaje.
T=$(cliente Carla soporte.strike@peoplebikes.test); con_plan "$T"
V=$(sacar_bici "$T" 50); devolver "$T"
ID=$(ticket "$T" PAGO "$V" "Me cobraron tiempo extra pero la estacion estaba llena" \
	"La estacion estaba llena y tuve que ir a otra, por eso tarde 50 minutos. Me pusieron un strike.")
echo "ticket $ID: soporte.strike -> viaje #$V de 50 min con strike ACTIVO y cobro pendiente"

# 4. Cuenta bloqueada por 3 strikes: anular uno (la desbloquea sola) o desbloquear a mano.
T=$(cliente Diego soporte.bloqueado@peoplebikes.test); con_plan "$T"
for minutos in 45 50 60; do V=$(sacar_bici "$T" "$minutos"); devolver "$T"; done
ID=$(ticket "$T" CUENTA null "Mi cuenta esta bloqueada" \
	"No puedo sacar ninguna bici, dice que mi cuenta esta bloqueada. Dos de esos viajes fueron por anclajes rotos.")
echo "ticket $ID: soporte.bloqueado -> 3 strikes activos y cuenta bloqueada"

# 5. Pago el plan y no se le activo: confirmar el pago a mano.
T=$(cliente Elena soporte.pago@peoplebikes.test)
como "$T" POST /suscripciones "{\"membresiaId\":$PLAN}" > /dev/null
ID=$(ticket "$T" PAGO null "Pague el plan y no se activo" \
	"Pague el plan mensual con la tarjeta pero me sigue diciendo que no tengo plan. El comprobante es MP-778899.")
echo "ticket $ID: soporte.pago -> pago del plan PENDIENTE (confirmar con la referencia MP-778899)"

echo "escenarios de soporte cargados: entran con el password de CLIENTE_PASSWORD"
