import { ArrowLeftIcon, FlagCheckeredIcon, TrendUpIcon } from '@phosphor-icons/react';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { PageFooter } from '@/components/sistema/PageFooter';
import { useMiniObjetivos, useObjetivos, useSemanasObjetivo } from '@/hooks/objetivos/queries';
import { useMedirObjetivos } from '@/hooks/objetivos/useMedirObjetivos';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import {
  avance,
  brecha,
  formatoPesos,
  tieneMeta,
  ultimoValor,
  type Objetivo,
  type ObjetivoConMeta,
} from '@/lib/objetivos';
import { COMO_SE_MIDE } from '@/lib/objetivosSami';
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
        <PageFooter />
      </main>
    </>
  );
}

function Detalle({ objetivo }: { objetivo: Objetivo }) {
  const cumplido = tieneMeta(objetivo) && brecha(objetivo) === 0;
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
      {tieneMeta(objetivo) ? (
        <>
          <Encabezado
            etiqueta={`Tu objetivo · Meta ${formatoPesos(objetivo.meta)} por mes`}
            titulo={objetivo.texto}
            texto="Sami lo divide en 3 mini-objetivos, mide cómo vas sin molestarte y los reajusta cada domingo. Vos decidís qué hacer: Ensambla no cambia tus precios."
            samiTitulo={sami.titulo}
            sami={sami.detalle}
          />
          <FranjaDeAvance objetivo={objetivo} cumplido={cumplido} />
          <SeguimientoDelObjetivo objetivoId={objetivo.id} teFaltan={brecha(objetivo)} />
        </>
      ) : (
        <>
          <Encabezado
            etiqueta={`Tu objetivo · ${COMO_SE_MIDE[objetivo.tipo]}`}
            titulo={objetivo.texto}
            texto="Sami te propone mini-objetivos cada semana y los renueva el domingo según lo que pasó."
            samiTitulo={sami.titulo}
            sami={sami.detalle}
          />
          <SeguimientoDelObjetivo objetivoId={objetivo.id} />
        </>
      )}
    </>
  );
}

function FranjaDeAvance({ objetivo, cumplido }: { objetivo: ObjetivoConMeta; cumplido: boolean }) {
  return (
    <>
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
    </>
  );
}
