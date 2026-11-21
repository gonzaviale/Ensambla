# Cobro con QR desde la caja (ENS-348 · H-61)

Documento corto de la investigación T-252 y de las decisiones del **nivel 1**. El nivel 2 (vincular la cuenta de
Mercado Pago del comerciante) queda como mejora de MVP 5.

## Recomendación

Implementar solo el **nivel 1**: el comerciante carga una vez su alias o CVU, la caja muestra el monto en grande, el
alias para copiar y un QR, y el comerciante confirma a mano con **"Ya me transfirieron"**. Ensambla no verifica el pago,
no guarda ni mueve dinero y no llama a Mercado Pago.

El nivel 2 necesita una aplicación de Mercado Pago con OAuth, una cuenta vendedora de prueba (hoy solo existen cuentas
de prueba de Suscripciones) y sucursales y cajas dadas de alta por cada comerciante. Es demasiado para este MVP y
muchos comerciantes no tienen habilitado el cobro con QR en Mercado Pago.

## Qué contiene el QR del nivel 1

El QR lleva el alias o el CVU **tal como lo cargó el comerciante**, sin monto. Decisiones:

- El alias es texto libre: no se valida su formato. Se recorta y se limita el largo para que entre en la base.
- El monto no va dentro del QR. La pantalla lo muestra en grande, junto al alias copiable, para que el cliente lo escriba
  al transferir.
- No se arma un QR interoperable de Transferencias 3.0 con el importe adentro: exige el CVU o CBU de 22 dígitos y no se
  pudo comprobar con una billetera real que acepte un QR generado por Ensambla. Queda como posible mejora cuando el
  comerciante cargue un CVU.

Límite que hay que aclararle al comerciante: según el BCRA, el QR de un vendedor sin monto obliga al comprador a
ingresar el importe a mano; y un QR con solo el alias en texto puede no abrir el pago en todas las apps. Por eso el alias
copiable y el monto en grande son lo importante de la pantalla.

## Qué se puede hacer con la API Orders de Mercado Pago (nivel 2)

Fuente: [integración de QR con la API Orders](https://www.mercadopago.com.ar/developers/es/news/2025/09/24/QR-Code-Integration-with-Orders-API).

- Hay tres modos de QR: **estático** (un QR fijo por caja que recibe las órdenes), **dinámico** (un QR por cada
  operación) e **híbrido** (la orden usa el QR estático y además genera uno dinámico; al pagar con uno se desactiva el
  otro).
- El estático dura 10 minutos como máximo y el dinámico 15 por defecto.
- Antes hay que dar de alta sucursales (`/users/{user_id}/stores`) y cajas (`/pos`).
- Las órdenes se crean con `POST /v1/orders`. Un pago rechazado no se informa: la orden queda en `created` hasta que
  llega uno aprobado.
- Un QR fijo por caja sirve con el modo estático.

**Sin confirmar** (queda para cuando se retome el nivel 2): requisitos exactos de OAuth para cuentas de terceros,
revisión o certificación de Mercado Pago y comisiones por camino. No se inventan cifras: las comisiones se consultan en
la documentación oficial del proveedor.

## Qué se puede hacer solo con el alias

Fuentes: [Transferencias 3.0 del BCRA](https://www.bcra.gob.ar/en/transfers-3-0/) y el
[boletín CIMPRA 525](https://www.bcra.gob.ar/archivos/Pdfs/SistemasFinancierosYdePagos/SNP3525.pdf) (no se pudo
descargar al redactar este documento; se usó su resumen).

- Cualquier cuenta de pago identificada con CBU, CVU o alias puede cobrar por transferencia.
- Con el QR del vendedor a la vista y sin herramientas de cobro, el comprador ingresa el monto a mano.
- No hace falta vincular nada ni tener habilitado el comercio en Mercado Pago.

## Diseño del nivel 1 en Ensambla

- **Negocio:** alias o CVU y nombre del titular (opcionales). Migración V43.
- **Cobro con QR:** registro propio con estados `PENDIENTE`, `COBRADO`, `VENCIDO` y `CANCELADO`, creado antes de la venta.
  El vencimiento es de 15 minutos y se calcula al leer, sin tareas programadas.
- **Confirmación manual:** "Ya me transfirieron" envía el id del cobro junto con el pedido de venta o de cobro de deuda
  que ya existe. En la misma transacción el cobro pasa a `COBRADO` con marca de confirmación manual y la venta se registra
  con medio `TRANSFERENCIA`. Un reintento con el mismo id no cobra dos veces.
- **Cancelar:** el cobro pendiente pasa a `CANCELADO` y no toca stock ni cierres. La caja no anula ventas ya pagadas
  (ver [caja-contratos.md](caja-contratos.md)).
- **Vencido:** se muestra como vencido y se puede generar uno nuevo. Confirmar a mano un cobro vencido sigue permitido
  para no perder una venta que el cliente sí pagó.
- **Montos:** el QR corresponde al importe del pago: la venta completa, la entrega parcial de un fiado a medias o el
  cobro de una deuda anterior.
- **Aislamiento:** el negocio sale de la sesión; un cobro de otro negocio responde 404.
- **Aviso:** la pantalla dice claramente que Ensambla no verificó el pago.

## Cómo funciona en la app (nivel 1)

1. **Cargar el alias.** En **Tu cuenta → Perfil → Cobro con QR**, el comerciante escribe su alias o CVU (texto libre) y,
   si quiere, el nombre del titular. Debajo ve un QR de prueba con lo que escribió.
2. **Cobrar.** En **Caja**, al cobrar una venta con transferencia aparece **Cobrar con QR**. Lo mismo al saldar una deuda
   desde **Clientes → Ver cuenta → Guardar pago**.
3. **Mostrarle al cliente.** La pantalla muestra el monto a transferir, el QR, el alias para copiar, el titular y hasta
   qué hora vale. Avisa que Ensambla no verificó el pago.
4. **Confirmar.** Cuando el comerciante ve la plata en su cuenta toca **Ya me transfirieron**: la venta o el pago se
   guardan y el cobro queda como confirmado a mano. **Cancelar cobro con QR** (o cerrar la pantalla) lo cancela y no guarda
   nada.

El QR es solo por la parte que se paga por transferencia: sirve con la venta entera, con una entrega a medias de un
fiado y con un pago dividido (por ejemplo, 6.000 en efectivo y 4.000 por transferencia generan un QR de 4.000).

## Endpoints

Todos exigen sesión y el token de `GET /api/auth/csrf`. El negocio sale de la sesión; un cobro de otro negocio responde 404.

| Método y ruta | Entrada | Salida |
|---|---|---|
| `POST /api/caja/cobros-qr` | `id` (lo genera el cliente) e `importe` | `{id, importe, alias, titular, estado, venceEn}` |
| `POST /api/caja/cobros-qr/{id}/cancelar` | nada | el mismo cobro, ya cancelado |
| `POST /api/caja/ventas` | los datos de siempre y `cobroQrId` opcional | el comprobante de siempre |
| `POST /api/caja/cobros` | los datos de siempre y `cobroQrId` opcional | el cobro de siempre |

- Generar el mismo `id` con el mismo importe devuelve el cobro que ya estaba; con otro importe responde 409. Sin alias
  cargado en el negocio responde 400.
- Con `cobroQrId`, la venta o el cobro se guardan y el cobro con QR queda `COBRADO` en la misma transacción. Lo transferido
  (la suma de los pagos por `TRANSFERENCIA`) tiene que coincidir con el importe del QR. Un cobro ya cobrado o cancelado no se
  puede reutilizar y frena toda la operación.
- Un cobro pendiente se ve `VENCIDO` a los 15 minutos, pero todavía se puede confirmar para no perder una venta que el
  cliente sí pagó.
- Un cobro cobrado no se cancela; uno cancelado no se confirma.

Los avisos de pago de Mercado Pago, la vinculación de la cuenta y la conciliación automática son del nivel 2 y no están
implementados.

## Guía de prueba (sin ningún cobro real)

El nivel 1 no llama a Mercado Pago, así que no hace falta ninguna cuenta de prueba ni se mueve dinero.

**Preparación:** levantar backend y frontend como indica el README y entrar con una cuenta que tenga un negocio con
productos y stock.

1. **Sin alias.** En **Caja**, armar una compra, tocar **Cobrar**, elegir **Transferencia** y **Cobrar con QR**.
   *Esperado:* dice que todavía no cargó el alias y ofrece **Cargar mi alias o CVU**.
2. **Cargar el alias.** Ir a **Tu cuenta → Perfil**, escribir un alias cualquiera (por ejemplo `mi.alias.prueba`) y el
   titular. *Esperado:* aparece el QR de prueba; **Guardar cobro con QR** avisa "Guardamos tu cobro con QR".
3. **Venta total.** Repetir el paso 1. *Esperado:* monto igual al total, QR, alias, titular y "Vale hasta las…".
   **Copiar alias** avisa "Alias copiado". **Ya me transfirieron** guarda la venta; el stock baja una sola vez.
4. **Cancelación.** Repetir y tocar **Cancelar cobro con QR**. *Esperado:* vuelve al cobro, no se guardó ninguna venta y se
   puede abrir otro QR.
5. **Cobro repetido.** Con la red cortada justo al tocar **Ya me transfirieron** (o con el servidor apagado un
   momento), usar **Verificar misma operación**. *Esperado:* la venta aparece una sola vez.
6. **Cobro vencido.** Abrir un QR, esperar más de 15 minutos y tocar **Ya me transfirieron**. *Esperado:* la venta se
   guarda igual.
7. **Fiado a medias.** Elegir **A medias**, un cliente, pagar ahora 4.000 con **Transferencia** y **Cobrar con QR**.
   *Esperado:* QR por 4.000 y la venta queda `PARCIAL` con 6.000 pendientes.
8. **Pago dividido.** Una compra de 10.000 con 6.000 en efectivo y el resto por transferencia. *Esperado:* QR por 4.000.
9. **Cobro de una deuda.** En **Clientes → Ver cuenta → Guardar pago**, elegir **Transferencia** y **Cobrar con QR**.
   *Esperado:* QR por el importe del pago; **Ya me transfirieron** lo guarda y reduce el saldo.
10. **Reutilizar un QR.** Con la API, mandar un `cobroQrId` ya cobrado o cancelado en `POST /api/caja/ventas`.
    *Esperado:* responde 400 y no se guarda la venta.
11. **Aislamiento.** Con otra cuenta, cancelar un `id` de cobro de la primera. *Esperado:* 404.

**Límite que hay que decirle al comerciante:** el QR lleva el alias o CVU como texto, sin monto. Algunas apps de pago
pueden no abrir la transferencia al escanearlo: por eso la pantalla muestra el monto en grande y el alias para copiar.
