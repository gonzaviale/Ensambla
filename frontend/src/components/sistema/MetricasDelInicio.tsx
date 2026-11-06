import { useState, type ReactNode } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { varsDeColor } from '@/components/sistema/colores';
import { GraficoDeBarras } from '@/components/sistema/metricas/GraficoDeBarras';
import { SelectorDeCuaderno, SelectorDePeriodo } from '@/components/sistema/metricas/Selectores';
import { useNumeroAnimado } from '@/components/sistema/metricas/useNumeroAnimado';
import { useResumenInicio } from '@/hooks/reportes/queries';
import { useCuadernoElegido } from '@/hooks/reportes/useCuadernoElegido';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { formatoPesos } from '@/lib/sistema/formato';
import {
  PERIODOS_DE_VENTAS,
  comparacionConElPeriodoAnterior,
  cuandoFue,
  diaCompleto,
  diasDelGrafico,
  etiquetaDelDia,
  formatoCantidad,
  textoUnidades,
  type PeriodoDeVentas,
} from '@/lib/sistema/metricasInicio';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type { Color, Sistema } from '@/lib/sistema/tipos';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { ResumenInicio } from '@/types/resumenInicio';

const COLORES_DE_CUADERNO: Color[] = ['petrol', 'orange', 'blue', 'coral', 'yellow'];

// El tablero del inicio para quien ya usa Ensambla: el saludo, lo que viene vendiendo (lo primero que se mira), lo
// que más sale, lo que se acaba y lo que está quieto. Nunca muestra ceros de relleno: si no hay datos, lo dice.
export function MetricasDelInicio({
  nombre,
  onPrimeraVez,
}: {
  nombre: string;
  onPrimeraVez: () => void;
}) {
  const navegacion = useNavegacion();
  const { cuadernos, cuaderno, elegir } = useCuadernoElegido();
  const { resumen, fallo, reintentar } = useResumenInicio(cuaderno?.id);
  if (!cuaderno) return null;

  return (
    <section className="tablero" aria-label="Cómo te va">
      <div className="tablero-cabecera">
        <div className="tablero-saludo">
          <h1>Hola, {nombre}.</h1>
          <p className="tablero-cuaderno">
            <span>Así viene</span>
            <SelectorDeCuaderno cuadernos={cuadernos} cuaderno={cuaderno} onCambio={elegir} />
          </p>
        </div>
        <div className="tablero-acciones">
          <button type="button" className="tablero-primera-vez" onClick={onPrimeraVez}>
            <Icon name="help" />
            Primera vez
          </button>
          <button
            type="button"
            className="tablero-ir-metricas"
            onClick={() => void navegacion.irAMetricas()}
          >
            <Icon name="chart" />
            Mis métricas
          </button>
        </div>
      </div>

      {fallo ? (
        <p className="tablero-aviso" role="status">
          No pudimos traer tus números ahora.{' '}
          <button type="button" onClick={() => void reintentar()}>
            Probar de nuevo
          </button>
        </p>
      ) : !resumen ? (
        <div className="tablero-cargando">
          <span className="sr-only" role="status">
            Mirando tus números…
          </span>
          <span className="tablero-cargando-principal" />
          <span />
          <span />
          <span />
        </div>
      ) : !resumen.hayVentas && !resumen.hayProductos ? (
        <p className="tablero-aviso" role="status">
          Cuando anotes ventas o productos en «{cuaderno.name}», acá vas a ver cómo te va.
        </p>
      ) : (
        <div className="tablero-cuerpo">
          {resumen.hayVentas && <TarjetaDeVentas resumen={resumen} />}
          <div className="tablero-tarjetas">
            {resumen.masVendido && <TarjetaMasVendido resumen={resumen} />}
            {resumen.hayProductos && <TarjetaDeFaltantes resumen={resumen} />}
            {resumen.parados.length > 0 && <TarjetaDeParados resumen={resumen} />}
          </div>
        </div>
      )}
    </section>
  );
}

type PropsDeTarjeta = { resumen: ResumenInicio };

// Lo que se viene vendiendo: el número grande, la comparación y las ventas día por día. Arranca en la semana.
function TarjetaDeVentas({ resumen }: PropsDeTarjeta) {
  const [periodo, setPeriodo] = useState<PeriodoDeVentas>('semana');
  const { ultimoDiaConVentas, hoy } = resumen;
  const ventas = resumen.ventas[periodo];
  const textos = PERIODOS_DE_VENTAS.find((opcion) => opcion.clave === periodo);
  const monto = useNumeroAnimado(ventas.ingresos);
  const conPlata = ventas.ingresos > 0;
  const comparacion = conPlata
    ? comparacionConElPeriodoAnterior(periodo, ventas.ingresos, ventas.ingresosAnteriores, hoy)
    : null;
  const barras = resumen.ventasPorDia.slice(-diasDelGrafico(periodo)).map((dia, indice, todos) => ({
    clave: dia.fecha,
    etiqueta:
      periodo !== 'mes' || indice % 5 === 4 || indice === todos.length - 1
        ? etiquetaDelDia(dia.fecha)
        : '',
    nombre: diaCompleto(dia.fecha),
    valor: dia.ingresos,
    detalle: formatoPesos(dia.ingresos),
    destacada: dia.fecha === hoy,
  }));

  return (
    <article className="tablero-ventas">
      <div className="tablero-ventas-texto">
        <SelectorDePeriodo valor={periodo} onCambio={setPeriodo} variante="oscuro" />
        {ventas.cantidad === 0 ? (
          <>
            <h2>{textos?.sin}</h2>
            {ultimoDiaConVentas && (
              <p className="tablero-ventas-dato">
                Lo último que anotaste fue {cuandoFue(ultimoDiaConVentas.fecha, hoy)}:{' '}
                {ultimoDiaConVentas.ingresos > 0
                  ? formatoPesos(ultimoDiaConVentas.ingresos)
                  : 'ventas sin precio'}
                .
              </p>
            )}
          </>
        ) : (
          <>
            <h2>{textos?.titulo}</h2>
            <p className="tablero-ventas-monto">
              {conPlata ? formatoPesos(monto) : textoUnidades(ventas.unidades)}
            </p>
            {comparacion && (
              <p
                className="tablero-ventas-comparacion"
                data-sube={ventas.ingresos >= ventas.ingresosAnteriores ? 'true' : 'false'}
              >
                <Icon name={ventas.ingresos >= ventas.ingresosAnteriores ? 'up' : 'down'} />
                {comparacion}
              </p>
            )}
            <p className="tablero-ventas-dato">
              {ventas.cantidad === 1 ? '1 venta anotada' : `${ventas.cantidad} ventas anotadas`} ·{' '}
              {textoUnidades(ventas.unidades)}
            </p>
            {!conPlata && (
              <p className="tablero-ventas-dato">
                Esas ventas no tienen precio, así que no puedo sumar la plata.
              </p>
            )}
            {ventas.ventasFiadas > 0 && (
              <p className="tablero-ventas-dato">
                Vendiste fiado {formatoPesos(ventas.fiado)} ({ventas.ventasFiadas}{' '}
                {ventas.ventasFiadas === 1 ? 'venta' : 'ventas'}).
              </p>
            )}
            {conPlata && ventas.sinPrecio > 0 && (
              <p className="tablero-ventas-nota">
                {ventas.sinPrecio === 1
                  ? '1 venta sin precio no está sumada.'
                  : `${ventas.sinPrecio} ventas sin precio no están sumadas.`}
              </p>
            )}
          </>
        )}
      </div>
      <GraficoDeBarras
        key={periodo}
        barras={barras}
        titulo={
          periodo === 'mes' ? 'Ventas de los últimos 30 días' : 'Ventas de los últimos 7 días'
        }
        variante="oscuro"
      />
    </article>
  );
}

function Tarjeta({
  color,
  icono,
  titulo,
  children,
}: {
  color: Color;
  icono: NombreDeIcono;
  titulo: string;
  children: ReactNode;
}) {
  return (
    <article className="tablero-tarjeta" style={varsDeColor(color)} data-color={color}>
      <header>
        <span className="tablero-tarjeta-icono">
          <Icon name={icono} />
        </span>
        <h3>{titulo}</h3>
      </header>
      {children}
    </article>
  );
}

function TarjetaMasVendido({ resumen }: PropsDeTarjeta) {
  const producto = resumen.masVendido;
  if (!producto) return null;
  return (
    <Tarjeta color="orange" icono="spark" titulo="Lo que más salió este mes">
      <p className="tablero-tarjeta-valor">{producto.nombre}</p>
      <p className="tablero-tarjeta-dato">
        {textoUnidades(producto.unidades)} en los últimos 30 días
      </p>
      {producto.ganancia != null && (
        <p className="tablero-tarjeta-nota">Te dejó cerca de {formatoPesos(producto.ganancia)}.</p>
      )}
    </Tarjeta>
  );
}

function TarjetaDeFaltantes({ resumen }: PropsDeTarjeta) {
  const { total, primeros } = resumen.faltantes;
  if (total === 0) {
    return (
      <Tarjeta color="yellow" icono="check-circle" titulo="Tu stock">
        <p className="tablero-tarjeta-valor">Nada se acabó</p>
        <p className="tablero-tarjeta-dato">Ningún producto está agotado ni en su mínimo.</p>
      </Tarjeta>
    );
  }
  return (
    <Tarjeta color="coral" icono="box" titulo="Se te está acabando">
      <p className="tablero-tarjeta-valor">{total === 1 ? '1 producto' : `${total} productos`}</p>
      <ul className="tablero-tarjeta-lista">
        {primeros.map((producto) => (
          <li key={producto.nombre}>
            <span>{producto.nombre}</span>
            <strong>
              {producto.cantidad <= 0 ? 'Agotado' : `Quedan ${formatoCantidad(producto.cantidad)}`}
            </strong>
          </li>
        ))}
      </ul>
      {total > primeros.length && (
        <p className="tablero-tarjeta-nota">y {total - primeros.length} más.</p>
      )}
    </Tarjeta>
  );
}

function TarjetaDeParados({ resumen }: PropsDeTarjeta) {
  return (
    <Tarjeta color="blue" icono="clock" titulo="Sin venderse hace 2 semanas">
      <ul className="tablero-tarjeta-lista">
        {resumen.parados.map((producto) => (
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
    </Tarjeta>
  );
}

// Los últimos cuadernos que se tocaron, para entrar directo; «Ver todos» lleva a «Mis cuadernos».
export function CuadernosRecientes() {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const navegacion = useNavegacion();
  const recientes = [...sistemas]
    .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
    .slice(0, 4);
  if (recientes.length === 0) return null;
  return (
    <section className="inicio-recientes" aria-label="Tus cuadernos">
      <div className="inicio-recientes-cabecera">
        <h2>Tus cuadernos</h2>
        <button
          type="button"
          className="inicio-recientes-todos"
          onClick={() => void navegacion.irAMisSistemas()}
        >
          Ver todos <Icon name="arrow" />
        </button>
      </div>
      <div className="inicio-recientes-lista">
        {recientes.map((sistema: Sistema, indice) => {
          const color = COLORES_DE_CUADERNO[indice % COLORES_DE_CUADERNO.length] ?? 'petrol';
          return (
            <button
              key={sistema.id}
              type="button"
              className="inicio-reciente"
              style={varsDeColor(color)}
              data-color={color}
              onClick={() => void navegacion.abrirSistema(sistema.id)}
            >
              <span className="inicio-reciente-icono">
                <Icon name="folder" />
              </span>
              <span className="inicio-reciente-nombre">{sistema.name}</span>
              <Icon name="arrow" />
            </button>
          );
        })}
      </div>
    </section>
  );
}
