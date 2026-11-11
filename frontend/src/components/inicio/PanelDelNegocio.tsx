import { useMemo } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { useAccionesDeAvisos } from '@/hooks/avisos/useAccionesDeAvisos';
import { usePanelDelNegocio } from '@/hooks/panel/queries';
import { useCuadernoElegido } from '@/hooks/reportes/useCuadernoElegido';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { bloqueDeVentas, pesos, sistemaDelPanel } from '@/lib/sistema/panelDelNegocio';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { PanelDelNegocio as Panel } from '@/types/panel';
import { DecisionesRapidas } from './DecisionesRapidas';
import { Desplegable } from './Desplegable';
import { FlujoDeCajaPorSemana } from './FlujoDeCajaPorSemana';

const DIA_MES = (fecha: string) => `${fecha.slice(8, 10)}/${fecha.slice(5, 7)}`;

// Debajo de las métricas del inicio: las sugerencias de Sami y el flujo de caja de los últimos días, con el cálculo
// del servidor. Mira el mismo cuaderno que las métricas (el que se eligió arriba). Sin cuadernos no aparece; sin
// ventas explica cómo cargarlas, sin inventar números.
export function PanelDelNegocio() {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const { cuaderno } = useCuadernoElegido();
  const sistema = useMemo(() => cuaderno ?? sistemaDelPanel(sistemas), [cuaderno, sistemas]);
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

  return (
    <section className="panel-negocio" aria-labelledby="panel-negocio-titulo">
      <div className="inicio-titulo">
        <h2 id="panel-negocio-titulo">Tu plata y lo que conviene hacer</h2>
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
      <DecisionesRapidas />
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
