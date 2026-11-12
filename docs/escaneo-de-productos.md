# Carga de productos por código en Inventario

La pantalla de Inventario consulta los catálogos mediante el backend autenticado de Ensambla.
Funciona con el frontend compilado y el despliegue de Docker/Caddy; no depende de adaptadores de Vite.
La búsqueda no modifica el stock. La persona revisa los datos y confirma la carga.

## Ejecutar la aplicación real

Requisitos para desarrollo: Java 21, Docker y Node.js 24. No se necesitan claves de SEPA ni Open Food Facts.
Para ejecutar todo con imágenes, alcanza con `docker compose up --build -d --wait` desde la raíz;
el frontend estático queda en <http://localhost:8088> y usa el backend y PostgreSQL reales.

Desde `backend/`:

```sh
docker compose up -d postgres
./mvnw spring-boot:run
```

En Windows se puede usar `mvnw.cmd`. Desde `frontend/`, en otra terminal:

```sh
npm ci
npm run dev
```

La configuración normal usa `VITE_API_MOCKS=ninguno` y `VITE_API_URL=/api`; las llamadas pasan al backend
por el proxy habitual. Registrar una cuenta o ingresar con una cuenta real de esa base, abrir un cuaderno
y tocar **Escanear productos** en una tabla de Inventario, también disponible a pantalla completa.
Las credenciales de la demo no crean una cuenta en PostgreSQL.

### Probar desde el celular con Docker

Con el entorno real de Docker ya iniciado, ejecutar desde la raíz:

```sh
docker compose --profile movil up -d tunel-movil
docker compose logs tunel-movil
```

Abrir en Chrome de Android o Safari de iPhone el enlace `https://…trycloudflare.com` que aparece
en los registros. Ingresar con la misma cuenta de la PC, abrir el cuaderno y tocar **Escanear productos**
en Inventario. El navegador del teléfono pide su propio permiso de cámara.

El enlace sirve el frontend compilado, el backend y PostgreSQL de ese entorno. Comparte cuentas y
registros con <http://localhost:8088>; no usa datos simulados. El acceso móvil agrega cookies `Secure`
y redirige HTTP a HTTPS. No requiere aceptar un certificado local ni abrir puertos del router.
La sesión del teléfono es independiente; al ingresar aparecen los datos guardados de la cuenta.
IA y WhatsApp mantienen la configuración del entorno: este acceso no agrega sus credenciales.

El enlace temporal es público: quien lo tenga puede llegar a la pantalla de acceso, y los datos
del negocio siguen requiriendo sesión. Funciona mientras la PC, Docker y el túnel estén encendidos;
no hace falta compartir Wi-Fi. Para cerrar sólo este acceso:

```sh
docker compose --profile movil stop tunel-movil acceso-movil
```

Para obtener un enlace nuevo, recrear el túnel y consultar nuevamente sus registros:

```sh
docker compose --profile movil up -d --force-recreate tunel-movil
docker compose logs tunel-movil
```

### Probar desde el celular con Vite

Para desarrollo fuera de Docker, en la misma red:

```sh
npm run dev:movil
```

Abrir la URL HTTPS del puerto 5174 que imprime Vite. El dispositivo debe aceptar el certificado local.
HTTP y HTTPS tienen sesiones y almacenamiento separados. Android/Chrome e iPhone/Safari necesitan
un contexto seguro y permiso de cámara; el selector permite cambiar de lente.

`npm run dev:demo` y `npm run dev:demo:movil` mantienen el modo completamente simulado. En ese modo el
catálogo de prueba devuelve ausencia y permite ensayar la carga manual; no valida la integración real.

## Catálogos en el backend

`GET /api/catalogos/{fuente}/{codigo}` requiere sesión, admite `sepa` y `openfoodfacts`, y devuelve
`{"producto":null}` si el código no existe. Un GTIN o fuente inválidos devuelven 400; una caída o límite
del proveedor devuelve 503, distinguible de una ausencia. Sin sesión responde 401.

Las fichas se normalizan a `codigo`, `nombre`, `marca`, `presentacion` e `imagen` opcional. El servidor
valida la identidad del código recibido y sólo admite imágenes HTTPS de los dominios de imágenes de
Open Food Facts y sus proyectos asociados. El frontend usa el cliente HTTP central, respeta
`VITE_API_URL` y valida la respuesta con Zod. Sólo el servidor habla con el proveedor público, sin reenviar
cookies, credenciales, referencias de navegación ni datos del negocio.

### SEPA incluido

`backend/src/main/resources/catalogos/sepa.json.gz` incluye **77.038 códigos del 10/11/2026** en unos 2 MB.
Se empaqueta automáticamente en el JAR y la imagen del backend. No hace falta descargar, copiar ni
configurar un archivo adicional para que otro integrante lo use.

El índice contiene identificación pública de productos, fecha, atribución, URL de origen y SHA-256 del
ZIP oficial. Se validaron el dígito de control y los GTIN equivalentes; se conservaron los ceros iniciales.
Se excluyen precios de supermercados, datos de comercios y sucursales. No representa todos los
productos disponibles en Argentina ni garantiza la exactitud de cada descripción.

Para actualizarlo, desde `frontend/`:

```sh
npm run catalogo:sepa:descargar
```

El comando consulta el recurso oficial más reciente y procesa sus ZIP anidados y `productos.csv`.
El código real es `id_producto`; `productos_ean` es un indicador. La descarga pesa cientos de MB y puede
tardar varios minutos. Genera `frontend/.local/catalogo-sepa.json` sin subir el ZIP ni reemplazar el
índice anterior si falla. También admite un ZIP ya descargado:

```sh
npm run catalogo:sepa -- "ruta/sepa.zip" AAAA-MM-DD
```

Para usar una actualización externa, configurar en el backend `ENSAMBLA_SEPA_RECURSO=file:/ruta/catalogo-sepa.json`
y reiniciar. Admite JSON o JSON comprimido con gzip. En Docker, montar el archivo en el volumen de datos
(por ejemplo `/app/datos/catalogos/sepa.json`) y configurar esa ruta. Un recurso inválido impide arrancar
con un índice parcial. El índice incluido sigue disponible cuando no se configura un recurso externo.

### Open Food Facts por API

El backend consulta la API v3 por código y sólo solicita los campos requeridos. Tiene un plazo de cinco
segundos, una caché compartida por instancia de 15 minutos y hasta 1.000 fichas, incluidas las ausencias.
Los fallos no se cachean. Limita a 12 consultas externas nuevas por minuto por instancia; SEPA sigue
funcionando si Open Food Facts no está disponible. No se descarga ni redistribuye una base combinada.

Configuración opcional centralizada en `application.properties`:

| Variable | Valor inicial |
|---|---|
| `ENSAMBLA_SEPA_RECURSO` | `classpath:catalogos/sepa.json.gz` |
| `ENSAMBLA_OFF_URL` | `https://world.openfoodfacts.org` |
| `ENSAMBLA_OFF_USER_AGENT` | Identificación de Ensambla y repositorio |
| `ENSAMBLA_OFF_TIMEOUT` | `5s` |
| `ENSAMBLA_OFF_CACHE` | `15m` |
| `ENSAMBLA_OFF_LIMITE` | `12` |

El límite es por proceso. Si se escala a varias réplicas con una misma IP de salida, corresponde coordinar
el presupuesto de consultas entre instancias o reducir el límite de cada una.

## Revisión y guardado

1. Leer con cámara, lector USB/Bluetooth que envía Enter o ingreso manual.
2. Si ya existe en el inventario elegido, se muestra su foto de referencia si está disponible, la descripción
   propia y **¿Querés sumar al stock?**. Ingresar las unidades a agregar, revisar el total previsto y confirmar
   **Agregar al stock**, o **Descartar carga**. La suma se calcula sobre las existencias vigentes de la pestaña;
   conserva precio, costo y otros campos. La consulta opcional de la foto no bloquea esta operación.
3. Para productos nuevos se consulta cada fuente y se sugiere la descripción más informativa, con marca,
   presentación y foto si existe. **Ver origen de los datos** muestra la procedencia. Todo es revisable.
4. Completar los campos reales de la tabla: conteo, costo, precio, mínimo y campos personalizados.
5. Confirmar. El guardado usa la sincronización autenticada existente de sistemas, con transacción y
   aislamiento por negocio. Al recargar, los datos se leen de PostgreSQL.

Durante la revisión, el lector y el ingreso manual se ocultan. **Escanear otro producto** al principio
permite volver a la lectura, con confirmación antes de descartar una carga pendiente. **Descartar carga**
queda como una acción visible al final. Tras guardar o descartar, vuelve a aparecer el lector.
Leer un código no altera por sí solo las unidades ni crea duplicados; la reposición requiere una cantidad
positiva y una confirmación explícita.

Se puede vincular un producto sin código, descartar o completar manualmente una ficha ausente.
La carga local conserva los campos no editados y advierte cambios concurrentes en un mismo campo.
El backend vuelve a validar el GTIN, el campo de texto y los datos del registro codificado, y rechaza
códigos equivalentes duplicados dentro del inventario enviado. Cada tabla tiene su propio inventario.
No se crean ventas, movimientos, cobros ni tickets. Se conserva el mecanismo existente de sincronización
de documentos; no se incorpora edición colaborativa en tiempo real.

## Cámara y pruebas

ZXing se carga al abrir el lector y admite EAN-8, EAN-13, UPC-A e ITF/GTIN-14. La validación no certifica
una asignación GS1. Se recuerda la cámara elegida manualmente y luego se prioriza la trasera principal.
Se liberan las pistas al leer, cerrar, ocultar la página o desmontar. No se guardan fotos ni videos.

```sh
# backend, con Docker y Java 21
./mvnw clean verify
# frontend
npm run lint
npm run typecheck
npm test
npm run build
```

Las pruebas cubren catálogos, caché, límites, errores, seguridad HTTP, guardado y recarga con PostgreSQL
real mediante Testcontainers, validación del servidor, duplicados, cancelación y ciclo de cámara.
La lectura y selección de lentes en dispositivos físicos Android e iPhone requiere comprobación manual.

## Atribución

- [SEPA / Secretaría de Comercio](https://datos.produccion.gob.ar/dataset/sepa-precios): CC BY 4.0.
  El índice incluido es una extracción transformada y deduplicada de sus identificadores de productos.
- [Open Food Facts y asociados](https://openfoodfacts.github.io/openfoodfacts-server/api/): base ODbL,
  contenido DbCL y fotos CC BY-SA, con posibles derechos de terceros. Las fuentes se mantienen separadas.
