import { useMemo } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { usePanelDelNegocio } from '@/hooks/panel/queries';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { formatoPesos } from '@/lib/objetivos';
import { sistemaDelPanel } from '@/lib/sistema/panelDelNegocio';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { MetricasDelPanel, PanelDelNegocio as Panel } from '@/types/panel';
import { FlujoDeCajaPorSemana } from './FlujoDeCajaPorSemana';

type Detalle = 'cuaderno' | 'objetivos';

type Tarjeta = {
  clave: string;
  etiqueta: string;
  valor: string;
  paraQueSirve: string;
  detalle: Detalle;
  alerta?: boolean;
  nota?: string;
  /** La nota habla de algo que empeoró. */
  notaMala?: boolean;
};

const DIA_MES = (fecha: string) => `${fecha.slice(8, 10)}/${fecha.slice(5, 7)}`;

function nota(variacion: number | null) {
  if (variacion === null) return undefined;
  if (variacion === 0) return 'Igual que antes';
  return `${variacion > 0 ? 'Subió' : 'Bajó'} ${Math.abs(variacion)} % contra antes`;
}

function tarjetasDe(metricas: MetricasDelPanel): Tarjeta[] {
  return [
    {
      clave: 'vendiste',
      etiqueta: 'Vendiste',
      valor: formatoPesos(metricas.vendiste),
      paraQueSirve: 'Todo lo que facturaste, también lo que quedó fiado.',
      detalle: 'cuaderno',
      nota: nota(metricas.variacion),
      notaMala: (metricas.variacion ?? 0) < 0,
    },
    {
      clave: 'queda',
      etiqueta: 'Te queda',
      valor: formatoPesos(metricas.queda),
      paraQueSirve: 'Lo que cobraste menos los gastos y la mercadería que compraste.',
      detalle: 'objetivos',
      alerta: metricas.queda < 0,
    },
    {
      clave: 'teDeben',
      etiqueta: 'Te deben',
      valor: formatoPesos(metricas.teDeben),
      paraQueSirve: 'Lo que vendiste fiado. Todavía no lo contamos como plata cobrada.',
      detalle: 'cuaderno',
    },
    {
      clave: 'margen',
      etiqueta: 'Margen',
      valor: metricas.margen === null ? 'Sin calcular' : `${metricas.margen} %`,
      paraQueSirve:
        metricas.margen === null
          ? 'Cargá el costo de tus productos y te decimos cuánto ganás por cada venta.'
          : 'Cuánto de lo que vendés es ganancia, después del costo de cada producto.',
      detalle: 'objetivos',
    },
  ];
}

// Lo que más importa del negocio al abrir la app: las métricas y el flujo de caja de los últimos días, con el
// cálculo del servidor. Sin cuadernos no aparece; sin ventas explica cómo cargarlas, sin inventar números.
export function PanelDelNegocio() {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const sistema = useMemo(() => sistemaDelPanel(sistemas), [sistemas]);
  const consulta = usePanelDelNegocio(sistema?.id ?? null);
  const navegacion = useNavegacion();
  if (!sistema) return null;

  const verDetalle = (detalle: Detalle) =>
    detalle === 'cuaderno' ? navegacion.abrirSistema(sistema.id, 'use') : navegacion.irAObjetivos();

  return (
    <section className="panel-negocio" aria-labelledby="panel-negocio-titulo">
      <div className="inicio-titulo">
        <h2 id="panel-negocio-titulo">Cómo viene tu negocio</h2>
        {consulta.data?.hayDatos && <p>{periodo(consulta.data)}</p>}
      </div>
      {consulta.isPending && (
        <p className="panel-estado" role="status" aria-busy="true">
          Estamos haciendo tus cuentas…
        </p>
      )}
      {consulta.isError && (
        <div className="panel-estado panel-error" role="alert">
          <p>No pudimos traer tus números. Tus datos están a salvo.</p>
          <Btn cls="small soft" onClick={() => void consulta.refetch()}>
            Probar de nuevo
          </Btn>
        </div>
      )}
      {consulta.data && !consulta.data.hayDatos && (
        <div className="panel-estado panel-vacio">
          <p>
            <strong>Todavía no cargaste ventas en los últimos {consulta.data.dias} días.</strong>{' '}
            Anotá tus primeras ventas en «{sistema.name}» y acá vas a ver cuánto vendés, cuánto te
            queda y cómo se mueve tu plata semana a semana.
          </p>
          <Btn cls="small" onClick={() => void navegacion.abrirSistema(sistema.id, 'use')}>
            Cargar mis ventas <Icon name="arrow" />
          </Btn>
        </div>
      )}
      {consulta.data?.metricas && (
        <>
          <ul className="panel-tarjetas">
            {tarjetasDe(consulta.data.metricas).map((tarjeta) => (
              <li key={tarjeta.clave} className="panel-tarjeta" data-metrica={tarjeta.clave}>
                <span className="panel-tarjeta-etiqueta">{tarjeta.etiqueta}</span>
                <strong
                  className={tarjeta.alerta ? 'panel-tarjeta-valor alerta' : 'panel-tarjeta-valor'}
                >
                  {tarjeta.valor}
                </strong>
                {tarjeta.nota && (
                  <span
                    className={tarjeta.notaMala ? 'panel-tarjeta-nota mala' : 'panel-tarjeta-nota'}
                  >
                    {tarjeta.nota}
                  </span>
                )}
                <p>{tarjeta.paraQueSirve}</p>
                <button
                  type="button"
                  className="panel-tarjeta-detalle"
                  onClick={() => void verDetalle(tarjeta.detalle)}
                >
                  {tarjeta.detalle === 'cuaderno' ? 'Ver mis ventas' : 'Ver mis objetivos'}
                  <Icon name="arrow" />
                </button>
              </li>
            ))}
          </ul>
          <FlujoDeCaja panel={consulta.data} />
        </>
      )}
    </section>
  );
}

const periodo = (panel: Panel) =>
  `Los últimos ${panel.dias} días, del ${DIA_MES(panel.desde)} al ${DIA_MES(panel.hasta)}.`;

function FlujoDeCaja({ panel }: { panel: Panel }) {
  return (
    <div className="panel-flujo">
      <h3>Tu flujo de caja, semana a semana</h3>
      <p className="panel-flujo-bajada">
        Entra lo que cobraste. Sale lo que gastaste y la mercadería que compraste. Lo fiado no entra
        hasta que lo cobrás.
      </p>
      <FlujoDeCajaPorSemana semanas={panel.flujoDeCaja} />
    </div>
  );
}
