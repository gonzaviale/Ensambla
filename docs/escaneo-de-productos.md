# Carga de productos por código en Inventario

Permite identificar un producto, revisar sus datos y confirmar su carga en una tabla de Inventario.
Se abre desde **Escanear productos** en el bloque o en la tabla a pantalla completa. El destino queda
identificado en `/sistemas/$sistemaId?escanear=$inventarioId`, incluso si hay varios inventarios.

## Ejecutar en local

Con Node.js 22, desde `frontend/`:

```sh
npm ci
npm run dev:demo
```

Abrir <http://127.0.0.1:5173/cuadernos>. La demo admite `demo@ensambla.local` / `EnsamblaDemo2026!`.
Abrir un cuaderno y su tabla Inventario. La demo usa los mocks existentes y guarda en el navegador.
Para trabajar con el backend configurado, ejecutar `npm run dev` en lugar de `dev:demo`.

Para probar desde Android (Chrome) o iPhone (Safari) en la misma red Wi-Fi:

```sh
npm run dev:demo:movil
```

Vite muestra la URL HTTPS de la red en el puerto 5174. La cámara del navegador requiere un contexto
seguro: en el teléfono debe usarse esa URL con un certificado aceptado por el dispositivo. HTTP y HTTPS
tienen almacenamiento y cachés separados.

## Lista SEPA descargada

La rama incluye `frontend/datos/catalogos/sepa.json.gz`: un índice comprimido de **77.038 códigos**
del relevamiento del 10/11/2026. Se carga automáticamente al iniciar Vite, sin descargar ni descomprimir
archivos a mano. El archivo contiene sólo los datos públicos de identificación de productos y su
atribución. No se sirve como parte del JavaScript de la aplicación.

Para actualizarlo desde `frontend/`:

```sh
npm run catalogo:sepa:descargar
```

`catalogo:sepa:descargar` consulta el catálogo oficial, selecciona el ZIP más reciente por su fecha y lo
descarga. Procesa los archivos `productos.csv`, incluidos los ZIP anidados, sin extraer sus rutas al
proyecto. Lee el código desde `id_producto`; `productos_ean` es un indicador y no el código.

Valida el dígito de control, conserva los ceros iniciales y deduplica GTIN equivalentes. El índice sólo
contiene código, descripción, marca y presentación; no incorpora los precios de cada supermercado.
Guarda fecha, origen y SHA-256 de la descarga para identificar el conjunto importado. Una descarga o
importación fallida conserva el índice anterior.

La actualización se guarda en `frontend/.local/catalogo-sepa.json`, excluido de Git, y tiene prioridad
sobre el índice incluido. La descarga diaria pesa cientos de MB y su procesamiento puede tardar varios
minutos. Reiniciar Vite después de actualizar el índice. `SEPA_CATALOGO_PATH` permite configurar otra
ubicación, en JSON o JSON comprimido con gzip. Una ruta explícita inválida se informa como indisponible.

También se admite importar un ZIP oficial ya descargado, indicando la fecha de sus datos:

```sh
npm run catalogo:sepa -- "ruta/sepa.zip" 2026-11-10
```

Para un conjunto propio previamente preparado se admite `.jsonl`, con un objeto por línea:

```json
{"codigo":"77903754","descripciones":["Descripción del producto"],"marcas":["Marca"]}
```

Sin un índice válido, la pantalla informa que SEPA no está disponible y permite continuar con la otra
fuente o con carga manual.

## Consulta de Open Food Facts por API

No necesita descargar una base ni configurar una clave. Con conexión a Internet, el adaptador consulta
la API v3 de Open Food Facts por código. Envía sólo ese identificador, un agente de aplicación y los
campos públicos requeridos; elimina cookies, credenciales y referencias al negocio.

Las respuestas se validan con Zod y se verifica que el GTIN coincida. Cada fuente tiene un plazo de
respuesta y estados separados para producto inexistente e indisponibilidad. Las consultas exitosas o
sin coincidencia se conservan en memoria 15 minutos; las fallidas pueden reintentarse. El proxy limita
las consultas externas a 12 por minuto y permite completar los datos a mano si se alcanza ese límite.
La caché no equivale a un catálogo completo sin conexión.

## Uso y guardado

1. Leer el código con cámara, lector USB/Bluetooth que envía Enter, o ingresarlo manualmente.
2. Para un producto nuevo, revisar la descripción sugerida y la foto, si existe. Se elige la descripción
   más informativa de SEPA y Open Food Facts, completando marca y presentación sin repetirlas. La foto
   puede provenir de la otra fuente para el mismo GTIN. **Ver origen de los datos** muestra el detalle.
3. Completar los campos reales de esa tabla: conteo, costo, precio, mínimo y campos personalizados.
4. Confirmar la carga. Se puede leer el siguiente producto o volver a la tabla de destino.

El inventario propio tiene prioridad. Un código ya registrado muestra el producto existente; leerlo
no crea una fila ni altera las unidades. También se puede vincular a un producto sin código. El
selector excluye productos con otro GTIN y vuelve a validar la condición al guardar.

Se respeta la configuración de cada campo: conteo cero válido y precios opcionales cuando la tabla
lo permite. Los errores aparecen junto al campo y al confirmar se enfoca el primero. Cambiar de código
o navegar advierte si queda una carga pendiente. Descartar conserva el inventario. Tras guardar o
descartar con teclado, el foco vuelve a la entrada del código; con cámara no se fuerza el teclado móvil.

El guardado reutiliza `upsertRow`, el historial `commit` y la sincronización habitual del sistema.
Valida sobre una copia para evitar cambios parciales y sólo agrega la columna de código al confirmar.
Revalida duplicados al guardar. En ediciones aplica los campos modificados y conserva los cambios
concurrentes de otros campos; si ambos flujos modificaron el mismo dato, pide volver a revisar.
No agrega coordinación nueva entre usuarios ni modifica movimientos de ventas o compras.

## Cámara

ZXing se carga al abrir el lector. Admite EAN-8, EAN-13, UPC-A e ITF/GTIN-14, con validación de longitud
y dígito de control. Esa validación no certifica una asignación de GS1.

La selección automática prioriza la cámara elegida manualmente, recordada por identificador y etiqueta,
y luego la trasera principal. Una lectura automática aislada no desplaza la selección manual. El
selector muestra la cámara utilizada y permite cambiarla.

Cada apertura tiene video, stream y decodificador propios. Libera las pistas al leer, cerrar, ocultar la
página o desmontar, incluidos los permisos y decodificadores tardíos. Hay plazo de inicio, errores
recuperables y enfoque continuo cuando el dispositivo lo admite. No guarda fotos ni videos.

Las pruebas automatizadas cubren la prioridad y los ciclos de cámara. Sigue siendo necesaria la prueba
física de apertura, lectura repetida y reapertura en Android e iPhone.

## Fuentes y despliegue

- [SEPA / Secretaría de Comercio](https://datos.produccion.gob.ar/dataset/sepa-precios): CC BY 4.0.
- [Open Food Facts y asociados](https://openfoodfacts.github.io/openfoodfacts-server/api/): base ODbL,
  contenido DbCL y fotos CC BY-SA, con posibles derechos de terceros.
- El índice SEPA incluido conserva fecha, URL de origen, licencia y SHA-256 de su ZIP. Es una extracción
  de los identificadores públicos de productos, con GTIN validados y deduplicados; se omiten precios,
  comercios y sucursales. No se garantiza que cubra todos los productos ni la exactitud de cada registro.
- Las fuentes se mantienen separadas y atribuidas. No se redistribuye una base combinada con Open Food
  Facts ni se envían modificaciones a esos catálogos. GS1 y ARCA no están integrados.

Los adaptadores de `frontend/config/catalogosProductos.ts` funcionan en el **servidor local de Vite**.
El build estático no sirve esos endpoints: antes de desplegar hay que trasladarlos al backend o a un
servicio de catálogo. Los endpoints existentes de guardado de inventario no cambian.

## Estructura y validación

Los componentes presentan controles y reciben acciones; los hooks coordinan cámara, consultas y
guardado; `lib/escaneo` valida e identifica productos; `api` valida las respuestas externas. Los estilos
usan los tokens centrales del sistema.

```sh
npm run lint
npm run typecheck
npm test
npm run build
```

Las pruebas cubren importación y descarga SEPA, errores que conservan el índice, consulta por fuente,
GTIN equivalentes, campos propios, duplicados, guardado atómico, ediciones concurrentes, cancelación,
navegación pendiente, selección de inventario y ciclo de cámara.
