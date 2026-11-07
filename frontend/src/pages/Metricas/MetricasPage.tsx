import { useState, type ReactNode } from 'react';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { varsDeColor } from '@/components/sistema/colores';
import { GraficoDeBarras } from '@/components/sistema/metricas/GraficoDeBarras';
import { SelectorDeCuaderno, SelectorDePeriodo } from '@/components/sistema/metricas/Selectores';
import { TablaDeDetalle } from '@/components/sistema/metricas/TablaDeDetalle';
import { useNumeroAnimado } from '@/components/sistema/metricas/useNumeroAnimado';
import { useMetricas } from '@/hooks/reportes/queries';
import { useCuadernoElegido } from '@/hooks/reportes/useCuadernoElegido';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { formatoPesos } from '@/lib/sistema/formato';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import {
  PERIODOS_DE_VENTAS,
  comparacionConElPeriodoAnterior,
  diaCompleto,
  etiquetaDelDia,
  formatoCantidad,
  type PeriodoDeVentas,
} from '@/lib/sistema/metricasInicio';
import type { Color } from '@/lib/sistema/tipos';
import type { Metricas } from '@/types/metricas';

const QUE_PERIODO: Record<PeriodoDeVentas, string> = {
  hoy: 'hoy',
  semana: 'en los últimos 7 días',
  mes: 'en los últimos 30 días',
};

// «Mis métricas»: los números del cuaderno en detalle, para hoy, la semana o el mes. Arriba lo importante en
// tarjetas de color; abajo los gráficos y las listas para entender qué pasó. Son las mismas cuentas que el PDF.
export function MetricasPage() {
  useTituloPagina('Mis métricas');
  const navegacion = useNavegacion();
  const { cuadernos, cuaderno, elegir } = useCuadernoElegido();
  const [periodo, setPeriodo] = useState<PeriodoDeVentas>('semana');
  const { metricas, cargando, actualizando, fallo, reintentar } = useMetricas(
    cuaderno?.id,
    periodo,
  );

  // Mientras llegan los números del período nuevo se siguen viendo los anteriores: los títulos tienen que
  // nombrar el período de esos números, no el del botón recién tocado.
  const mostrado: PeriodoDeVentas = metricas
    ? (metricas.periodo.toLowerCase() as PeriodoDeVentas)
    : periodo;

  return (
    <>
      <CabeceraComun active="metricas" />
      <main id="main" tabIndex={-1} className="home metricas reveal">
        <header className="metricas-cabecera">
          <div>
            <h1>Mis métricas</h1>
            {cuaderno && (
              <p className="metricas-cuaderno">
                <span>De</span>
                <SelectorDeCuaderno cuadernos={cuadernos} cuaderno={cuaderno} onCambio={elegir} />
              </p>
            )}
          </div>
          {cuaderno && <SelectorDePeriodo valor={periodo} onCambio={setPeriodo} />}
        </header>

        {!cuaderno ? (
          <div className="metricas-vacio">
            <span className="metricas-vacio-icono">
              <Icon name="chart" />
            </span>
            <h2>Todavía no hay números para mostrar</h2>
            <p>Cuando anotes ventas o productos en un cuaderno, acá vas a ver cómo te va.</p>
            <button
              type="button"
              className="btn primary"
              onClick={() => void navegacion.irAMisSistemas()}
            >
              Ir a mis cuadernos
            </button>
          </div>
        ) : fallo && !metricas ? (
          <p className="metricas-aviso" role="status">
            No pudimos traer tus métricas.{' '}
            <button type="button" onClick={() => void reintentar()}>
              Probar de nuevo
            </button>
          </p>
        ) : cargando || !metricas ? (
          <div className="metricas-cargando" role="status">
            <span className="sr-only">Mirando tus números…</span>
            <span />
            <span />
            <span />
            <span />
          </div>
        ) : (
          <div className="metricas-contenido" data-actualizando={actualizando ? 'true' : undefined}>
            <Resumen metricas={metricas} periodo={mostrado} />
            <div className="metricas-grilla">
              {mostrado !== 'hoy' && metricas.hayVentas && (
                <Panel
                  titulo="Día por día"
                  detalle="Lo que entró cada día. Tocá una barra para ver el número."
                  ancho
                >
                  <GraficoDeBarras
                    key={mostrado}
                    titulo={`Ventas ${QUE_PERIODO[mostrado]}`}
                    barras={metricas.porDia.map((dia, indice, todos) => ({
                      clave: dia.fecha,
                      etiqueta:
                        mostrado === 'semana' || indice % 5 === 4 || indice === todos.length - 1
                          ? etiquetaDelDia(dia.fecha)
                          : '',
                      nombre: diaCompleto(dia.fecha),
                      valor: dia.ingresos,
                      detalle: formatoPesos(dia.ingresos),
                      destacada: indice === todos.length - 1,
                    }))}
                  />
                </Panel>
              )}
              {metricas.masVendidos.length > 0 && (
                <PanelDeMasVendidos metricas={metricas} periodo={mostrado} />
              )}
              {metricas.stock.productos > 0 && <PanelDeStock metricas={metricas} />}
              {metricas.parados.length > 0 && <PanelDeParados metricas={metricas} />}
              {(metricas.clientes.top.length > 0 || metricas.clientes.ventasFiadas > 0) && (
                <PanelDeClientes metricas={metricas} periodo={mostrado} />
              )}
              {metricas.gastos && <PanelDeGastos gastos={metricas.gastos} periodo={mostrado} />}
              {metricas.zona && <PanelDeZona zona={metricas.zona} />}
              <PanelDeAvisos avisos={metricas.avisos} periodo={mostrado} />
            </div>
          </div>
        )}
        <PageFooter />
      </main>
    </>
  );
}

// Las cuatro tarjetas de arriba, cada una de un color lleno: lo vendido, lo que quedó, cuántas ventas y el promedio.
function Resumen({ metricas, periodo }: { metricas: Metricas; periodo: PeriodoDeVentas }) {
  const { ventas } = metricas;
  const vendido = useNumeroAnimado(ventas.ingresos);
  const titulo =
    PERIODOS_DE_VENTAS.find((opcion) => opcion.clave === periodo)?.titulo ?? 'Vendiste';
  const comparacion = comparacionConElPeriodoAnterior(
    periodo,
    ventas.ingresos,
    ventas.ingresosAnteriores,
    metricas.hasta,
  );
  const { gastos, stock, clientes } = metricas;
  const hayOtrasCifras = gastos != null || stock.productos > 0;
  const aviso = (
    <p className="metricas-aviso" role="status">
      Todavía no anotaste ventas en «{metricas.sistema}». Cuando lo hagas, acá vas a ver cuánto
      vendés.
    </p>
  );
  if (!metricas.hayVentas && !hayOtrasCifras) return aviso;
  return (
    <>
      {!metricas.hayVentas && aviso}
      <section className="metricas-resumen" aria-label="Resumen">
        {metricas.hayVentas && (
          <>
            <Cifra color="petrol" icono="wallet" titulo={titulo} principal>
              <p className="metricas-cifra-valor">{formatoPesos(vendido)}</p>
              {comparacion && (
                <p className="metricas-cifra-dato">
                  <Icon name={ventas.ingresos >= ventas.ingresosAnteriores ? 'up' : 'down'} />
                  {comparacion}
                </p>
              )}
              {ventas.sinPrecio > 0 && (
                <p className="metricas-cifra-nota">
                  {ventas.sinPrecio === 1
                    ? '1 venta sin precio no está sumada.'
                    : `${ventas.sinPrecio} ventas sin precio no están sumadas.`}
                </p>
              )}
            </Cifra>
            <Cifra color="orange" icono="spark" titulo="Te quedó, más o menos">
              {ventas.gananciaEstimada != null ? (
                <>
                  <p className="metricas-cifra-valor">{formatoPesos(ventas.gananciaEstimada)}</p>
                  <p className="metricas-cifra-dato">
                    Lo vendido menos lo que te costó, con el costo de hoy.
                  </p>
                </>
              ) : (
                <p className="metricas-cifra-dato">
                  Cargá el costo de tus productos para ver cuánto te queda.
                </p>
              )}
            </Cifra>
            <Cifra color="blue" icono="check-circle" titulo="Unidades vendidas">
              <p className="metricas-cifra-valor">{formatoCantidad(ventas.unidades)}</p>
              <p className="metricas-cifra-dato">
                {ventas.cantidad === 1 ? '1 venta anotada' : `${ventas.cantidad} ventas anotadas`}
              </p>
            </Cifra>
            {periodo !== 'hoy' && (
              <Cifra color="yellow" icono="clock" titulo="Por día, en promedio">
                <p className="metricas-cifra-valor">{formatoPesos(ventas.ingresoPorDia)}</p>
                <p className="metricas-cifra-dato">Contando también los días sin ventas.</p>
              </Cifra>
            )}
          </>
        )}
        {gastos && (
          <>
            <Cifra color="blue" icono="wallet" titulo="Gastos">
              <p className="metricas-cifra-valor">{formatoPesos(gastos.total)}</p>
              <p className="metricas-cifra-dato">
                {gastos.cantidad === 1 ? '1 gasto anotado' : `${gastos.cantidad} gastos anotados`}
              </p>
            </Cifra>
            <Cifra color="petrol" icono="chart" titulo="Resultado">
              <p className="metricas-cifra-valor">{formatoPesos(gastos.resultado)}</p>
              <p className="metricas-cifra-dato">Ingresos menos gastos</p>
            </Cifra>
          </>
        )}
        {metricas.hayVentas && clientes.ventasFiadas > 0 && (
          <Cifra color="yellow" icono="note" titulo="Vendido a fiado">
            <p className="metricas-cifra-valor">{formatoPesos(clientes.fiado)}</p>
            <p className="metricas-cifra-dato">
              {clientes.ventasFiadas === 1 ? '1 venta' : `${clientes.ventasFiadas} ventas`}
            </p>
          </Cifra>
        )}
        {stock.productos > 0 && (
          <Cifra color="orange" icono="box" titulo="Valor del stock">
            <p className="metricas-cifra-valor">{formatoPesos(stock.valorACosto)}</p>
            <p className="metricas-cifra-dato">A costo</p>
          </Cifra>
        )}
      </section>
    </>
  );
}

function Cifra({
  color,
  icono,
  titulo,
  principal = false,
  children,
}: {
  color: Color;
  icono: NombreDeIcono;
  titulo: string;
  principal?: boolean;
  children: ReactNode;
}) {
  return (
    <article
      className={`metricas-cifra ${principal || color === 'petrol' ? 'metricas-cifra-principal' : ''}`.trim()}
      style={varsDeColor(color)}
      data-color={color}
    >
      <header>
        <span className="metricas-cifra-icono">
          <Icon name={icono} />
        </span>
        <h2>{titulo}</h2>
      </header>
      {children}
    </article>
  );
}

function Panel({
  titulo,
  detalle,
  color = 'petrol',
  ancho = false,
  children,
}: {
  titulo: string;
  detalle: string;
  color?: Color;
  /** Ocupa las dos columnas: para lo que tiene muchas columnas o muchos días. */
  ancho?: boolean;
  children: ReactNode;
}) {
  return (
    <section
      className={ancho ? 'metricas-panel metricas-panel-ancho' : 'metricas-panel'}
      style={varsDeColor(color)}
      data-color={color}
      aria-label={titulo}
    >
      <h2>{titulo}</h2>
      <p className="metricas-panel-detalle">{detalle}</p>
      {children}
    </section>
  );
}

const SIN_DATO = '-';

const pesosOGuion = (valor: number | null | undefined) =>
  valor == null ? SIN_DATO : formatoPesos(valor);

const proporcionDe = (valor: number, valores: number[]) => {
  const maximo = Math.max(0, ...valores);
  return maximo > 0 ? valor / maximo : 0;
};

// Lo que más salió, con lo que entró y lo que quedó de cada producto (la ganancia usa el costo de hoy).
function PanelDeMasVendidos({
  metricas,
  periodo,
}: {
  metricas: Metricas;
  periodo: PeriodoDeVentas;
}) {
  const unidades = metricas.masVendidos.map((producto) => producto.unidades);
  return (
    <Panel
      titulo="Lo que más se vendió"
      detalle={`Por unidades, ${QUE_PERIODO[periodo]}. La ganancia usa el costo actual de cada producto.`}
      color="orange"
      ancho
    >
      <TablaDeDetalle
        titulo="Lo que más se vendió"
        columnas={[
          { titulo: 'Producto' },
          { titulo: 'Unidades' },
          { titulo: 'Ingresos' },
          { titulo: 'Ganancia' },
        ]}
        filas={metricas.masVendidos.map((producto) => ({
          clave: producto.nombre,
          barra: proporcionDe(producto.unidades, unidades),
          celdas: [
            producto.nombre,
            formatoCantidad(producto.unidades),
            producto.ingresos > 0 ? formatoPesos(producto.ingresos) : SIN_DATO,
            pesosOGuion(producto.ganancia),
          ],
        }))}
      />
    </Panel>
  );
}

// Lo que se acaba, con lo que queda, el mínimo que se puso y cuánto falta para llegar al doble de ese mínimo.
function PanelDeStock({ metricas }: { metricas: Metricas }) {
  const { stock } = metricas;
  const hayFaltantes = stock.totalFaltantes > 0;
  return (
    <Panel
      titulo={hayFaltantes ? 'Se te está acabando' : 'Tu stock está al día'}
      detalle={`${stock.productos} ${stock.productos === 1 ? 'producto' : 'productos'}. Valen ${formatoPesos(stock.valorACosto)} a costo y ${formatoPesos(stock.valorAPrecio)} a precio de venta.`}
      color={hayFaltantes ? 'coral' : 'petrol'}
    >
      {hayFaltantes ? (
        <TablaDeDetalle
          titulo="Productos que se acaban"
          columnas={[
            { titulo: 'Producto' },
            { titulo: 'Quedan' },
            { titulo: 'Mínimo' },
            { titulo: 'Para llegar al doble' },
          ]}
          filas={stock.faltantes.map((producto) => ({
            clave: producto.nombre,
            celdas: [
              producto.nombre,
              producto.cantidad <= 0 ? 'Agotado' : formatoCantidad(producto.cantidad),
              producto.minimo == null ? SIN_DATO : formatoCantidad(producto.minimo),
              producto.pedir == null ? SIN_DATO : formatoCantidad(producto.pedir),
            ],
          }))}
        />
      ) : (
        <p className="metricas-nota">Ningún producto está agotado ni por debajo de su mínimo.</p>
      )}
      {stock.totalFaltantes > stock.faltantes.length && (
        <p className="metricas-nota">Y {stock.totalFaltantes - stock.faltantes.length} más.</p>
      )}
    </Panel>
  );
}

// Lo que tiene stock y no se vende hace dos semanas: la plata parada, a costo.
function PanelDeParados({ metricas }: { metricas: Metricas }) {
  const capitales = metricas.parados.map((producto) => producto.capital ?? 0);
  const conCosto = metricas.parados.some((producto) => producto.capital != null);
  return (
    <Panel
      titulo="Sin venderse hace 2 semanas"
      detalle="Tienen stock y no tuvieron ventas: es plata que está quieta."
      color="blue"
    >
      <TablaDeDetalle
        titulo="Productos parados"
        columnas={[
          { titulo: 'Producto' },
          { titulo: 'En stock' },
          { titulo: 'Plata parada (a costo)' },
        ]}
        filas={metricas.parados.map((producto) => ({
          clave: producto.nombre,
          barra: conCosto ? proporcionDe(producto.capital ?? 0, capitales) : undefined,
          celdas: [
            producto.nombre,
            formatoCantidad(producto.cantidad),
            pesosOGuion(producto.capital),
          ],
        }))}
      />
      {conCosto && (
        <p className="metricas-nota">
          En total hay {formatoPesos(capitales.reduce((suma, capital) => suma + capital, 0))}{' '}
          parados.
        </p>
      )}
    </Panel>
  );
}

// Quiénes más compraron y cuánto se vendió a fiado (lo vendido, no lo que todavía se debe).
function PanelDeClientes({ metricas, periodo }: { metricas: Metricas; periodo: PeriodoDeVentas }) {
  const { top, fiado, ventasFiadas } = metricas.clientes;
  const ingresos = top.map((cliente) => cliente.ingresos);
  return (
    <Panel
      titulo="Tus clientes"
      detalle={`Quiénes más te compraron ${QUE_PERIODO[periodo]}.`}
      color="petrol"
    >
      {top.length > 0 && (
        <TablaDeDetalle
          titulo="Tus clientes"
          columnas={[{ titulo: 'Cliente' }, { titulo: 'Compras' }, { titulo: 'Ingresos' }]}
          filas={top.map((cliente) => ({
            clave: cliente.nombre,
            barra: proporcionDe(cliente.ingresos, ingresos),
            celdas: [cliente.nombre, String(cliente.compras), formatoPesos(cliente.ingresos)],
          }))}
        />
      )}
      {ventasFiadas > 0 && (
        <p className="metricas-nota">
          Vendiste fiado {formatoPesos(fiado)} ({ventasFiadas}{' '}
          {ventasFiadas === 1 ? 'venta' : 'ventas'}).
        </p>
      )}
    </Panel>
  );
}

// Lo gastado en el período, por categoría, y lo que queda de los ingresos.
function PanelDeGastos({
  gastos,
  periodo,
}: {
  gastos: NonNullable<Metricas['gastos']>;
  periodo: PeriodoDeVentas;
}) {
  const totales = gastos.porCategoria.map((categoria) => categoria.total);
  return (
    <Panel
      titulo="Tus gastos"
      detalle={`Lo que anotaste como gasto ${QUE_PERIODO[periodo]}.`}
      color="yellow"
    >
      <p className="metricas-gastos">
        <span>
          Gastaste <strong>{formatoPesos(gastos.total)}</strong>{' '}
          <small>
            ({gastos.cantidad} {gastos.cantidad === 1 ? 'gasto anotado' : 'gastos anotados'})
          </small>
        </span>
        <span>
          {gastos.resultado >= 0 ? 'Te quedaron' : 'Te faltaron'}{' '}
          <strong>{formatoPesos(Math.abs(gastos.resultado))}</strong>
        </span>
      </p>
      {gastos.porCategoria.length > 0 ? (
        <TablaDeDetalle
          titulo="Gastos por categoría"
          columnas={[{ titulo: 'Categoría' }, { titulo: 'Total' }]}
          filas={gastos.porCategoria.map((categoria) => ({
            clave: categoria.categoria,
            barra: proporcionDe(categoria.total, totales),
            celdas: [categoria.categoria, formatoPesos(categoria.total)],
          }))}
        />
      ) : (
        <p className="metricas-nota">No anotaste gastos en el período.</p>
      )}
    </Panel>
  );
}

// Cómo están sus precios frente a los de la zona. Son datos agregados y anónimos de otros comercios.
function PanelDeZona({ zona }: { zona: NonNullable<Metricas['zona']> }) {
  return (
    <Panel
      titulo={`Tus precios frente a la zona (${zona.nombre})`}
      detalle={`${zona.debajo} por debajo del rango, ${zona.dentro} dentro y ${zona.encima} por encima. Son datos agregados y anónimos de ${zona.comercios} comercios.`}
      color="blue"
      ancho
    >
      {zona.desalineados.length > 0 ? (
        <TablaDeDetalle
          titulo="Precios fuera del rango de la zona"
          columnas={[
            { titulo: 'Producto' },
            { titulo: 'Tu precio' },
            { titulo: 'Rango de la zona' },
            { titulo: 'Diferencia' },
          ]}
          filas={zona.desalineados.map((producto) => ({
            clave: producto.nombre,
            celdas: [
              producto.nombre,
              formatoPesos(producto.precio),
              `${formatoPesos(producto.minimo)} a ${formatoPesos(producto.maximo)}`,
              `${producto.porcentaje > 0 ? '+' : ''}${producto.porcentaje} %`,
            ],
          }))}
        />
      ) : (
        <p className="metricas-nota">
          Todos tus precios comparables están dentro del rango de la zona.
        </p>
      )}
    </Panel>
  );
}

const NOMBRE_DE_AVISO: Record<Metricas['avisos']['pendientes'][number]['tipo'], string> = {
  STOCK: 'Stock',
  PRECIO_ZONA: 'Precio frente a la zona',
  ESTANCADO: 'Productos sin movimiento',
  MUY_VENDIDO: 'Productos que se venden mucho',
  COMBO: 'Combos',
  CONSUMO_ZONA: 'Lo que pide la zona',
  REVISAR_PRECIO: 'Precios para revisar',
};

// Lo que Sami le avisó y lo que hizo con esos avisos. El impacto es una comparación, no una causa.
function PanelDeAvisos({
  avisos,
  periodo,
}: {
  avisos: Metricas['avisos'];
  periodo: PeriodoDeVentas;
}) {
  const { totalPendientes, pendientes, aplicados, descartados, medidos, ingresoSemanalDelta } =
    avisos;
  const pendientesTexto = totalPendientes === 1 ? '1 pendiente' : `${totalPendientes} pendientes`;
  const cuando =
    periodo === 'hoy'
      ? 'Hoy'
      : periodo === 'semana'
        ? 'En los últimos 7 días'
        : 'En los últimos 30 días';
  return (
    <Panel
      titulo="Avisos de Sami"
      detalle={`${pendientesTexto}. ${cuando} aplicaste ${aplicados} y descartaste ${descartados}.`}
      color="orange"
    >
      {pendientes.length > 0 ? (
        <TablaDeDetalle
          titulo="Avisos pendientes"
          columnas={[{ titulo: 'Qué conviene mirar' }, { titulo: 'Avisos' }]}
          filas={pendientes.map((pendiente) => ({
            clave: pendiente.tipo,
            barra: proporcionDe(
              pendiente.cantidad,
              pendientes.map((otro) => otro.cantidad),
            ),
            celdas: [NOMBRE_DE_AVISO[pendiente.tipo], String(pendiente.cantidad)],
          }))}
        />
      ) : (
        <p className="metricas-nota">No tenés avisos pendientes.</p>
      )}
      {medidos > 0 && (
        <p className="metricas-nota">
          De los avisos aplicados ya medimos {medidos}: el ingreso semanal cambió{' '}
          {formatoPesos(ingresoSemanalDelta)} entre antes y después. Es una comparación, no prueba
          que el aviso sea la causa: también influyen la época del año, los feriados o el faltante.
        </p>
      )}
    </Panel>
  );
}
