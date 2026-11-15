import { useState } from 'react';
import { CarruselDeRentabilidad } from '@/components/inicio/CarruselDeRentabilidad';
import { Icon } from '@/components/sistema/Icon';
import { GraficoDeBarras } from '@/components/sistema/metricas/GraficoDeBarras';
import { SelectorDePeriodo } from '@/components/sistema/metricas/Selectores';
import { useNumeroAnimado } from '@/components/sistema/metricas/useNumeroAnimado';
import { useResumenInicio } from '@/hooks/reportes/queries';
import { useCuadernoElegido } from '@/hooks/reportes/useCuadernoElegido';
import { formatoPesos } from '@/lib/sistema/formato';
import {
  PERIODOS_DE_VENTAS,
  comparacionConElPeriodoAnterior,
  cuandoFue,
  diaCompleto,
  diasDelGrafico,
  etiquetaDelDia,
  textoUnidades,
  type PeriodoDeVentas,
} from '@/lib/sistema/metricasInicio';
import type { ResumenInicio } from '@/types/resumenInicio';

// El tablero del inicio para quien ya usa Ensambla: lo que viene vendiendo del negocio elegido en el encabezado y,
// debajo, las tarjetas cortas del aviso económico (ventas y ganancia frente a la semana pasada), todo junto. Nunca muestra ceros de
// relleno: si no hay datos, lo dice.
export function MetricasDelInicio() {
  const { cuaderno } = useCuadernoElegido();
  const { resumen, fallo, reintentar } = useResumenInicio(cuaderno?.id);
  if (!cuaderno) return null;

  return (
    <section className="tablero" aria-labelledby="tablero-titulo">
      <h2 id="tablero-titulo" className="inicio-seccion-titulo">
        Cómo te va
      </h2>

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
      ) : !resumen.hayVentas ? (
        <p className="tablero-aviso" role="status">
          Cuando anotes ventas o productos en «{cuaderno.name}», acá vas a ver cómo te va.
        </p>
      ) : (
        <div className="tablero-cuerpo">
          <TarjetaDeVentas resumen={resumen} />
        </div>
      )}
      <CarruselDeRentabilidad />
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
        <SelectorDePeriodo valor={periodo} onCambio={setPeriodo} />
        {ventas.cantidad === 0 ? (
          <>
            <h3>{textos?.sin}</h3>
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
            <h3>{textos?.titulo}</h3>
            <p className="tablero-ventas-monto">
              {conPlata ? formatoPesos(monto) : textoUnidades(ventas.unidades)}
            </p>
            {comparacion && (
              <p
                className="tablero-ventas-comparacion"
                data-sube={ventas.ingresos >= ventas.ingresosAnteriores ? 'true' : 'false'}
              >
                <Icon name={ventas.ingresos >= ventas.ingresosAnteriores ? 'sube' : 'baja'} />
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
        variante="claro"
      />
    </article>
  );
}
