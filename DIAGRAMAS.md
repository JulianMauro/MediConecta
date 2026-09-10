# PeopleBikes — Diagramas del sistema

Sistema de transporte público en bicicletas compartidas. El cliente retira una
bicicleta de una estación y la devuelve en cualquier otra, dentro de los límites
del plan que contrató.

- **Alcance funcional completo:** [ALCANCE.md](ALCANCE.md) — 59 funciones (F-01…F-59) en 11 módulos.
- **Patrones de arquitectura:** [patrones.md](patrones.md).
- **Stack:** Spring Boot 4 + Spring Data JPA + PostgreSQL en el backend · React 19 + TypeScript + Vite 8 + React Router 7 en el frontend.
- El nombre heredado *MediConecta* sobrevive solo en el package Java (`com.mediconecta.*`); el producto es **PeopleBikes**.

> Los bloques ```` ```mermaid ```` se renderizan en GitHub, VS Code y la mayoría de los visores de Markdown. Para exportarlos a imagen: <https://mermaid.live>.

---

## 1. Arquitectura

Vista de capas: qué corre dónde y contra qué habla.

```mermaid
flowchart TB
    subgraph FE["Frontend — React 19 · TypeScript · Vite 8"]
        R["SPA · React Router 7<br/>login · registro · estaciones · mapa<br/>planes · mi cuenta · panel admin"]
        TOK["Sesión JWT<br/>access token + refresh automático"]
    end

    subgraph BE["Backend — Spring Boot 4 · Spring Data JPA"]
        JWT["Filtro JWT + @PreAuthorize<br/>roles ADMIN / CLIENTE"]
        DOM["11 módulos de dominio<br/>cada uno: entity → repository → service → controller"]
        SEED["Seeder de admin inicial"]
    end

    PG[("PostgreSQL<br/>H2 en memoria para el perfil de test")]
    GM(["Google Maps · Geocoding API"])

    R -->|"HTTP · JSON · Bearer token"| JWT
    TOK -.->|adjunta el token| R
    JWT --> DOM
    DOM --> PG
    DOM -->|"dirección → lat/lng<br/>una sola vez, al crear la estación"| GM
```

**Decisiones que se ven acá:**

- El frontend nunca habla con la base: todo pasa por el backend, que valida rol y reglas.
- El QR de desbloqueo **no toca el backend** — el escáner lee un string que ya es el código de la bici y usa el mismo endpoint que el tipeo manual.
- Google Maps es opcional: sin API key la estación se guarda sin coordenadas y el mapa degrada con un aviso.

---

## 2. Los 11 módulos y su orquestación

Cada módulo tiene las cinco capas completas. Las flechas son dependencias reales
entre servicios: **no hay ciclos** — ningún módulo depende de otro que dependa de él.

```mermaid
flowchart LR
    subgraph ACC["1 · Cuenta y acceso"]
        AUTH["auth<br/>registro · login · JWT"]
        USU["usuario<br/>+ bloqueo de cuenta"]
    end
    subgraph COM["2-4 · Comercial"]
        MEM["membresia<br/>+ suscripciones"]
        PAG["pago"]
    end
    subgraph INF["5-8 · Infraestructura"]
        EST["estacion<br/>+ anclajes"]
        FLO["flota"]
        ALM["almacen"]
        LOG["logistica<br/>traslados en lote"]
    end
    subgraph OPE["9-11 · Operación"]
        VIA["viaje — orquesta el ciclo del viaje"]
        STR["strike — orquesta el exceso de tiempo"]
        NOT["notificacion"]
    end
    MON["monitoreo · usuarios activos (auxiliar)"]

    AUTH --> USU
    MEM -->|"pago pendiente al contratar"| PAG
    VIA -->|"exige plan vigente"| MEM
    VIA -->|"exige cuenta no bloqueada"| USU
    VIA -->|"mueve el estado de la bici"| FLO
    VIA -->|"libera / ocupa anclajes"| EST
    VIA -->|"si el viaje excedió el tiempo"| STR
    STR -->|"cobro por minuto extra"| PAG
    STR -->|"avisa el exceso"| NOT
    STR -->|"3 strikes sin saldar ⇒ bloquea"| USU
    USU -->|"avisa el bloqueo"| NOT
    LOG --> FLO
    LOG --> EST
    LOG --> ALM
    VIA --> MON
```

---

## 3. Modelo de dominio — núcleo (cliente, plan, viaje, penalización)

```mermaid
erDiagram
    USUARIO ||--o{ SUSCRIPCION_USUARIO : contrata
    USUARIO ||--o{ VIAJE : realiza
    USUARIO ||--o{ PAGO : "es titular de"
    USUARIO ||--o{ STRIKE : acumula
    USUARIO ||--o{ NOTIFICACION : recibe
    USUARIO ||--o{ BLOQUEO_CUENTA : "queda registrado en"
    MEMBRESIA ||--o{ SUSCRIPCION_USUARIO : "es la plantilla de"
    SUSCRIPCION_USUARIO ||--o{ VIAJE : habilita
    VIAJE ||--o| STRIKE : "puede generar"
    STRIKE ||--o| PAGO : "emite (TIEMPO_EXTRA)"
    MEMBRESIA ||--o{ PAGO : "cobra (MEMBRESIA)"
    VIAJE ||--o{ NOTIFICACION : origina

    USUARIO {
        bigint id PK
        string email UK
        string dni
        enum rol "ADMIN | CLIENTE"
        boolean activo "baja lógica, no se borra"
    }
    MEMBRESIA {
        bigint id PK
        enum tipo "INDIVIDUAL | SEMANAL | MENSUAL"
        decimal precio
        int tiempoPermitidoMinutos "incluidos por viaje"
        int viajesPorDia
        int tiempoEsperaMinutos "entre viajes"
        decimal tarifaMinutoExtra
        boolean activa
    }
    SUSCRIPCION_USUARIO {
        bigint id PK
        datetime fechaInicio
        datetime fechaFin
        enum estado "ACTIVA | VENCIDA | CANCELADA"
    }
    VIAJE {
        bigint id PK
        datetime fechaInicio
        datetime fechaFin "null mientras está en curso"
        boolean excedioTiempo
    }
    PAGO {
        bigint id PK
        decimal monto
        enum concepto "MEMBRESIA | TIEMPO_EXTRA"
        enum estado "PENDIENTE | PAGADO | RECHAZADO"
        string referenciaPasarela
    }
    STRIKE {
        bigint id PK
        datetime fechaGeneracion
        enum estado "ACTIVO | SALDADO"
    }
    BLOQUEO_CUENTA {
        bigint id PK
        datetime fechaBloqueo
        string motivo
        boolean activo
        datetime fechaDesbloqueo
    }
    NOTIFICACION {
        bigint id PK
        enum tipo "TIEMPO_EXTRA | STRIKE | BLOQUEO"
        string mensaje
        boolean leida
    }
```

---

## 4. Modelo de dominio — infraestructura (estaciones, flota, logística)

```mermaid
erDiagram
    ESTACION ||--o{ ANCLAJE : contiene
    ANCLAJE ||--o| BICICLETA : "aloja 0 o 1"
    ALMACEN ||--o{ BICICLETA : guarda
    ANCLAJE ||--o{ VIAJE : "es el origen de"
    ANCLAJE |o--o{ VIAJE : "es el destino de"
    BICICLETA ||--o{ VIAJE : "se usa en"
    MOVIMIENTO_BICIS ||--|{ MOVIMIENTO_BICI_ITEM : agrupa
    BICICLETA ||--o{ MOVIMIENTO_BICI_ITEM : "trasladada en"
    USUARIO ||--o{ MOVIMIENTO_BICIS : "ejecuta (admin)"
    ESTACION ||--o{ MOVIMIENTO_BICIS : "origen / destino"
    ALMACEN ||--o{ MOVIMIENTO_BICIS : "origen / destino"

    ESTACION {
        bigint id PK
        string nombre
        string direccion
        decimal latitud "geocodificada"
        decimal longitud
        int capacidad "tope de anclajes"
        boolean activa
    }
    ANCLAJE {
        bigint id PK
        int numero "único dentro de la estación"
        enum estado "LIBRE | OCUPADO | FUERA_SERVICIO"
    }
    BICICLETA {
        bigint id PK
        string codigo UK "grabado en la bici · va en el QR"
        enum estado "DISPONIBLE | EN_VIAJE | EN_REPARACION | DESACTIVADA"
    }
    ALMACEN {
        bigint id PK
        string nombre
        string direccion
        int capacidad
    }
    MOVIMIENTO_BICIS {
        bigint id PK
        datetime fecha
    }
    MOVIMIENTO_BICI_ITEM {
        bigint id PK
    }
```

**Regla 6 — ubicación de la bicicleta:** cada bicicleta apunta a un anclaje *o* a
un almacén (dos FK nullable). Durante un viaje no está en ninguno; en todo otro
momento, en exactamente uno. El invariante lo garantiza la capa de servicio.

---

## 5. Flujo principal del cliente (camino feliz)

Contratar → pagar → retirar → devolver.

```mermaid
sequenceDiagram
    actor C as Cliente
    participant MEM as membresia
    participant PAG as pago
    participant VIA as viaje
    participant FLO as flota
    participant EST as estacion y anclaje

    C->>MEM: Contratar plan (F-13)
    MEM->>PAG: Pago PENDIENTE · concepto MEMBRESIA (F-18)
    C->>PAG: Confirmar pago (F-21)
    PAG->>MEM: Suscripción ACTIVA · vigencia según el tipo (F-14)

    C->>VIA: Desbloquear BICI-001 (F-41)
    VIA->>VIA: Validar plan vigente · viajes del día ·<br/>espera entre viajes · cuenta no bloqueada (F-42)
    VIA->>FLO: Bicicleta → EN_VIAJE (F-33)
    VIA->>EST: Anclaje de origen → LIBRE (F-46)
    VIA-->>C: Viaje en curso · tiempo transcurrido (F-43)

    C->>VIA: Devolver en un anclaje libre (F-44)
    VIA->>VIA: Calcular duración · ¿excedió el tiempo? (F-45)
    VIA->>FLO: Bicicleta → DISPONIBLE
    VIA->>EST: Anclaje de destino → OCUPADO (F-46)
    VIA-->>C: Viaje finalizado · queda en el historial (F-47)
```

---

## 6. Flujo de excepción — exceso de tiempo, penalización y bloqueo

```mermaid
sequenceDiagram
    participant VIA as viaje
    participant STR as strike
    participant PAG as pago
    participant NOT as notificacion
    participant USU as usuario

    VIA->>VIA: Al finalizar · la duración supera los minutos del plan (F-45)
    VIA->>STR: Generar penalización (F-49)
    STR->>PAG: Pago PENDIENTE · minutos extra × tarifa del plan (F-50)
    STR->>NOT: Notificar el exceso de tiempo (F-56)

    Note over STR,USU: Evaluar los strikes ACTIVO del usuario
    alt Llegó a 3 strikes sin saldar
        STR->>USU: Bloquear la cuenta · registrar motivo y fecha (F-52, F-55)
        USU->>NOT: Notificar el bloqueo (F-56)
    end

    Note over PAG,STR: Más tarde, cuando el cliente paga
    USU->>PAG: Confirmar el pago de la penalización (F-21)
    PAG->>STR: Strike → SALDADO (F-51)
```

---

## 7. Ciclo de vida de la bicicleta

```mermaid
stateDiagram-v2
    [*] --> DESACTIVADA : alta en un almacén (F-31)
    DESACTIVADA --> DISPONIBLE : traslado a un anclaje (F-38)
    DISPONIBLE --> EN_VIAJE : desbloqueo (F-41)
    EN_VIAJE --> DISPONIBLE : devolución en un anclaje (F-44)
    DISPONIBLE --> EN_REPARACION : lo marca un admin (F-33)
    EN_REPARACION --> DISPONIBLE : lo repone un admin
    DISPONIBLE --> DESACTIVADA : baja lógica (F-33)

    note right of EN_VIAJE
        Fuera de todo anclaje y almacén.
        En cualquier otro estado: en exactamente uno.
    end note
```

---

## 8. Ciclo de vida de la suscripción

```mermaid
stateDiagram-v2
    [*] --> PagoPendiente : contratar un plan (F-13)
    PagoPendiente --> ACTIVA : se confirma el pago (F-14)
    PagoPendiente --> Descartada : se rechaza el pago (F-22)
    ACTIVA --> VENCIDA : llega la fecha de fin
    ACTIVA --> CANCELADA : la cancela el cliente (F-17)
    Descartada --> [*]
    VENCIDA --> [*]
    CANCELADA --> [*]
```

---

## 9. Ciclo de vida de la cuenta

```mermaid
stateDiagram-v2
    [*] --> Activa : registro (F-01)
    Activa --> Activa : 1.er y 2.º strike
    Activa --> Bloqueada : 3.er strike sin saldar (F-52)
    Bloqueada --> Activa : se saldan los strikes / un admin desbloquea

    note right of Bloqueada
        No puede iniciar viajes (F-42).
        Cada bloqueo queda como historial auditable (F-55).
    end note
```

---

## Reglas de negocio transversales

1. **Sin plan vigente no hay viaje.** El plan define minutos incluidos, viajes por día y espera mínima entre viajes.
2. **Un viaje a la vez por usuario.**
3. **Nada se borra.** Usuarios, estaciones, planes y bicicletas se dan de baja lógica.
4. **Toda penalización tiene un pago asociado**, y saldarla exige confirmarlo.
5. **Tres penalizaciones sin saldar bloquean la cuenta.**
6. **Una bicicleta está en un anclaje o en un almacén**, nunca en ambos ni en ninguno (salvo durante el viaje).
7. **Cada cambio de estado relevante deja historial**, no un simple indicador.
