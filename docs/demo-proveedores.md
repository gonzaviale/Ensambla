# Proveedores: prueba de funcionalidad de Nacho

Rama `feat/prueba-funcionalidad-nacho`, creada desde `dev` actualizado (`5d7229f0`).

## Abrir la demo

En `frontend`, ejecutar `npm ci` y `npm run dev:demo`. Entrar con la cuenta ficticia
`demo@ensambla.local` / `EnsamblaDemo2026!` y abrir **Proveedores · Demo** desde la barra
de navegación, o ir a `http://127.0.0.1:5173/proveedores`.

La ruta requiere sesión, conserva la guía y bienvenida existentes, y trabaja con una copia
independiente del almacén de ejemplo. Todo el servidor está simulado en `dev:demo`.
La comparación con compras anteriores se consulta en **Costos y gastos**; la revisión del
comprobante permite editar cantidades, costos y totales, pero todavía no muestra etiquetas
de variación de precio junto a cada renglón.
Los cambios del prototipo se guardan por cuenta en `localStorage`, con esquema validado y
clave versionada `ensambla:proveedores-demo:v1:<usuarioId>`. No hay cambios del backend,
migraciones ni escrituras a los cuadernos reales. El cierre de sesión desmonta la pantalla;
otra cuenta usa una instancia y almacenamiento diferentes. Es aislamiento de demostración,
no un mecanismo de seguridad para información sensible.

## Recorrido para probar

1. **Preparar pedido:** se sugieren agotados y productos bajo mínimo, por proveedor. La
   cantidad inicial repone hasta dos veces el mínimo (al menos una unidad). Quitar un
   producto, cambiar cantidades o agregar uno con stock suficiente actualiza el mensaje.
   Cantidad cero lo excluye de la salida. Recalcular reemplaza el borrador del proveedor.
2. **Compartir:** copiar el texto para pegarlo en WhatsApp o abrir la impresión y elegir
   «Guardar como PDF». No se envía ningún mensaje ni se mueve stock al preparar el pedido.
3. **Compras e historial:** seleccionar un PDF/imagen (máximo 10 MB) o «Probar sin archivo».
   **La lectura es simulada**: se proponen hasta tres productos de ejemplo, independientemente
   del contenido del archivo. El proveedor nuevo sin productos permite agregar renglones
   manuales; primero se pueden crear sus productos desde Stock de prueba.
4. **Revisar:** corregir proveedor, número, fecha, vinculación de producto, unidades, costo
   unitario, cargos y descuento. El total declarado debe coincidir al centavo. Para cajas,
   convertir explícitamente a unidades del producto; otra presentación es otro producto.
   La revisión se desmarca si se modifica cualquier dato. Confirmar requiere la aceptación.
5. **Confirmar:** una transición registra compra, suma stock y agrega un gasto de Mercadería
   vinculado. Id y proveedor+número evitan duplicados; un comprobante antiguo no pisa un
   costo más reciente. El precio de venta nunca cambia. El archivo se previsualiza localmente
   y se libera al cancelar/confirmar; sólo queda su nombre, no su contenido.
6. **Costos y gastos:** comparar últimas dos compras del mismo producto/presentación y
   proveedor. Se indica variación nominal, margen bruto al precio actual y una referencia
   para conservar el margen porcentual que daría ese precio con el costo anterior.
7. **Gastos generales:** agregar alquiler, servicios u otros; elegir el mes. Se reutilizan
   las categorías y claves (`name`, `date`, `total`, `category`) del bloque Gastos existente.
8. **Stock de prueba:** comprobar las nuevas cantidades, costo y precio conservado, o
   agregar productos al proveedor seleccionado. Recargar conserva los datos locales.

## Criterio de cálculo

Ventas registradas − costo histórico de unidades vendidas = margen bruto.
Margen bruto − gastos generales del mismo mes = resultado operativo estimado.
Las compras de mercadería se muestran por separado: no se restan otra vez del margen.
Las ventas incluyen fiado; el resultado no es caja ni ganancia neta, y sólo considera
los registros de demostración. Los cargos/descuentos generales afectan el total de la
compra; no se distribuyen al costo unitario usado para comparar. Importes en ARS.

## Estructura

- `types/proveedores.ts`: contratos y validación de datos locales.
- `lib/proveedores`: semilla, pedidos, validaciones, recepción idempotente y cálculos puros.
- `hooks/proveedores`: estado, persistencia, archivos locales, impresión y portapapeles.
- `components/proveedores`: presentación reutilizable y formularios controlados.
- `pages/Proveedores` y ruta: composición y navegación.
- `styles/sistema/proveedores.css`: estilos; todos los colores salen de los tokens existentes
  en `styles/sistema/styles.css`, sin colores literales en componentes.

Para convertirlo en funcionalidad productiva faltan persistencia transaccional en servidor
por negocio, extracción documental real con revisión, conservación privada de adjuntos,
identidad/presentaciones de productos y política contable definida. Este prototipo no altera
el ingreso por texto/voz/WhatsApp que ya exista, ni incorpora esas integraciones externas.

Pruebas específicas: `npm test -- proveedores` (23 pruebas de reglas, pantallas, persistencia y archivos).
Chequeos generales: `npm run lint`, `npm run build`, `npm test`.
