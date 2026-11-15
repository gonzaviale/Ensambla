# Caja: clientes, ventas con saldo y cobros (ENS-317)

La venta conserva artículos, precios y total. Los cobros registran dinero recibido y su medio; el saldo es el total menos lo cobrado. El servidor calcula saldo y estado: `PENDIENTE` sin cobros, `PARCIAL` con una entrega y `PAGADA` al completar el total.

## Clientes y venta

Caja reutiliza los registros de las tablas Clientes de Mi Negocio. Si Ventas tiene una relación de cliente, se usa esa tabla. Una venta que deja saldo exige un cliente real del mismo negocio y conserva su nombre como referencia histórica. Una venta pagada puede ser anónima.

`POST /api/caja/ventas` conserva el contrato existente y agrega `clienteId` opcional. `pagos` omitido conserva el cobro completo anterior; `pagos: []` registra un fiado; importes positivos que sumen menos que el total registran un pago parcial. Un pago mixto puede ser completo o parcial.

```json
{
  "id": "00000000-0000-0000-0000-000000000001",
  "proyectoId": "00000000-0000-0000-0000-000000000002",
  "inventarioId": "00000000-0000-0000-0000-000000000003",
  "ventasId": "00000000-0000-0000-0000-000000000004",
  "clienteId": "00000000-0000-0000-0000-000000000005",
  "lineas": [{"productoId": "00000000-0000-0000-0000-000000000006", "cantidad": 1, "precio": 10000, "modo": "UNIDAD"}],
  "medio": "TRANSFERENCIA",
  "recibido": null,
  "pagos": [{"medio": "TRANSFERENCIA", "importe": 4000}]
}
```

El comprobante responde `total: 10000`, `cobrado: 4000`, `saldo: 6000`, `estado: "PARCIAL"`, `clienteId` y `clienteNombre`, además de los campos existentes. El efectivo recibido se usa para el vuelto, no como importe cobrado ni como cancelación automática del saldo.

## Cuenta y cobros posteriores

En **Caja → Clientes** se ven los contactos compartidos con Mi Negocio, el total que deben en el inventario elegido y el filtro **Con deuda**. **Ver cuenta** abre sus ventas y movimientos. Se puede cobrar una venta puntual o aplicar un importe a varias deudas del mismo cliente; la pantalla propone completar primero las más antiguas y muestra el reparto antes de confirmar. Un cliente eliminado de la agenda conserva su cuenta histórica.

**Actividad → Ventas → Pendientes** incluye ventas fiadas y parciales. El detalle permite ver pagos y registrar otro cobro. **Actividad → Cobros** muestra las entradas del día por su fecha real. Una venta pagada permanece en el historial con saldo cero.

`GET /api/caja/ventas?proyectoId=…&inventarioId=…` devuelve `{ventas, cobros}`. Las ventas incluyen sus pagos acumulados y `pagosIniciales`; los cobros posteriores son eventos separados, con fecha del servidor y aplicaciones por venta. Así no se cuenta dos veces la entrega inicial.

`POST /api/caja/cobros` registra una operación independiente e idempotente:

```json
{
  "id": "00000000-0000-0000-0000-000000000007",
  "proyectoId": "00000000-0000-0000-0000-000000000002",
  "inventarioId": "00000000-0000-0000-0000-000000000003",
  "aplicaciones": [{
    "ventaId": "00000000-0000-0000-0000-000000000001",
    "pagos": [{"medio": "TRANSFERENCIA", "importe": 2000}]
  }]
}
```

Se permiten hasta tres medios distintos por venta y varias ventas del mismo cliente. Si una aplicación supera el saldo o pertenece a otra caja, no se guarda ninguna. Un reintento con el mismo id y contenido devuelve el comprobante anterior, incluso después del cierre. Ante una respuesta incierta el navegador conserva la solicitud y ofrece **Verificar misma operación**; recargar no crea un cobro nuevo.

Los cierres suman las entregas iniciales de sus ventas y los cobros posteriores abiertos. Se puede cerrar un período con cobros aunque no tenga ventas nuevas. Cobrar una deuda vieja no reabre el cierre anterior ni modifica su comprobante. La migración V36 agrega la tabla de cobros sin editar migraciones aplicadas ni reescribir comprobantes viejos.

## Reglas del circuito

- El negocio se obtiene de la sesión. Cliente, inventario o venta ajenos responden 404.
- El servidor revalida precio, unidad y existencias; stock insuficiente bloquea toda la venta.
- Venta, cobro inicial y descuento de stock comparten transacción. Un reintento con el mismo id y los mismos datos no vuelve a descontar ni cobrar; otro contenido con ese id responde 409.
- El historial de ventas muestra saldo actual. Pendientes incluye ventas sin pagos y parcialmente cobradas. Saldar una venta la conserva en Todas y Pagadas.
- Los pagos posteriores deben tener id propio, importe positivo, medio y fecha del servidor. Una deuda no puede quedar negativa ni cobrarse dos veces por un reintento.
- Los cobros posteriores se cuentan en el día y período en que se reciben. No generan otra venta ni descuentan nuevamente stock. Los cierres guardados conservan su foto histórica.
- Las ventas previas a esta entrega siguen siendo pagadas y mantienen precios, pagos y cierres originales.

## Validación de aceptación

Registrar una venta de $10.000 con una entrega de $4.000 a nombre de un cliente, recargar, encontrar $6.000 pendientes, registrar $2.000 y luego $4.000. Verificar saldo cero, estado Pagada, historial completo, descuento único de stock, separación por fecha de cobro, cierre anterior intacto y aislamiento respecto de otra cuenta.

El acceso móvil se prueba contra frontend compilado, backend y PostgreSQL reales con el perfil `movil` de `compose.yaml`; los mocks sólo sirven como apoyo del desarrollo y las pruebas del frontend.
