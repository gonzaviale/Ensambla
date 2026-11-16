import { useState } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { GraficoDeBarras } from '@/components/sistema/metricas/GraficoDeBarras';
import { SelectorDeSistema, SelectorDePeriodo } from '@/components/sistema/metricas/Selectores';
import { useNumeroAnimado } from '@/components/sistema/metricas/useNumeroAnimado';
import { useResumenInicio } from '@/hooks/reportes/queries';
import { useAvisoEconomico } from '@/hooks/reportes/useAvisoEconomico';
import { useSistemaElegido } from '@/hooks/reportes/useSistemaElegido';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { formatoPesos } from '@/lib/sistema/formato';
import { useSistemaStore } from '@/stores/sistemaStore';
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
import type { AvisoEconomico } from '@/types/avisoEconomico';
import type { ResumenInicio } from '@/types/resumenInicio';

// El tablero del inicio para quien ya usa Ensambla: lo que viene vendiendo (lo primero que se mira) y el aviso
// económico, de una sección a elegir. La página ya saluda arriba, en la bienvenida compacta. Nunca muestra ceros de
// relleno: si no hay datos, lo dice.
export function MetricasDelInicio() {
  const { sistemas, sistema, elegir } = useSistemaElegido();
  const { resumen, fallo, reintentar } = useResumenInicio(sistema?.id);
  if (!sistema) return null;

  return (
    <section className="tablero" aria-label="Cómo te va">
      <div className="tablero-cabecera">
        <div className="tablero-saludo">
          <p className="tablero-sistema">
            <span>Así viene</span>
            <SelectorDeSistema sistemas={sistemas} sistema={sistema} onCambio={elegir} />
          </p>
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
          Cuando anotes ventas o productos en «{sistema.name}», acá vas a ver cómo te va.
        </p>
      ) : (
        <div className="tablero-cuerpo">
          {resumen.hayVentas && <TarjetaDeVentas resumen={resumen} />}
          <TarjetaDeAvisoEconomico sistemaId={sistema.id} sistema={sistema.name} />
        </div>
      )}
    </section>
  );
}

type PropsDeTarjeta = { resumen: ResumenInicio };

const PRIORIDAD: Record<AvisoEconomico['prioridad'], string> = {
  ALTA: 'Prioridad alta',
  MEDIA: 'Prioridad media',
  BAJA: 'Para mirar',
};

const conSigno = (valor: number) => `${valor > 0 ? '+' : valor < 0 ? '−' : ''}${Math.abs(valor)} %`;

// El aviso económico de la sección: esta semana frente a la anterior. La prioridad a un costado, qué cambió, los
// números que lo muestran y qué hacer. Cuando el negocio mejora también avisa, para que se sepa qué está andando bien.
function TarjetaDeAvisoEconomico({ sistemaId, sistema }: { sistemaId: string; sistema: string }) {
  const { aviso } = useAvisoEconomico(sistemaId);
  const navegacion = useNavegacion();
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  if (!aviso) return null;
  const irAObjetivos = () => void navegacion.irAObjetivos();
  const mejora = aviso.tendencia === 'MEJORA';
  return (
    <article
      className="tablero-aviso-sami"
      data-prioridad={aviso.prioridad}
      data-tendencia={aviso.tendencia}
      aria-label="Aviso económico"
    >
      <div className="tablero-aviso-sami-prioridad">
        <Icon name={mejora ? 'up' : 'chart'} />
        <span>{mejora ? 'Vas mejorando' : PRIORIDAD[aviso.prioridad]}</span>
      </div>
      <div className="tablero-aviso-sami-contenido">
        <p className="tablero-aviso-sami-etiqueta">
          {aviso.categoria} · {sistema}
        </p>
        <h3>{aviso.titulo}</h3>
        <p className="tablero-aviso-sami-detalle">{aviso.explicacion}</p>
        <dl className="tablero-aviso-sami-evidencias">
          <div>
            <dt>Ventas</dt>
            <dd>{conSigno(aviso.variacionVentas)}</dd>
          </div>
          <div>
            <dt>Ganancia</dt>
            <dd>
              {aviso.variacionResultadoBruto === null
                ? 'Sin costos'
                : conSigno(aviso.variacionResultadoBruto)}
            </dd>
          </div>
          <div>
            <dt>Productos involucrados</dt>
            <dd>
              {aviso.productos.length > 0 ? (
                <button
                  type="button"
                  className="tablero-aviso-sami-evidencia-boton"
                  aria-haspopup="dialog"
                  onClick={() =>
                    abrirModal({ tipo: 'productosDelAviso', productos: aviso.productos })
                  }
                >
                  {aviso.productos.length}
                  <Icon name="arrow" />
                </button>
              ) : (
                0
              )}
            </dd>
          </div>
        </dl>
        <div className="tablero-aviso-sami-acciones">
          {mejora ? (
            <button
              type="button"
              className="tablero-aviso-sami-boton principal"
              onClick={irAObjetivos}
            >
              Ver mis objetivos
            </button>
          ) : (
            <>
              <button
                type="button"
                className="tablero-aviso-sami-boton principal"
                onClick={irAObjetivos}
              >
                Entender por qué
              </button>
              <button type="button" className="tablero-aviso-sami-boton" onClick={irAObjetivos}>
                Probar una solución
              </button>
            </>
          )}
        </div>
      </div>
    </article>
  );
}

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
