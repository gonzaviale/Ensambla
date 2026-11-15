import { ArrowLeftIcon, FlagCheckeredIcon, TrendUpIcon } from '@phosphor-icons/react';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { useMiniObjetivos, useObjetivos, useSemanasObjetivo } from '@/hooks/objetivos/queries';
import { useMedirObjetivos } from '@/hooks/objetivos/useMedirObjetivos';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { avance, brecha, formatoPesos, ultimoValor, type Objetivo } from '@/lib/objetivos';
import { estadoDeSami, resumenSinVer } from '@/lib/objetivosAutogestion';
import { hoy } from '@/lib/sistema/formato';
import { SeguimientoDelObjetivo } from './autogestion/SeguimientoDelObjetivo';
import { Encabezado, Progreso } from './piezas';

// El seguimiento de un objetivo: sus 3 mini-objetivos, cómo fue cada semana y qué reajustó Sami los domingos.
export function ObjetivoDetallePage({ objetivoId }: { objetivoId: string }) {
  useMedirObjetivos();
  const { objetivos, cargando } = useObjetivos();
  const objetivo = objetivos.find((item) => item.id === objetivoId);
  const navegacion = useNavegacion();
  useTituloPagina(objetivo || cargando ? 'Mi semana' : 'Objetivo no encontrado');

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
          <Detalle objetivo={objetivo} />
        ) : cargando ? (
          <p role="status" className="objetivos-cargando">
            Cargando el objetivo…
          </p>
        ) : (
          <section className="objetivos-vacio">
            <h1>No encontramos ese objetivo</h1>
            <p>Puede que lo hayas quitado. Elegí otro o definí uno nuevo.</p>
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

function Detalle({ objetivo }: { objetivo: Objetivo }) {
  const cumplido = brecha(objetivo) === 0;
  const minis = useMiniObjetivos(objetivo.id).data;
  const ultimoCierre = useSemanasObjetivo(objetivo.id).data?.ultimoCierre ?? null;
  const sami = estadoDeSami({
    alcanzado: cumplido || minis?.faltanDatos === 'OBJETIVO_ALCANZADO',
    sinDatos: minis?.faltanDatos === 'POCOS_DATOS',
    resumenListo: resumenSinVer(objetivo.id, ultimoCierre),
    dia: hoy(),
  });
  return (
    <>
      <Encabezado
        etiqueta={`Tu objetivo · Meta ${formatoPesos(objetivo.meta)} por mes`}
        titulo={objetivo.texto}
        texto="Sami lo divide en 3 mini-objetivos y los ajusta cada domingo."
        samiTitulo={sami.titulo}
        sami={sami.detalle}
      />
      <section className="objetivo-tablero" aria-label="Avance hacia tu meta">
        <div className="objetivo-tablero-extremo hoy">
          <span className="objetivo-tablero-etiqueta">
            <TrendUpIcon aria-hidden size={20} weight="bold" /> Hoy
          </span>
          <strong>{formatoPesos(ultimoValor(objetivo))}</strong>
          <small>ganás por mes</small>
        </div>
        <p className="objetivo-tablero-porcentaje">
          <strong>{Math.round(avance(objetivo) * 100)} %</strong>
          <span>de tu meta</span>
        </p>
        <div className="objetivo-tablero-extremo meta">
          <span className="objetivo-tablero-etiqueta">
            <FlagCheckeredIcon aria-hidden size={20} weight="bold" /> Meta
          </span>
          <strong>{formatoPesos(objetivo.meta)}</strong>
          <small>por mes</small>
        </div>
        <div className="objetivo-tablero-barra">
          <Progreso
            valor={avance(objetivo)}
            etiqueta={`Lo que ganás hoy frente a ${formatoPesos(objetivo.meta)}`}
          />
        </div>
        <p className="objetivo-tablero-falta">
          {cumplido ? '¡Llegaste!' : `Te faltan ${formatoPesos(brecha(objetivo))}`}
        </p>
      </section>
      <SeguimientoDelObjetivo objetivoId={objetivo.id} teFaltan={brecha(objetivo)} />
    </>
  );
}
