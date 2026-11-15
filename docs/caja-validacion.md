# Guía de aceptación de caja — ENS-317

Esta guía verifica ENS-318 a ENS-326 (T-224 a T-232). Implementación y pruebas automáticas no sustituyen la aceptación del usuario.

## Preparación

Usar el entorno de prueba de clientes y deudas, con backend y PostgreSQL reales.
No borrar volúmenes ni restaurar fixtures sobre el negocio que usa la persona.
El script crea sus propias cuentas y sistema de prueba y elimina su sistema al terminar.

Desde la raíz de la rama:

```powershell
$env:ENSAMBLA_PUERTO='8091'
docker compose -p ensambla-clientes up -d --build --wait
$env:ENSAMBLA_URL='http://localhost:8091'
node scripts/verificar-caja.mjs
```

Para el teléfono, usar el túnel HTTPS existente y su misma base. Revisar la dirección actual con
`docker compose -p ensambla-clientes logs tunel-movil`.
No es necesario recrearlo para actualizar la aplicación.

## Recorrido manual pendiente de aceptación

| Paso | Acción | Resultado esperado |
|---|---|---|
| Cliente | Crear/editar un cliente en Caja | El mismo contacto se ve en Mi Negocio |
| Venta pagada | Agregar dos productos y cobrar todo | Total, vuelto e inventario correctos; venta Pagada |
| Parcial | Venta de 10.000, entrega de 4.000 y cliente | 6.000 pendientes; historia conserva total e importe inicial |
| Fiado | Venta con cliente y sin pagos | Toda la venta pendiente; no entra efectivo |
| Importe libre | Agregar importe, descripción Servicio, monto 2.500 | Aparece en compra e historial; no se crea producto |
| Importe parcial/mixto | Pagar 500 efectivo y 500 transferencia de ese importe | Saldo 1.500; solo 500 al cajón; vuelto si se entrega más efectivo |
| Inicio | Consultar el importe de 2.500 con entrega de 1.000 | Vendido 2.500, cobrado 1.000 y deuda 1.500, sin duplicaciones ni productos ficticios |
| Cuenta | Abrir Clientes → Con deuda → Ver cuenta | Saldos y ventas de esa persona, incluida la deuda del servicio |
| Entregas | Registrar 2.000 y luego 4.000 sobre la primera deuda | El saldo baja hasta cero; pasa a Pagada y permanece el historial |
| Antigüedad | Consultar una venta con deuda de 30 días o más | Etiqueta informativa; pagar una parte conserva fecha original; saldar quita etiqueta |
| Recuperación | Recargar durante una respuesta incierta y verificar la misma operación | No se duplica venta, cobro ni descuento |
| Stock | Intentar vender más existencia que la disponible | Se rechaza toda la venta; ningún efecto parcial |
| Movimientos | Revisar stock y sus movimientos tras venta/reintento | Una salida por renglón y saldo coincidente con inventario |
| Cierre | Abrir cierre sin completar contado | No permite guardar |
| Cero | Contar 0 en un período sin efectivo | Puede cerrar sin motivo |
| Diferencia | Contar un monto distinto del esperado | Exige motivo; espacios vacíos no alcanzan |
| Historial | Cerrar y después cobrar una deuda anterior | Dinero en el período actual; cierre anterior idéntico |
| Celular | Probar dos artículos, cobro pagado, parcial y dividido | Acciones accesibles; compra breve y cobro caben con teclado cerrado |
| Accesibilidad | Recorrer con Tab, editar cantidades, abrir/cerrar diálogos | Foco visible y contenido identificable sin depender del color |
| Anulación | Revisar las acciones disponibles | No hay anulación parcial; ventas confirmadas no se editan ni se borran por fila |

Con teclado abierto, zoom o mensajes extensos puede haber desplazamiento interno para no ocultar campos ni confirmación.
Una compra grande mantiene accesible Cobrar y permite desplazar su lista.

## Datos y pruebas reproducibles

- `scripts/verificar-caja.mjs`: sesión/CSRF, aislamiento, venta pagada/mixta/fiada/parcial, importe libre,
  idempotencia, concurrencia de stock, cobros posteriores, agrupación de cuentas, cierres y métricas de Inicio.
- `CajaPostgresIntegrationTest`: persistencia real, rollback, bloqueo concurrente, cantidades y movimientos,
  reintentos, importes libres con deuda y cierres históricos.
- `ImporteLibreCajaTest`: descripción, cantidad uno, compatibilidad de huellas y datos incompatibles.
- `CuentaClienteCajaItemTest`: agrupación, saldo, fecha más antigua, cuentas saldadas y cobros ajenos excluidos.
- `ReportesImporteLibreTest`: importes pagados, parciales y fiados en Inicio/resumen, sin inventar costo ni producto y respetando el período.
- `recuentoCaja.test.tsx`: recuento obligatorio, cero válido y explicación de diferencias.
- `importeLibreCaja.test.tsx`: formulario, importes separados y umbrales de 29/30 días con zona argentina.
- `contratosCaja.test.ts`: cliente HTTP, zod, fixtures y handlers MSW compartidos.

La fixture MSW fija una venta parcial del 16/10/2026 para revisar antigüedad respecto del 15/11/2026.
Las pruebas de frontera fijan su fecha explícitamente; no hace falta cambiar fechas ni datos de la base real.
Los mocks prueban presentación y contratos; las reglas de dinero y stock se validan contra el backend real.

## Criterio de cierre

- Implementación y verificaciones automáticas verdes.
- Revisión y merge de los PR en orden: #93 → dev; después #97 → dev (actualizar su base al integrar #93).
- Prueba manual del usuario con celular/cámara, teclado y cobros, y resolución de cualquier hallazgo.
- Registrar la aceptación en Jira recién después de esas comprobaciones.

La política de anulación está definida en [los contratos](caja-contratos.md); la operación se implementará en otra historia.
El botón flotante y el escáner emparejado corresponden a ENS-327. El QR de Mercado Pago corresponde a ENS-348.
