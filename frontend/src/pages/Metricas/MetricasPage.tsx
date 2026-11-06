import { useState, type ReactNode } from 'react';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { varsDeColor } from '@/components/sistema/colores';
import { BarrasHorizontales, GraficoDeBarras } from '@/components/sistema/metricas/GraficoDeBarras';
import { SelectorDeCuaderno, SelectorDePeriodo } from '@/components/sistema/metricas/Selectores';
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
  textoUnidades,
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
                <Panel
                  titulo="Lo que más se vendió"
                  detalle={`Por unidades, ${QUE_PERIODO[mostrado]}.`}
                  color="orange"
                >
                  <BarrasHorizontales
                    titulo="Lo que más se vendió"
                    filas={metricas.masVendidos.map((producto) => ({
                      clave: producto.nombre,
                      nombre: producto.nombre,
                      valor: producto.unidades,
                      texto: textoUnidades(producto.unidades),
                    }))}
                  />
                </Panel>
              )}
              {metricas.stock.productos > 0 && <PanelDeStock metricas={metricas} />}
              {metricas.parados.length > 0 && (
                <Panel
                  titulo="Sin venderse hace 2 semanas"
                  detalle="Tienen stock y no tuvieron ventas: es plata que está quieta."
                  color="blue"
                >
                  <ul className="metricas-lista">
                    {metricas.parados.map((producto) => (
                      <li key={producto.nombre}>
                        <span>{producto.nombre}</span>
                        <strong>
                          {producto.capital != null
                            ? `${formatoPesos(producto.capital)} en stock`
                            : textoUnidades(producto.cantidad)}
                        </strong>
                      </li>
                    ))}
                  </ul>
                </Panel>
              )}
              {(metricas.clientes.top.length > 0 || metricas.clientes.ventasFiadas > 0) && (
                <Panel titulo="Tus clientes" detalle="Quiénes más te compraron." color="petrol">
                  {metricas.clientes.top.length > 0 && (
                    <BarrasHorizontales
                      titulo="Tus clientes"
                      filas={metricas.clientes.top.map((cliente) => ({
                        clave: cliente.nombre,
                        nombre: `${cliente.nombre} · ${cliente.compras} ${cliente.compras === 1 ? 'compra' : 'compras'}`,
                        valor: cliente.ingresos,
                        texto: formatoPesos(cliente.ingresos),
                      }))}
                    />
                  )}
                  {metricas.clientes.ventasFiadas > 0 && (
                    <p className="metricas-nota">
                      Vendiste fiado {formatoPesos(metricas.clientes.fiado)} (
                      {metricas.clientes.ventasFiadas}{' '}
                      {metricas.clientes.ventasFiadas === 1 ? 'venta' : 'ventas'}).
                    </p>
                  )}
                </Panel>
              )}
              {metricas.gastos && (
                <Panel
                  titulo="Tus gastos"
                  detalle={`Lo que anotaste como gasto ${QUE_PERIODO[mostrado]}.`}
                  color="yellow"
                >
                  <p className="metricas-gastos">
                    <span>
                      Gastaste <strong>{formatoPesos(metricas.gastos.total)}</strong>
                    </span>
                    <span>
                      {metricas.gastos.resultado >= 0 ? 'Te quedaron' : 'Te faltaron'}{' '}
                      <strong>{formatoPesos(Math.abs(metricas.gastos.resultado))}</strong>
                    </span>
                  </p>
                  {metricas.gastos.porCategoria.length > 0 && (
                    <BarrasHorizontales
                      titulo="Gastos por categoría"
                      filas={metricas.gastos.porCategoria.map((categoria) => ({
                        clave: categoria.categoria,
                        nombre: categoria.categoria,
                        valor: categoria.total,
                        texto: formatoPesos(categoria.total),
                      }))}
                    />
                  )}
                </Panel>
              )}
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
  if (!metricas.hayVentas) {
    return (
      <p className="metricas-aviso" role="status">
        Todavía no anotaste ventas en «{metricas.sistema}». Cuando lo hagas, acá vas a ver cuánto
        vendés.
      </p>
    );
  }
  return (
    <section className="metricas-resumen" aria-label="Resumen">
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
      <Cifra color="blue" icono="check-circle" titulo="Ventas anotadas">
        <p className="metricas-cifra-valor">{formatoCantidad(ventas.cantidad)}</p>
        <p className="metricas-cifra-dato">{textoUnidades(ventas.unidades)} en total</p>
      </Cifra>
      {periodo !== 'hoy' && (
        <Cifra color="yellow" icono="clock" titulo="Por día, en promedio">
          <p className="metricas-cifra-valor">{formatoPesos(ventas.ingresoPorDia)}</p>
          <p className="metricas-cifra-dato">Contando también los días sin ventas.</p>
        </Cifra>
      )}
    </section>
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
      className={`metricas-cifra ${principal ? 'metricas-cifra-principal' : ''}`.trim()}
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
  children,
}: {
  titulo: string;
  detalle: string;
  color?: Color;
  children: ReactNode;
}) {
  return (
    <section className="metricas-panel" style={varsDeColor(color)} aria-label={titulo}>
      <h2>{titulo}</h2>
      <p className="metricas-panel-detalle">{detalle}</p>
      {children}
    </section>
  );
}

function PanelDeStock({ metricas }: { metricas: Metricas }) {
  const { stock } = metricas;
  return (
    <Panel
      titulo={stock.totalFaltantes > 0 ? 'Se te está acabando' : 'Tu stock'}
      detalle={`${stock.productos} ${stock.productos === 1 ? 'producto' : 'productos'}. A precio de venta valen ${formatoPesos(stock.valorAPrecio)}.`}
      color={stock.totalFaltantes > 0 ? 'coral' : 'petrol'}
    >
      {stock.totalFaltantes === 0 ? (
        <p className="metricas-nota">Ningún producto está agotado ni en su mínimo.</p>
      ) : (
        <ul className="metricas-lista">
          {stock.faltantes.map((producto) => (
            <li key={producto.nombre}>
              <span>{producto.nombre}</span>
              <strong>
                {producto.cantidad <= 0
                  ? 'Agotado'
                  : `Quedan ${formatoCantidad(producto.cantidad)}`}
                {producto.pedir != null && ` · conviene pedir ${formatoCantidad(producto.pedir)}`}
              </strong>
            </li>
          ))}
        </ul>
      )}
      {stock.totalFaltantes > stock.faltantes.length && (
        <p className="metricas-nota">y {stock.totalFaltantes - stock.faltantes.length} más.</p>
      )}
    </Panel>
  );
}
