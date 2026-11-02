# Resumen del negocio en PDF

Un documento para el comerciante con lo que le sirve para decidir, armado con los datos que cargó en un sistema (proyecto) y un período.
Es una función **Premium** (`FuncionPlan.REPORTES_FILTRADOS`): con el plan gratuito el servidor responde con la restricción de plan y la
pantalla lo explica y lleva a los planes.

## Cómo se pide

- **Pantalla:** en un sistema, en modo usar, el botón «Resumen en PDF» abre un modal para elegir el período (últimos 7, 30 o 90 días, o fechas
  a mano) y descargar el archivo.
- **API:** `GET /api/proyectos/{proyectoId}/resumen/pdf?desde=aaaa-mm-dd&hasta=aaaa-mm-dd` devuelve `application/pdf` como adjunto
  (`resumen-<sistema>-<aaaammdd>.pdf`). Sin fechas son los últimos 30 días; sin `hasta` termina hoy. El período no puede superar 366 días ni
  tener `desde` posterior a `hasta`. Un sistema de otro negocio responde 404.
- Las fechas y el «hoy» son los de Argentina (`ZonaNegocio`).

## Qué lleva

| Sección | Contenido |
|---|---|
| Encabezado | Negocio, sistema, período y fecha de emisión |
| Tarjetas | Ingresos (y variación contra el período anterior de igual largo), unidades vendidas, ganancia estimada, ingreso por día, gastos, resultado, vendido a fiado y valor del stock a costo |
| Lo que más se vendió | Top 10 por unidades (a igualdad, por ingresos) con ingresos y ganancia cuando el producto tiene costo |
| Productos parados | Hasta 10 con stock y sin ventas en el período, con la plata inmovilizada (cantidad por costo) |
| Stock | Agotados y en o bajo su mínimo (hasta 15, con cuánto pedir para llegar al doble del mínimo), y lo que vale el inventario a costo y a precio |
| Clientes y fiado | Top 5 clientes del período y lo vendido a fiado (campo `credit` de las ventas) |
| Gastos | Total del período por categoría y resultado (ingresos menos gastos); sólo si el sistema tiene una tabla de gastos con importe (`total`) |
| Precios frente a la zona | Cuántos productos están por debajo, dentro o por encima del rango de la zona y los 5 más alejados; sólo agregados anónimos y con el mínimo de 3 comercios |
| Avisos de Sami | Pendientes por tipo, aplicados y descartados en el período y, de los aplicados ya medidos, cuánto cambió el ingreso semanal (siempre como comparación, no como causa) |

Las secciones sin datos se omiten (gastos, clientes, zona, productos).

## Cómo se calcula

- **Ingresos:** precio por cantidad de cada venta del período con precio. Las ventas sin precio cuentan en las unidades pero no en los ingresos.
- **Ganancia estimada:** (precio de venta − costo actual del producto) por cantidad, sólo de los productos que tienen costo; sin costos no se muestra.
- **Producto parado:** con cantidad mayor a cero y ninguna venta en el período.
- **En falta:** sin stock, o con un mínimo definido y cantidad igual o menor. Para llegar al doble del mínimo se pide `mínimo × 2 − cantidad` (al menos 1).
- **Variación:** (ingresos − ingresos del período anterior) / ingresos del período anterior; sin ventas antes no hay variación.
- Las filas de ejemplo de las plantillas no cuentan.

## Dónde está en el código

- `domain/model/reporte/ResumenNegocio` (cálculo puro), `PeriodoResumen`, `DatosDeProyecto`, `BloqueGastos`.
- `domain/service/reporte/DatosDeProyectoService` (carga las tablas del proyecto), `domain/useCase/reporte/exportarResumen/*` (caso de uso),
  puerto `GeneradorResumenPdfPort` y adaptador `infrastructure/reporte/GeneradorResumenPdfAdapter` (OpenPDF).
- `ResumenController`. Front: `components/sistema/modales/ModalResumen.tsx`, `api/resumen.ts`, `hooks/reportes`, `lib/descargas.ts`.

## Fuera de alcance por ahora

PDF o Excel por tabla desde la pantalla (el endpoint de Excel `GET /api/bloques/{id}/reporte/descarga` sigue sin pantalla), reportes por
rubro, envío por mail o WhatsApp y resúmenes programados.
