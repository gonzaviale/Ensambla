import { ArrowLeftIcon } from '@phosphor-icons/react';
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
        texto="Sami lo divide en 3 mini-objetivos, mide cómo vas sin molestarte y los reajusta cada domingo. Vos decidís qué hacer: Ensambla no cambia tus precios."
        samiTitulo={sami.titulo}
        sami={sami.detalle}
      />
      <section className="objetivo-avance-franja" aria-label="Avance hacia tu meta">
        <div>
          <span>Ganás por mes</span>
          <strong>{formatoPesos(ultimoValor(objetivo))}</strong>
        </div>
        <div className="objetivo-avance-barra">
          <Progreso
            valor={avance(objetivo)}
            etiqueta={`Lo que ganás hoy frente a ${formatoPesos(objetivo.meta)}`}
          />
          <p>
            {Math.round(avance(objetivo) * 100)} % de {formatoPesos(objetivo.meta)}
          </p>
        </div>
        <div className="objetivo-avance-falta">
          <span>{cumplido ? '¡Llegaste!' : 'Te faltan'}</span>
          <strong>{cumplido ? formatoPesos(objetivo.meta) : formatoPesos(brecha(objetivo))}</strong>
        </div>
      </section>
      <SeguimientoDelObjetivo objetivoId={objetivo.id} teFaltan={brecha(objetivo)} />
    </>
  );
}
