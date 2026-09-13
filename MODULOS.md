# PeopleBikes — Mapa de módulos, funciones e interacciones

Complementa a [DIAGRAMAS.md](DIAGRAMAS.md): allá está el modelo de dominio y los
flujos de negocio; acá está **qué función expone cada módulo y qué función de otro
módulo invoca**.

Todo lo que sigue está extraído del código, no del diseño previsto: las flechas son
imports y llamadas reales en `src/main/java/com/mediconecta/modulos/`.

Cada módulo repite las mismas cinco capas:

```
controller  →  recibe HTTP, valida el rol (@PreAuthorize), no tiene lógica
service     →  la lógica de negocio y las transacciones  ←── las flechas salen de acá
repository  →  Spring Data JPA
entity      →  la tabla y sus invariantes
dto         →  lo que entra y sale por HTTP (nunca se expone la entity)
```

---

## 1. Mapa general: módulos, sus funciones y quién llama a quién

Dos tipos de flecha, y la diferencia importa:

- **Línea llena** = un servicio llama a otro servicio. Acoplamiento fuerte y explícito.
- **Línea punteada** = un servicio lee el *repositorio* de otro módulo, sin pasar por
  su servicio. Acoplamiento por datos: más rápido de escribir, pero saltea las reglas
  que ese módulo pudiera tener.

```mermaid
flowchart LR
    subgraph ACC["Acceso y cuenta"]
        AUTH["auth<br/>login()<br/>refresh()"]
        USU["usuario<br/>crear() · listar()<br/>registrarCliente()<br/>obtenerPorId()<br/>actualizar() · desactivar()"]
        BLO["usuario · bloqueo<br/>estaBloqueado()<br/>obtenerActivo()<br/>desbloquear()"]
    end

    subgraph COM["Comercial"]
        MEM["membresia<br/>crear() · listar()<br/>obtenerPorId()<br/>desactivar()"]
        SUS["suscripcion<br/>contratar()<br/>listarPropias()<br/>cancelar()"]
        PAG["pago<br/>crear()<br/>listarPropios() · obtener()<br/>confirmar() · rechazar()"]
    end

    subgraph INF["Infraestructura"]
        EST["estacion<br/>crear() · actualizar()<br/>geocodificar()<br/>listar() · obtenerPorId()<br/>desactivar()"]
        ANC["anclaje<br/>agregar()<br/>listarPorEstacion()<br/>marcarFueraServicio()<br/>habilitar()"]
        FLO["flota · bicicleta<br/>crear() · listar()<br/>obtenerPorId()<br/>cambiarEstadoEnAlmacen()"]
        ALM["almacen<br/>crear() · listar()<br/>obtenerPorId()"]
        LOG["logistica<br/>registrar()"]
        GEO["geocoding<br/>geocodificar()<br/>· Google Maps API ·"]
    end

    subgraph OPE["Operación"]
        VIA["viaje<br/>iniciar()<br/>finalizar()<br/>obtenerActual()<br/>listarPropios()"]
        STR["strike<br/>generarPorExceso()<br/>listarPropios()<br/>listarDeUsuario()"]
        NOT["notificacion<br/>crear()<br/>listarPropias()<br/>listarNoLeidas()<br/>marcarLeida()"]
        MON["monitoreo<br/>incrementar() · decrementar()<br/>obtenerActual()<br/>listarHistorial()<br/>guardarSnapshot()"]
    end

    %% --- llamadas servicio a servicio ---
    ANC ==>|"valida que la estación exista"| EST
    EST ==>|"geocodificar()"| GEO
    SUS ==>|"pagoService.crear()"| PAG
    VIA ==>|"generarPorExceso() si excedió"| STR
    VIA ==>|"incrementar() / decrementar()"| MON
    STR ==>|"pagoService.crear() tiempo extra"| PAG
    STR ==>|"notificacionService.crear()"| NOT

    %% --- lecturas de repositorio ajeno ---
    AUTH -.->|"UsuarioRepository"| USU
    SUS -.->|"Membresia + Usuario"| MEM
    PAG -.->|"activa la suscripción al confirmar"| SUS
    PAG -.->|"marca el strike como saldado"| STR
    STR -.->|"crea el bloqueo a los 3 strikes"| BLO
    VIA -.->|"exige plan vigente"| SUS
    VIA -.->|"exige cuenta no bloqueada"| BLO
    VIA -.->|"mueve el estado de la bici"| FLO
    VIA -.->|"libera / ocupa el anclaje"| ANC
    MON -.->|"cuenta viajes en curso"| VIA
    FLO -.->|"ubicación en depósito"| ALM
    LOG -.->|"traslada en lote"| FLO
    LOG -.->|"origen / destino"| ALM
    LOG -.->|"anclajes destino"| ANC
```

> **No hay ciclos.** Ningún módulo depende de otro que dependa de él, ni directa ni
> transitivamente. Por eso el sistema se puede leer de arriba hacia abajo:
> `viaje` orquesta, `strike` reacciona, `pago` y `notificacion` son terminales.

---

## 2. Los tres orquestadores

De los 13 módulos, **10 son CRUD**: reciben un request, validan y persisten. Los que
concentran la lógica del negocio son tres, y son los que hay que mirar primero:

| Módulo | Función que orquesta | Qué coordina |
|---|---|---|
| `viaje` | `finalizar()` | anclaje + bicicleta + monitoreo + strike |
| `strike` | `generarPorExceso()` | pago + notificación + bloqueo de cuenta |
| `pago` | `confirmar()` | activa suscripción **o** salda strike, según el concepto |

`PagoService.confirmar()` es el punto donde el dinero mueve el estado del sistema: el
mismo método activa una suscripción si el pago era de un plan, o da por saldado un
strike si era de tiempo extra.

---

## 3. Secuencia real: devolver la bici fuera de tiempo

El camino de excepción completo, con los nombres de método tal como están en el
código. Es la interacción más densa del sistema — toca seis módulos.

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant VC as ViajeController
    participant VS as ViajeService
    participant MS as UsuariosActivosService
    participant SS as StrikeService
    participant PS as PagoService
    participant NS as NotificacionService

    C->>VC: POST /api/viajes/finalizar
    VC->>VS: finalizar(usuarioId, request)

    VS->>VS: busca el viaje en curso
    VS->>VS: valida que el anclaje destino esté LIBRE
    VS->>VS: viaje.finalizar() · bici.anclarEn() · anclaje.ocupar()
    VS->>MS: decrementar()

    alt el viaje excedió el tiempo del plan
        VS->>SS: generarPorExceso(usuario, viaje)
        SS->>SS: crea el Strike
        SS->>SS: monto = minutos excedidos × tarifaMinutoExtra
        SS->>PS: crear(usuario, viaje, null, TIEMPO_EXTRA, monto)
        PS-->>SS: Pago PENDIENTE
        SS->>SS: strike.asociarPago(pago)
        SS->>NS: crear(usuario, viaje, TIEMPO_EXTRA, mensaje)
        SS->>SS: evaluarBloqueo(usuario)
        alt acumula 3 strikes activos
            SS->>SS: guarda BloqueoCuenta
            SS->>NS: crear(usuario, null, BLOQUEO, mensaje)
        end
    end

    VS-->>VC: ViajeResponse
    VC-->>C: 200 OK
```

El strike nace **activo**. Se salda cuando el cliente paga:
`PagoService.confirmar()` busca el strike asociado a ese pago y lo marca saldado. Si
llega a tres activos sin pagar, la cuenta queda bloqueada y solo un ADMIN la libera
con `BloqueoCuentaService.desbloquear()`.

---

## 4. Inventario completo de funciones

Las funciones públicas de cada servicio, que son la superficie real de cada módulo.

| Módulo | Servicio | Funciones |
|---|---|---|
| auth | `AuthService` | `login()` · `refresh()` |
| usuario | `UsuarioService` | `crear()` · `registrarCliente()` · `listar()` · `obtenerPorId()` · `actualizar()` · `desactivar()` |
| usuario | `BloqueoCuentaService` | `estaBloqueado()` · `obtenerActivo()` · `desbloquear()` |
| membresia | `MembresiaService` | `crear()` · `listar()` · `obtenerPorId()` · `desactivar()` |
| membresia | `SuscripcionUsuarioService` | `contratar()` · `listarPropias()` · `cancelar()` |
| pago | `PagoService` | `crear()` · `listarPropios()` · `obtener()` · `confirmar()` · `rechazar()` |
| estacion | `EstacionService` | `crear()` · `actualizar()` · `geocodificar()` · `listar()` · `obtenerPorId()` · `desactivar()` |
| estacion | `AnclajeService` | `agregar()` · `listarPorEstacion()` · `marcarFueraServicio()` · `habilitar()` |
| estacion | `GeocodingService` | `geocodificar()` — única salida a un servicio externo |
| flota | `BicicletaService` | `crear()` · `listar()` · `obtenerPorId()` · `cambiarEstadoEnAlmacen()` |
| almacen | `AlmacenService` | `crear()` · `listar()` · `obtenerPorId()` |
| logistica | `MovimientoBicisService` | `registrar()` — traslado en lote |
| viaje | `ViajeService` | `iniciar()` · `finalizar()` · `obtenerActual()` · `listarPropios()` |
| strike | `StrikeService` | `generarPorExceso()` · `listarPropios()` · `listarDeUsuario()` |
| notificacion | `NotificacionService` | `crear()` · `listarPropias()` · `listarNoLeidas()` · `marcarLeida()` |
| monitoreo | `UsuariosActivosService` | `inicializar()` · `incrementar()` · `decrementar()` · `obtenerActual()` · `listarHistorial()` · `guardarSnapshot()` |
| saludo | `SaludoService` | `saludar()` — componente de prueba, fuera del dominio |

---

## 5. Cómo se cruza la seguridad

La seguridad no es un módulo de dominio: es una capa que atraviesa a todos, en
`com.mediconecta.seguridad`.

```mermaid
flowchart TB
    REQ["Request HTTP"] --> RL["LoginRateLimitFilter<br/>solo POST /api/auth/login<br/>5 fallos ⇒ 429"]
    RL --> JWT["JwtAuthenticationFilter<br/>valida la firma<br/>y lee el usuario de la BASE"]
    JWT --> PRE["@PreAuthorize en el controller<br/>ADMIN / CLIENTE"]
    PRE --> OWN["El service valida la PROPIEDAD<br/>¿el recurso es de quien lo pide?"]
    OWN --> SVC["Lógica del módulo"]
```

Los cuatro controles son distintos y ninguno reemplaza a otro:

1. **Rate limit** — corta la fuerza bruta antes de tocar la base.
2. **Filtro JWT** — prueba *quién sos*. El rol y el estado salen de la base, no del
   token: degradar o dar de baja a alguien tiene efecto en el request siguiente.
3. **`@PreAuthorize`** — control por *rol*. Sirve para "esto es solo de ADMIN".
4. **Validación de propiedad en el service** — control por *dueño*. Es lo que impide
   que un cliente lea el pago de otro, y el rol no alcanza para eso: los dos son
   CLIENTE. Está en `PagoService`, `NotificacionService` y `SuscripcionUsuarioService`.
