# Configuración, ubicación y rubro · ENS-69 / ENS-209

## Flujo integrado con persistencia

La pantalla privada `/ubicacion` permite buscar una dirección comercial de Argentina, elegir una de hasta cinco
coincidencias, corregir su punto con Leaflet (arrastre, clic o coordenadas) y elegir un rubro del catálogo del backend.
`Revisar configuración` valida y muestra el borrador. `Guardar configuración` confirma nombre del comercio,
dirección, localidad, coordenadas, rubro y participación en datos agregados. Al volver a entrar se consulta lo guardado
en PostgreSQL. Los cambios sin confirmar se descartan al salir. Mover el punto conserva la dirección como referencia,
sin hacer geocodificación inversa ni afirmar que la dirección textual cambió. La localidad se confirma explícitamente.
El centro de avisos invita a completar la configuración cuando el negocio todavía no tiene ubicación.

Se reutilizan `RubroNegocio` y los códigos incorporados por V9. Los nombres legibles salen del mismo catálogo,
`GET /api/ubicacion/rubros`, sin introducir una tabla ni migración duplicada.

`POST /api/ubicacion/direcciones` recibe `{ "consulta": "calle, altura, localidad" }` y devuelve
`{ "resultados": [{ "direccion": "...", "latitud": -34.67, "longitud": -58.56 }] }`.
Requiere sesión y CSRF. El body evita poner la dirección en el URL del navegador y de los logs de acceso de Ensambla;
el proveedor externo sí recibe la consulta. No se registran consultas ni respuestas del proveedor.

## Contrato de configuración del negocio (ENS-69)

- `GET /api/business`: configuración del negocio del dueño autenticado.
- `PATCH /api/business`: edición parcial de ese mismo negocio, con sesión y CSRF. La ruta conserva el contrato
  `/business` de T-041 dentro del prefijo `/api` del proyecto.
- Los dos responden con `Cache-Control: no-store`. No aceptan un negocio objetivo ni un dueño desde el cliente.
  La selección respeta el criterio actual del proyecto: primer negocio del dueño por fecha de creación e id.
- Los campos omitidos o `null` conservan su valor. Si se envía `ubicacion`, sus cuatro campos son obligatorios.
  No se implementa borrar una ubicación con `null`. Un rubro desconocido o datos inválidos responden 400.
- El PATCH bloquea la fila del negocio dentro de la transacción para evitar perder campos en ediciones parciales
  concurrentes. La fecha sale del Clock del caso de uso. Las coordenadas se redondean a seis decimales, como V9.

Ejemplo de cuerpo de PATCH y de respuesta (GET/PATCH):

```json
{
  "nombre": "Mi almacén",
  "rubro": "ALMACEN",
  "ubicacion": {
    "direccion": "Florencio Varela 1903, San Justo",
    "localidad": "San Justo",
    "latitud": -34.670123,
    "longitud": -58.560789
  },
  "comparteDatosZona": false,
  "zona": "San Justo - UNLaM"
}
```

Un negocio sin coordenadas devuelve `ubicacion: null`. `zona` es sólo el nombre de la zona asignada (o `null`);
no se devuelven dueño, id de zona ni otros comercios (el PATCH ignora `zona` si el cliente la envía).
Cambiar sólo el nombre, rubro o privacidad conserva la ubicación y la zona.

### Asignación de la zona

Al guardar, `NegocioService` completa `negocio.zona_id` cuando el negocio tiene punto y no tiene zona, que pasa si
el punto cambió (se invalida la asignación anterior) o si nunca se resolvió. `ZonaService.resolver` busca la zona
cuyo radio (`radio_metros`, distancia haversine al centro) contiene el punto y, si hay varias, elige la de centro más
cercano. Si ninguna lo contiene, `zona_id` queda en `null`: las zonas son datos precargados por migración, no se crean
a demanda. En ese caso la pantalla avisa que todavía no hay una zona que cubra la ubicación.
Desactivar `comparteDatosZona` persiste el opt-out; no recalcula los agregados estáticos de la migración de demo V10.

## Proveedor y límites

Antes de desplegar, leer la [política de Nominatim](https://operations.osmfoundation.org/policies/nominatim/) y la
[política de mosaicos OSM](https://operations.osmfoundation.org/policies/tiles/).

- La búsqueda es explícita por botón: no hay autocompletado, búsquedas periódicas ni barridos.
- Nominatim se llama sólo desde el backend, con `User-Agent` propio, timeout de 5 s, `countrycodes=ar` y límite 5.
- La instancia serializa llamadas y permite como máximo una salida cada 1,1 s. Una consulta nueva dentro del
  intervalo responde 503 y permite reintentar. La caché de hasta 256 consultas dura 24 h, incluyendo resultados vacíos.
- La limitación y la caché son **por instancia**. Si se despliegan varias, deben compartir un gateway/limitador antes
  de usar el servidor público: la cuota pública se aplica a toda la aplicación. No hay SLA del proveedor público.
- `ENSAMBLA_GEOCODIFICACION_URL`, `ENSAMBLA_GEOCODIFICACION_USER_AGENT` y
  `ENSAMBLA_GEOCODIFICACION_TIMEOUT` permiten cambiar el proveedor compatible y su identificación sin tocar la UI.
  En producción configurar un `User-Agent` con contacto del responsable de la aplicación.
- La dirección comercial se transmite a Nominatim. La pantalla lo informa antes de enviar; no ingresar datos personales.
- Los mosaicos se cargan para la vista interactiva, sin precarga ni descargas masivas, conservando atribución y caché
  del navegador. `VITE_MAPA_TILES_URL` y `VITE_MAPA_ATRIBUCION` permiten cambiar el proveedor de mosaicos.

## Ejecutar y probar

1. Backend con PostgreSQL: instrucciones de `backend/README.md`.
2. Frontend: `VITE_API_MOCKS=parcial npm run dev` (en PowerShell, asignar primero `$env:VITE_API_MOCKS='parcial'`).
   El login, el catálogo, la búsqueda de direcciones, la lectura de fotos y los avisos usan el backend real.
   Sistemas, preferencias y comparaciones de zona conservan los mocks que ya tenía el proyecto.
3. Abrir `/ubicacion`, iniciar sesión y buscar, por ejemplo, `Florencio Varela 1903, San Justo`.
4. Elegir un resultado, mover el punto, completar nombre y localidad, seleccionar rubro y revisar.
5. Guardar; salir, volver y recargar para comprobar persistencia. Cambiar un dato sin guardar y volver para
   comprobar que se recupera la última configuración confirmada. Probar también un fallo de carga o guardado.

`VITE_API_MOCKS=completo` usa fixtures declaradas de San Justo y guardado simulado por cuenta en este navegador;
la pantalla lo indica. El modo parcial nunca intercepta ubicación ni `/business`: carga y guardado son reales.
Las pruebas usan PostgreSQL de Testcontainers o MSW; no consultan Nominatim ni descargan mosaicos.

Verificación: `backend/mvnw.cmd clean verify`; en frontend `npm test`, `npm run build` y `npm run lint`.

## Alcance pendiente

Esta entrega incorpora ENS-69 y el guardado/recuperación de la configuración de ENS-197/ENS-217/ENS-218.
No conecta la configuración a las comparaciones de Mi zona, que siguen siendo una demostración con mocks.
ENS-202 (equivalencias confirmadas por presentación/unidad) y ENS-219
(selección por radio, rubro, exclusión del negocio propio y mínimo de tres aportantes) siguen requiriendo su entrega
de dominio e integración. No marcar ENS-209 completa por este flujo de configuración.
