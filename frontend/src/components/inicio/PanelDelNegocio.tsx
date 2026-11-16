import { useMemo } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { useAccionesDeAvisos } from '@/hooks/avisos/useAccionesDeAvisos';
import { usePanelDelNegocio } from '@/hooks/panel/queries';
import { useSistemaElegido } from '@/hooks/reportes/useSistemaElegido';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { bloqueDeVentas, pesos, sistemaDelPanel } from '@/lib/sistema/panelDelNegocio';
import { seleccionarSistemasDelNegocio, useSistemaStore } from '@/stores/sistemaStore';
import type { PanelDelNegocio as Panel } from '@/types/panel';
import { Desplegable } from './Desplegable';
import { FlujoDeCajaPorSemana } from './FlujoDeCajaPorSemana';

const DIA_MES = (fecha: string) => `${fecha.slice(8, 10)}/${fecha.slice(5, 7)}`;

// Lo último del inicio, para consultar: el flujo de caja de los últimos días, con el cálculo del servidor. Mira la
// misma sección que las métricas (la que se eligió arriba). Sin secciones no aparece; sin
// ventas explica cómo cargarlas, sin inventar números.
export function PanelDelNegocio() {
  const sistemas = useSistemaStore(seleccionarSistemasDelNegocio);
  const { sistema: elegido } = useSistemaElegido();
  const sistema = useMemo(() => elegido ?? sistemaDelPanel(sistemas), [elegido, sistemas]);
  const consulta = usePanelDelNegocio(sistema?.id ?? null);
  const navegacion = useNavegacion();
  const acciones = useAccionesDeAvisos();
  if (!sistema) return null;

  // «Ver mis ventas» lleva a la tabla de ventas y la marca; si la sección no tiene una, la abre.
  const abrirVentas = () => {
    const ventas = bloqueDeVentas(sistema);
    return ventas
      ? acciones.irAlObjeto({ sistemaId: sistema.id, bloqueId: ventas.id })
      : navegacion.abrirSistema(sistema.id, 'use');
  };

  return (
    <section className="panel-negocio" aria-labelledby="panel-negocio-titulo">
      <div className="inicio-titulo">
        <h2 id="panel-negocio-titulo" className="inicio-seccion-titulo">
          Tu flujo de caja
        </h2>
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
            Anotá tus primeras ventas en «{sistema.name}» y acá vas a ver cómo se mueve tu plata
            semana a semana.
          </p>
          <Btn cls="small" onClick={() => void abrirVentas()}>
            Cargar mis ventas <Icon name="arrow" />
          </Btn>
        </div>
      )}
      {consulta.data?.hayDatos && <FlujoDeCaja panel={consulta.data} />}
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
      <div className="panel-esqueleto-flujo" aria-hidden="true" />
    </div>
  );
}

// Primero lo que queda y, en chico, lo que entró y salió en todo el período; el detalle semana a semana, en el
// desplegable de siempre.
function FlujoDeCaja({ panel }: { panel: Panel }) {
  const queda = panel.flujoDeCaja.at(-1)?.saldoAcumulado ?? 0;
  const entro = panel.flujoDeCaja.reduce((total, semana) => total + semana.entradas, 0);
  const salio = panel.flujoDeCaja.reduce((total, semana) => total + semana.salidas, 0);
  const semanas = panel.flujoDeCaja.length;
  return (
    <div className="panel-flujo">
      <dl className="panel-flujo-resumen">
        <div className="panel-flujo-queda" data-signo={Math.sign(queda)}>
          <dt>Te va quedando</dt>
          <dd>{pesos(queda)}</dd>
        </div>
        <div data-tipo="entra">
          <dt>Entró</dt>
          <dd>{pesos(entro)}</dd>
        </div>
        <div data-tipo="sale">
          <dt>Salió</dt>
          <dd>{pesos(salio)}</dd>
        </div>
      </dl>
      <Desplegable
        titulo="Semana a semana"
        resumen={`${semanas} ${semanas === 1 ? 'semana' : 'semanas'}`}
      >
        <p className="panel-flujo-bajada">
          Entra lo que cobraste. Sale lo que gastaste y la mercadería que compraste. Lo fiado no
          entra hasta que lo cobrás.
        </p>
        <FlujoDeCajaPorSemana semanas={panel.flujoDeCaja} />
      </Desplegable>
    </div>
  );
}
