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
