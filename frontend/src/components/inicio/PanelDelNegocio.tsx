import { useMemo } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { useAccionesDeAvisos } from '@/hooks/avisos/useAccionesDeAvisos';
import { usePanelDelNegocio } from '@/hooks/panel/queries';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { bloqueDeVentas, pesos, porcentaje, sistemaDelPanel } from '@/lib/sistema/panelDelNegocio';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { MetricasDelPanel, PanelDelNegocio as Panel } from '@/types/panel';
import { FlujoDeCajaPorSemana } from './FlujoDeCajaPorSemana';

type Detalle = 'ventas' | 'objetivos';

type Tarjeta = {
  clave: string;
  etiqueta: string;
  valor: string;
  paraQueSirve: string;
  /** A dónde lleva y cómo se llama el acceso: cada tarjeta dice el suyo, así no hay dos botones iguales. */
  detalle: Detalle;
  verMas: string;
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
      valor: pesos(metricas.vendiste),
      paraQueSirve: 'Todo lo facturado, también lo fiado.',
      detalle: 'ventas',
      verMas: 'Ver mis ventas',
      nota: nota(metricas.variacion),
      notaMala: (metricas.variacion ?? 0) < 0,
    },
    {
      clave: 'queda',
      etiqueta: 'Te queda',
      valor: pesos(metricas.queda),
      paraQueSirve: 'Lo cobrado menos gastos y mercadería.',
      detalle: 'objetivos',
      verMas: 'Ver mis objetivos',
      alerta: metricas.queda < 0,
    },
    {
      clave: 'teDeben',
      etiqueta: 'Te deben',
      valor: pesos(metricas.teDeben),
      paraQueSirve: 'Lo que vendiste fiado y todavía no cobraste.',
      detalle: 'ventas',
      verMas: 'Ver las ventas fiadas',
    },
    {
      clave: 'margen',
      etiqueta: 'Margen',
      valor: metricas.margen === null ? 'Sin calcular' : porcentaje(metricas.margen),
      paraQueSirve:
        metricas.margen === null
          ? 'Cargá el costo de tus productos para calcularlo.'
          : 'Cuánto de lo vendido es ganancia.',
      detalle: 'objetivos',
      verMas: 'Mejorar mi margen',
    },
  ];
}

// Lo que más importa del negocio al abrir la app: las métricas y el flujo de caja de los últimos días, con el
// cálculo del servidor. Sin cuadernos no aparece; sin ventas explica cómo cargarlas, sin inventar números. Es el
// tema principal de la pantalla, por eso su título es el h1 (el saludo de abajo pasa a h2).
export function PanelDelNegocio() {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const sistema = useMemo(() => sistemaDelPanel(sistemas), [sistemas]);
  const consulta = usePanelDelNegocio(sistema?.id ?? null);
  const navegacion = useNavegacion();
  const acciones = useAccionesDeAvisos();
  if (!sistema) return null;

  // «Ver mis ventas» lleva a la tabla de ventas y la marca; si el cuaderno no tiene una, lo abre.
  const abrirVentas = () => {
    const ventas = bloqueDeVentas(sistema);
    return ventas
      ? acciones.irAlObjeto({ sistemaId: sistema.id, bloqueId: ventas.id })
      : navegacion.abrirSistema(sistema.id, 'use');
  };
  const verDetalle = (detalle: Detalle) =>
    detalle === 'ventas' ? abrirVentas() : navegacion.irAObjetivos();

  return (
    <section className="panel-negocio" aria-labelledby="panel-negocio-titulo">
      <div className="inicio-titulo">
        <h1 id="panel-negocio-titulo">Cómo viene tu negocio</h1>
        {consulta.data?.hayDatos && <p>{periodo(consulta.data)}</p>}
      </div>
      {consulta.isPending && <Esqueleto />}
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
          <Btn cls="small" onClick={() => void abrirVentas()}>
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
                  {tarjeta.verMas}
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

// Ocupa casi lo mismo que el panel ya armado: así, cuando llegan los números, lo de abajo no salta.
function Esqueleto() {
  return (
    <div className="panel-esqueleto" role="status" aria-busy="true">
      <span className="sr-only">Estamos haciendo tus cuentas…</span>
      <div className="panel-esqueleto-tarjetas" aria-hidden="true">
        <span />
        <span />
        <span />
        <span />
      </div>
      <div className="panel-esqueleto-flujo" aria-hidden="true" />
    </div>
  );
}

function FlujoDeCaja({ panel }: { panel: Panel }) {
  return (
    <div className="panel-flujo">
      <h2>Tu flujo de caja, semana a semana</h2>
      <p className="panel-flujo-bajada">
        Entra lo que cobraste. Sale lo que gastaste y la mercadería que compraste. Lo fiado no entra
        hasta que lo cobrás.
      </p>
      <FlujoDeCajaPorSemana semanas={panel.flujoDeCaja} />
    </div>
  );
}
