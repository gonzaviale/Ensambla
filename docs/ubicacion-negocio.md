# Ubicación y rubro · ENS-209

## Primera entrega: búsqueda y revisión sin persistencia

La pantalla privada `/ubicacion` permite buscar una dirección comercial de Argentina, elegir una de hasta cinco
coincidencias, corregir su punto con Leaflet (arrastre, clic o coordenadas) y elegir un rubro del catálogo del backend.
`Revisar configuración` valida y muestra el borrador. No envía una escritura del negocio. Al salir de la pantalla,
recargar o cerrar sesión se descartan los datos. Mover el punto conserva la dirección como referencia, sin hacer
geocodificación inversa ni afirmar que la dirección textual cambió.

Se reutilizan `RubroNegocio` y los códigos incorporados por V9. Los nombres legibles salen del mismo catálogo,
`GET /api/ubicacion/rubros`, sin introducir una tabla ni migración duplicada.

`POST /api/ubicacion/direcciones` recibe `{ "consulta": "calle, altura, localidad" }` y devuelve
`{ "resultados": [{ "direccion": "...", "latitud": -34.67, "longitud": -58.56 }] }`.
Requiere sesión y CSRF. El body evita poner la dirección en el URL del navegador y de los logs de acceso de Ensambla;
el proveedor externo sí recibe la consulta. No se registran consultas ni respuestas del proveedor.

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
4. Elegir un resultado, mover el punto, seleccionar rubro y revisar; navegar a Mi zona y volver para comprobar descarte.

`VITE_API_MOCKS=completo` usa fixtures declaradas de San Justo y la pantalla lo indica. El modo parcial nunca
intercepta los dos endpoints de ubicación. Las pruebas no consultan Nominatim ni descargan mosaicos.

Verificación: `backend/mvnw.cmd clean verify`; en frontend `npm test`, `npm run build` y `npm run lint`.

## Alcance pendiente

Esto permite comprobar el flujo sin esperar la dependencia ENS-69. No completa la persistencia de ENS-197/ENS-218,
la carga de configuración existente ni el guardado de coordenadas, localidad, zona y opt-out. Tampoco conecta el
borrador a las comparaciones de Mi zona. ENS-202 (equivalencias confirmadas por presentación/unidad) y ENS-219
(selección por radio, rubro, exclusión del negocio propio y mínimo de tres aportantes) siguen requiriendo su entrega
de dominio e integración. No marcar ENS-209 y sus subtareas como terminadas por esta vista previa.
