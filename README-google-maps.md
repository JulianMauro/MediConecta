# Integración de Google Maps API — Estaciones de Bicicletas

Guía para geocodificar direcciones automáticamente (dirección → lat/lng) y mostrar las estaciones en un mapa.

## 1. Conseguir la API Key

1. Ir a [Google Cloud Console](https://console.cloud.google.com/)
2. Crear un proyecto (o usar uno existente)
3. Activar estas APIs:
   - **Maps JavaScript API** (para pintar el mapa en el front)
   - **Geocoding API** (para convertir dirección → lat/lng en el back)
4. En "Credenciales" generar **dos** API keys:
   - **Key de backend**: sin restricción de dominio, restringida solo a Geocoding API
   - **Key de frontend**: restringida por HTTP referrer (tu dominio), solo Maps JavaScript API

> Google pide tarjeta de crédito pero da crédito gratuito mensual.

## 2. Backend (Spring Boot)

### Modelo

```java
@Entity
public class Estacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String direccion;
    private Double lat;
    private Double lng;

    // getters y setters
}
```

### Repository

```java
public interface EstacionRepository extends JpaRepository<Estacion, Long> {
}
```

### Servicio de Geocoding

```java
@Service
public class GeocodingService {

    @Value("${google.maps.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public double[] geocodificar(String direccion) {
        String url = UriComponentsBuilder
            .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
            .queryParam("address", direccion)
            .queryParam("key", apiKey)
            .toUriString();

        Map response = restTemplate.getForObject(url, Map.class);
        List results = (List) response.get("results");
        Map location = (Map) ((Map) ((Map) results.get(0)).get("geometry")).get("location");

        double lat = (double) location.get("lat");
        double lng = (double) location.get("lng");
        return new double[]{lat, lng};
    }
}
```

`application.properties`:
```properties
google.maps.api.key=TU_API_KEY_BACKEND
```

### Controller

```java
@RestController
@RequestMapping("/api/estaciones")
public class EstacionController {

    @Autowired private EstacionRepository repo;
    @Autowired private GeocodingService geocodingService;

    @PostMapping
    public Estacion crear(@RequestBody Estacion estacion) {
        double[] coords = geocodingService.geocodificar(estacion.getDireccion());
        estacion.setLat(coords[0]);
        estacion.setLng(coords[1]);
        return repo.save(estacion);
    }

    @GetMapping
    public List<Estacion> listar() {
        return repo.findAll();
    }
}
```

Al hacer `POST /api/estaciones` con:
```json
{ "nombre": "Estación Centro", "direccion": "Av. Corrientes 1234, CABA, Argentina" }
```
el backend geocodifica solo y guarda `lat`/`lng` automáticamente.

## 3. Frontend

```html
<div id="map" style="height: 500px;"></div>
<script src="https://maps.googleapis.com/maps/api/js?key=TU_API_KEY_FRONTEND"></script>
<script>
async function initMap() {
  const map = new google.maps.Map(document.getElementById("map"), {
    center: { lat: -34.6, lng: -58.38 },
    zoom: 12,
  });

  const estaciones = await fetch('/api/estaciones').then(r => r.json());

  estaciones.forEach(e => {
    new google.maps.Marker({
      position: { lat: e.lat, lng: e.lng },
      map,
      title: e.nombre,
    });
  });
}
initMap();
</script>
```

## 4. Probar la Geocoding API sin código

Pegar en el navegador (reemplazando la key):

```
https://maps.googleapis.com/maps/api/geocode/json?address=Av.+Corrientes+1234,+CABA,+Argentina&key=TU_API_KEY
```

Devuelve un JSON con `lat`/`lng` de esa dirección.

## Notas importantes (Argentina)

- Siempre agregar ciudad + país a la dirección (`", CABA, Argentina"`), sino Google puede confundir la calle con una de otro lugar.
- Geocodificar **una sola vez** al crear la estación, no en cada request — guardar lat/lng en la base.
- Nunca exponer la key de backend en el frontend.

## Flujo resumido

1. Guardás estación con dirección → back geocodifica → guarda lat/lng en la base
2. Front pide `GET /api/estaciones` → recibe lat/lng
3. Front pinta un marker por cada estación en el mapa
