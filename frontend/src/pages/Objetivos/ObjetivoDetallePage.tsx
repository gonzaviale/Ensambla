import { ArrowLeftIcon, ClipboardTextIcon } from '@phosphor-icons/react';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Sami } from '@/components/sistema/Sami';
import { useObjetivos } from '@/hooks/objetivos/queries';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import {
  PALANCA,
  aportes,
  avance,
  brecha,
  formatoPesos,
  ultimoValor,
  valoresRecomendados,
  variacion,
  type Objetivo,
} from '@/lib/objetivos';
import { Encabezado, Metrica } from './piezas';

// El plan de acción y el seguimiento de un objetivo: cómo se llega y cómo se va.
export function ObjetivoDetallePage({ objetivoId }: { objetivoId: string }) {
  const { objetivos, cargando } = useObjetivos();
  const objetivo = objetivos.find((item) => item.id === objetivoId);
  const navegacion = useNavegacion();
  useTituloPagina(objetivo || cargando ? 'Plan de acción' : 'Objetivo no encontrado');

  return (
    <>
      <CabeceraComun active="goals" />
      <main id="main" tabIndex={-1} className="page-wrap objetivos-page reveal">
        <button
          type="button"
          className="objetivos-volver"
          onClick={() => void navegacion.irAObjetivos()}
        >
          <ArrowLeftIcon aria-hidden size={20} /> Volver a objetivos
        </button>
        {objetivo ? (
          <Detalle
            objetivo={objetivo}
            onProbar={() => void navegacion.irASamiTeEnsena('escenario', objetivo.id)}
          />
        ) : cargando ? (
          <p role="status" className="objetivos-cargando">
            Cargando el objetivo…
          </p>
        ) : (
          <section className="objetivos-vacio">
            <h1>No encontramos ese objetivo</h1>
            <p>
              Puede que lo hayas quitado. Volvé a tus objetivos para elegir otro o definir uno
              nuevo.
            </p>
            <button
              type="button"
              className="btn primary"
              onClick={() => void navegacion.irAObjetivos()}
            >
              Ir a mis objetivos
            </button>
          </section>
        )}
      </main>
    </>
  );
}

function Detalle({ objetivo, onProbar }: { objetivo: Objetivo; onProbar: () => void }) {
  const recomendados = valoresRecomendados(objetivo);
  const impacto = aportes(objetivo, recomendados);
  const cumplido = brecha(objetivo) === 0;
  const porcentaje = Math.round(avance(objetivo) * 100);
  const diferencia = ultimoValor(objetivo) - objetivo.meta;

  const tareas = [
    {
      titulo: 'Revisar los precios con margen bajo',
      detalle: `Meta: ${PALANCA.margen.formato(recomendados.margen)}`,
      impacto: impacto.margen,
    },
    {
      titulo: 'Reducir gastos extraordinarios',
      detalle: `Meta: ${PALANCA.gastos.formato(recomendados.gastos)} por mes`,
      impacto: impacto.gastos,
    },
    {
      titulo: 'Aumentar ventas con margen sano',
      detalle: `Objetivo: ${PALANCA.ventas.formato(recomendados.ventas)} de ventas`,
      impacto: impacto.ventas,
    },
  ];

  return (
    <>
      <Encabezado
        etiqueta={`Plan de acción · Meta ${formatoPesos(objetivo.meta)} por mes`}
        titulo={objetivo.texto}
        texto="Un plan pequeño, medible y revisable. Ensambla no cambia precios ni registros: vos elegís qué aplicar."
        sami="Te acompaña paso a paso"
      />

      {cumplido ? (
        <section className="objetivos-vacio">
          <h2>Ya alcanzaste este objetivo</h2>
          <p>
            No hace falta un plan: tu resultado de hoy llega a la meta. Podés definir un objetivo
            más alto.
          </p>
        </section>
      ) : (
        <div className="objetivos-plan-layout">
          <article className="objetivos-plan">
            <header>
              <ClipboardTextIcon aria-hidden size={28} />
              <div>
                <p className="objetivos-etiqueta">Camino combinado</p>
                <h2>Cómo llegar sin depender solo de vender más</h2>
              </div>
            </header>
            {tareas.map((tarea, indice) => (
              <div className="objetivos-tarea" key={tarea.titulo}>
                <span>{indice + 1}</span>
                <div>
                  <strong>{tarea.titulo}</strong>
                  <p>{tarea.detalle}</p>
                </div>
                <b>+{formatoPesos(tarea.impacto)} por mes</b>
              </div>
            ))}
            <p className="objetivos-formula">
              Estimaciones de demostración: con las tres tareas, tu resultado llegaría a la meta.
            </p>
          </article>
          <aside className="objetivos-meta-plan">
            <p>Avance hacia tu meta</p>
            <strong>{porcentaje} %</strong>
            <span>
              {formatoPesos(ultimoValor(objetivo))} de {formatoPesos(objetivo.meta)}
            </span>
            <div>
              <i style={{ width: `${porcentaje}%` }} />
            </div>
            <b>Te faltan {formatoPesos(brecha(objetivo))} por mes</b>
            <small>Se revisa cada semana. Nada se aplica sin confirmación.</small>
            <button type="button" className="btn primary" onClick={onProbar}>
              Ver cómo funcionaría
            </button>
          </aside>
        </div>
      )}

      <Seguimiento objetivo={objetivo} diferencia={diferencia} />
    </>
  );
}

function resumenDeSami(objetivo: Objetivo) {
  const semanas = objetivo.historial.length - 1;
  if (semanas < 1) {
    return 'Todavía no hay semanas para comparar. Cuando pase la primera te cuento cómo vas y ajusto mis supuestos.';
  }
  const primero = objetivo.historial[0]?.valor ?? ultimoValor(objetivo);
  const cambio = variacion(objetivo);
  const tendencia =
    cambio > 0
      ? `Tu resultado subió ${formatoPesos(cambio)}: de ${formatoPesos(primero)} a ${formatoPesos(ultimoValor(objetivo))}.`
      : cambio < 0
        ? `Tu resultado bajó ${formatoPesos(-cambio)}: de ${formatoPesos(primero)} a ${formatoPesos(ultimoValor(objetivo))}.`
        : `Tu resultado se mantuvo en ${formatoPesos(primero)}.`;
  const resto =
    brecha(objetivo) === 0
      ? 'Ya llegaste a la meta.'
      : `Al ritmo de estas semanas, todavía hay camino por recorrer: te faltan ${formatoPesos(brecha(objetivo))}. Para el próximo escenario voy a usar un supuesto de crecimiento prudente.`;
  return `${tendencia} ${resto}`;
}

function Seguimiento({ objetivo, diferencia }: { objetivo: Objetivo; diferencia: number }) {
  return (
    <section aria-labelledby="seguimiento-titulo">
      <h2 id="seguimiento-titulo" className="objetivos-subtitulo">
        Seguimiento
      </h2>
      <div className="objetivos-seguimiento">
        <Metrica etiqueta="Meta mensual" valor={formatoPesos(objetivo.meta)} />
        <Metrica etiqueta="Resultado de hoy" valor={formatoPesos(ultimoValor(objetivo))} />
        <Metrica
          etiqueta="Diferencia"
          valor={`${diferencia < 0 ? '−' : '+'}${formatoPesos(Math.abs(diferencia))}`}
          alerta={diferencia < 0}
        />
      </div>
      <article className="objetivos-cierre">
        <div className="objetivos-cierre-sami">
          <Sami pose="hello" />
        </div>
        <div>
          <p className="objetivos-etiqueta">Sami mide cómo vas</p>
          <h2>Proyectado frente a real</h2>
          <p>{resumenDeSami(objetivo)}</p>
        </div>
      </article>
    </section>
  );
}
