# Contratos y decisiones de caja — ENS-317

Complementa [el circuito funcional](caja-clientes-y-cobros.md). El prefijo es `/api/caja`.
Todos los importes JSON están en pesos, con hasta dos decimales; la UI calcula en centavos.
Los identificadores son UUID. El negocio sale de la sesión, nunca del cuerpo.
Las escrituras requieren sesión y el token de `GET /api/auth/csrf`.
Las fechas de operación las asigna el servidor. El día comercial usa `America/Argentina/Buenos_Aires`.

## Operaciones

| Método y ruta | Entrada | Salida |
|---|---|---|
| POST /ventas | Identificador propio, proyecto, inventario, tabla Ventas, líneas, medio, recibido, pagos y cliente opcional | Venta con líneas históricas, pagos iniciales/acumulados, saldo y estado |
| GET /ventas | proyectoId, inventarioId | ventas, cobros y cuentas agrupadas por cliente |
| POST /cobros | Identificador propio, proyecto, inventario y aplicaciones a ventas | Cobro fechado con sus aplicaciones y medios |
| GET /resumen | proyectoId, inventarioId, fecha opcional ISO yyyy-MM-dd, pagina desde 0 | Día elegido, período abierto y lista paginada de cierres |
| POST /cierres | Identificador propio, proyecto, inventario, revisión del período, fondo, entradas, retiros, contado y nota | Cierre histórico con resumen, efectivo esperado, contado y diferencia |
| GET /cierres/{id} | Identificador del cierre | El mismo comprobante histórico, sin recalcularlo |
| POST /cobros-qr | Identificador propio e importe | Cobro con QR pendiente con el alias del negocio y su vencimiento |
| POST /cobros-qr/{id}/cancelar | Identificador del cobro | El mismo cobro, cancelado |

No hay endpoint de anulación ni devolución en ENS-317. `POST /ventas` y `POST /cobros` aceptan un `cobroQrId` opcional: ver [cobro-con-qr.md](cobro-con-qr.md).

## Venta por importe libre

**Agregar importe** pide descripción (1–160 caracteres sin espacios en los bordes) y monto positivo.
Se agrega como renglón independiente: cantidad 1, modo UNIDAD, productoId null y descripción.
No se agrupan dos importes libres aunque tengan el mismo precio. Se pueden combinar con artículos.

Ejemplo de venta parcial con dos medios:

```json
{
  "id": "00000000-0000-0000-0000-000000000101",
  "proyectoId": "00000000-0000-0000-0000-000000000102",
  "inventarioId": "00000000-0000-0000-0000-000000000103",
  "ventasId": "00000000-0000-0000-0000-000000000104",
  "clienteId": "00000000-0000-0000-0000-000000000105",
  "lineas": [
    {"productoId": null, "descripcion": "Servicio", "cantidad": 1, "precio": 2500, "modo": "UNIDAD"}
  ],
  "medio": "MIXTO",
  "recibido": 600,
  "pagos": [{"medio": "EFECTIVO", "importe": 500}, {"medio": "TRANSFERENCIA", "importe": 500}]
}
```

Respuesta:

```json
{
  "id": "00000000-0000-0000-0000-000000000101",
  "lineas": [
    {"productoId": null, "nombre": "Servicio", "modo": "UNIDAD", "cantidad": 1, "precio": 2500, "importe": 2500}
  ],
  "medio": "MIXTO",
  "total": 2500,
  "recibido": 1100,
  "vuelto": 100,
  "creadoEn": "2026-11-15T15:00:00Z",
  "pagos": [{"medio": "EFECTIVO", "importe": 500}, {"medio": "TRANSFERENCIA", "importe": 500}],
  "clienteId": "00000000-0000-0000-0000-000000000105",
  "clienteNombre": "Ana de prueba",
  "cobrado": 1000,
  "saldo": 1500,
  "estado": "PARCIAL",
  "pagosIniciales": [{"medio": "EFECTIVO", "importe": 500}, {"medio": "TRANSFERENCIA", "importe": 500}]
}
```

`recibido` de entrada es el efectivo entregado. En la respuesta conserva la entrega total, incluido el vuelto.
Solo los pagos aplicados cancelan deuda. Pagar 600 en efectivo para una parte de 500 devuelve 100.
No se crea ningún producto ni movimiento de stock para el importe libre.
La tabla Ventas conserva cantidad, precio, fecha, cliente y marca de fiado. El producto queda vacío y
el origen conserva la referencia de la venta; el historial de caja conserva la descripción y el detalle completo.
La relación Producto obligatoria de las ventas de inventario se mantiene; el importe libre es la excepción explícita.

Para un artículo se envía productoId real, cantidad, precio y modo UNIDAD/PESO, sin descripción.
El servidor revalida precio y modo. UNIDAD exige cantidad entera; PESO admite hasta tres decimales.
Hasta 100 renglones; máximo tres medios distintos (EFECTIVO, TRANSFERENCIA, TARJETA).
Omitir pagos mantiene la compatibilidad de cobro completo. pagos vacío deja la venta fiada.
Una deuda exige cliente real del mismo negocio. Una venta pagada admite cliente anónimo.

## Cuenta de cliente y cobros

GET /ventas conserva ventas y cobros y añade cuentas. Cada cuenta contiene:

| Campo | Significado |
|---|---|
| clienteId, clienteNombre | Identidad y nombre histórico del cliente |
| saldo | Suma de saldos actuales de sus ventas en ese inventario |
| cantidadPendientes | Ventas con saldo mayor a cero |
| pendienteDesde | Fecha de la venta pendiente más antigua; null al saldar |
| ventas | Historial completo del cliente, incluidas las pagadas |
| cobros | Cobros posteriores de esa misma persona |

Una venta anónima no crea cuenta. Una cuenta saldada conserva su historial y no aparece en Con deuda.
Eliminar al cliente de la agenda conserva el nombre y sus deudas en caja.
La etiqueta **Deuda de 30 días o más** usa días calendario del negocio desde la venta original,
solo mientras quede saldo. Un pago parcial no cambia la fecha. No genera avisos ni notificaciones.

Ejemplo POST /cobros para saldar el servicio anterior:

```json
{
  "id": "00000000-0000-0000-0000-000000000106",
  "proyectoId": "00000000-0000-0000-0000-000000000102",
  "inventarioId": "00000000-0000-0000-0000-000000000103",
  "aplicaciones": [
    {"ventaId": "00000000-0000-0000-0000-000000000101", "pagos": [{"medio": "TRANSFERENCIA", "importe": 1500}]}
  ]
}
```

El comprobante contiene id, clienteId, clienteNombre, importe (1500), creadoEn y aplicaciones.
Cada aplicación devuelve ventaId, pagos e importe. La venta pasa a PAGADA y saldo 0 en GET /ventas.
Un cobro puede repartirse entre varias ventas del mismo cliente y caja; toda la operación es atómica.
La UI propone primero las deudas más antiguas y permite revisar el reparto antes de confirmar.
El dinero entra en el día del cobro. No genera otra venta ni otro movimiento de stock.

## Resumen y cierre

GET /resumen devuelve fecha, dia, abierta, cierres, pagina y hayMas.
Los resúmenes dia/abierta tienen ventas, cobros, total, efectivo, transferencia, tarjeta, desde y revision.
total es venta registrada; los medios suman dinero efectivamente recibido en ese día/período.
La lista cierres contiene id, desde, cerradoEn, total, cantidadVentas, diferencia y cobrado.
Un período con solo cobros posteriores también puede cerrarse.

Ejemplo POST /cierres con un faltante explicado:

```json
{
  "id": "00000000-0000-0000-0000-000000000110",
  "proyectoId": "00000000-0000-0000-0000-000000000102",
  "inventarioId": "00000000-0000-0000-0000-000000000103",
  "revision": "copiar-la-revision-del-resumen-abierto",
  "fondoInicial": 100,
  "entradas": 0,
  "retiros": 0,
  "contado": 550,
  "nota": "Retiro de 50 pendiente de registrar"
}
```

Con 500 cobrados en efectivo: esperado 600, contado 550, diferencia -50.
La respuesta contiene id, resumen, fondoInicial, entradas, retiros, esperado, contado, diferencia, nota y cerradoEn.
Se exige contado en cierres nuevos; **0 es válido**. Una diferencia distinta de cero exige nota sin espacios vacíos.
Sin diferencia, la nota es opcional (se envía cadena vacía). Máximo 1000 caracteres.
Todos los movimientos de efectivo admiten cero, sin negativos ni más de dos decimales.
Transferencias y tarjetas no forman parte del efectivo esperado.
Los cierres anteriores con contado/diferencia null siguen legibles y sus reintentos conservan el comprobante original.
Si entraron ventas o cobros desde la revisión, se responde 409 y se pide revisar el resumen actualizado.

## Stock, idempotencia y concurrencia

La confirmación bloquea el negocio y guarda venta, filas, stock y movimientos en una transacción.
Cada artículo con control de stock registra su SALIDA en MovimientoStock y actualiza la cantidad del inventario una vez.
El stock insuficiente rechaza toda la venta, sin venta, cobro ni movimientos parciales.
Los productos sin control de stock y los importes libres no generan salidas.

Para inventarios anteriores o modificados desde el editor, se registra primero la variación entre
el saldo de movimientos y la cantidad actual, con nota **Ajuste al stock del inventario antes de cobrar**.
Ese ajuste puede ser una entrada o salida y evita inventar un saldo negativo al incorporar inventarios existentes.
Luego se registra **Venta de caja {id}**. No se vuelve a descontar al cargar las filas del historial.
Los importes libres se incorporan a ventas e ingresos de Inicio desde su comprobante, sin inventar
un producto ni costo y sin volver a sumar cobros o deuda desde las filas de la tabla.

Cada venta, cobro y cierre tiene identificador propio. Mismo id y mismo contenido devuelve el comprobante original.
Otro contenido con el mismo id responde 409. La descripción del importe libre participa en la huella.
Las huellas de ventas de inventario anteriores conservan su formato.
Ante un error de red, se guarda la solicitud y se ofrece verificar la misma operación; no se genera un nuevo id.
El precio actualizado no cambia un comprobante previo. Cobrar después de cerrar no reescribe cierres.

## Política de anulación acordada

ENS-317 documenta la política; no incorpora anulación ni devolución operativa.
Las ventas confirmadas no se editan ni eliminan desde el editor. Cerrar caja tampoco borra una deuda.
Una futura corrección deberá crear un movimiento de reversión vinculado al original y conservar ambos.
Debe distinguir cuánto se devuelve, con qué medio y qué deuda se cancela.
Solo se repone stock cuando se devuelve físicamente el artículo. Un importe libre nunca repone stock.
Una devolución posterior a un cierre afectará el período actual y conservará el cierre anterior.
No se habilita una acción parcial que solo borre la venta o cambie su estado sin registrar esos efectos.

## Errores

Formato común: timestamp, status, error, message, path, traceId y errors opcional por campo.
La UI muestra message; traceId sirve para diagnóstico. No expone detalles internos ni datos de otro negocio.

| HTTP | Caso | Acción |
|---|---|---|
| 400 | Importe/cantidad/descripción inválidos, cliente faltante para deuda, pago mayor al saldo, recuento ausente o diferencia sin motivo | Corregir el dato indicado |
| 401 | Sesión ausente o vencida | Volver a iniciar sesión |
| 403 | Escritura sin CSRF, o límite del plan | Renovar token o revisar el límite informado |
| 404 | Proyecto, inventario, cliente, venta o cierre ajeno/inexistente | Volver a seleccionar dentro del negocio |
| 409 | Precio/modo cambiados, stock insuficiente, id reutilizado con otros datos o revisión vieja | Actualizar/revisar la operación; no repetir con un id nuevo a ciegas |
| 5xx o red | Resultado de escritura incierto | Verificar la misma operación |

## Mocks compartidos

`frontend/src/mocks/handlers/caja.ts` exporta crearHandlersCaja para fixtures de contratos.
Recibe getters de ventas/cobros y callbacks opcionales cobrar, registrarPago, consultar, cerrar, obtenerCierre.
Los handlers aplican sesión y CSRF y mantienen el formato de errores. GET /ventas agrupa mediante
`mocks/db/cuentasCaja.ts`. Los callbacks pueden lanzar ApiError para ensayar rechazos.
`mocks/fixtures/caja.ts` aporta un importe parcial y cierre con recuento cero.
`test/api/contratosCaja.test.ts` valida esos contratos a través del cliente HTTP y los schemas zod.

Estos handlers se instalan por escenario con server.use; no reemplazan las validaciones del backend ni
se usan como prueba de stock/transacciones. Eso se comprueba contra PostgreSQL y por HTTP real con
`node scripts/verificar-caja.mjs`.

## Lenguaje

Referencia: H-56 / ENS-300 y T-209 / ENS-301. Textos breves, neutros, sin coloquialismos ni voz de Sami
fuera de su chat. Los estados y montos llevan la información principal; la explicación queda en el detalle.
La etiqueta de antigüedad es informativa, con texto además de color. La prueba de aceptación física sigue pendiente.
