import { useMemo } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { usePanelDelNegocio } from '@/hooks/panel/queries';
import { useAbrirVentas } from '@/hooks/panel/useAbrirVentas';
import { useSistemaElegido } from '@/hooks/reportes/useSistemaElegido';
import { pesos, sistemaDelPanel } from '@/lib/sistema/panelDelNegocio';
import { seleccionarSistemasDelNegocio, useSistemaStore } from '@/stores/sistemaStore';
import type { PanelDelNegocio as Panel } from '@/types/panel';
import { Desplegable } from './Desplegable';
import { FlujoDeCajaPorSemana } from './FlujoDeCajaPorSemana';

const DIA_MES = (fecha: string) => `${fecha.slice(8, 10)}/${fecha.slice(5, 7)}`;

// Al final del inicio: el flujo de caja de los últimos días, con el cálculo del servidor. Mira la sección
// elegida en el inicio. Sin secciones no aparece; sin
// ventas explica cómo cargarlas, sin inventar números.
export function PanelDelNegocio() {
  const sistemas = useSistemaStore(seleccionarSistemasDelNegocio);
  const { sistema: elegido } = useSistemaElegido();
  const sistema = useMemo(() => elegido ?? sistemaDelPanel(sistemas), [elegido, sistemas]);
  const consulta = usePanelDelNegocio(sistema?.id ?? null);
  const abrirVentas = useAbrirVentas();
  if (!sistema) return null;

  return (
    <section className="panel-negocio" aria-labelledby="panel-negocio-titulo">
      <div className="inicio-titulo">
        <h2 id="panel-negocio-titulo">Tu flujo de caja</h2>
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
          <Btn cls="small" onClick={() => void abrirVentas(sistema)}>
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

function FlujoDeCaja({ panel }: { panel: Panel }) {
  const queda = panel.flujoDeCaja.at(-1)?.saldoAcumulado ?? 0;
  return (
    <Desplegable
      titulo="Tu flujo de caja, semana a semana"
      resumen={`Te va quedando ${pesos(queda)}`}
    >
      <p className="panel-flujo-bajada">
        Entra lo que cobraste. Sale lo que gastaste y la mercadería que compraste. Lo fiado no entra
        hasta que lo cobrás.
      </p>
      <FlujoDeCajaPorSemana semanas={panel.flujoDeCaja} />
    </Desplegable>
  );
}
