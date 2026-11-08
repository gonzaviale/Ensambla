import { useState } from 'react';
import { PlusIcon } from '@phosphor-icons/react';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { formatoPesos, type Objetivo } from '@/lib/objetivos';
import { useObjetivosStore } from '@/stores/objetivosStore';
import { useSistemaStore } from '@/stores/sistemaStore';
import { CruceConZona } from './CruceConZona';
import { DefinirObjetivo } from './DefinirObjetivo';
import { ObjetivoCard } from './ObjetivoCard';
import { Encabezado } from './piezas';

type Pestania = 'objetivos' | 'zona';

const PESTANIAS: { clave: Pestania; nombre: string }[] = [
  { clave: 'objetivos', nombre: 'Mis objetivos' },
  { clave: 'zona', nombre: 'Cruce con Mi zona' },
];

export function ObjetivosPage() {
  useTituloPagina('Objetivos');
  const navegacion = useNavegacion();
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const objetivos = useObjetivosStore((estado) => estado.objetivos);
  const eliminar = useObjetivosStore((estado) => estado.eliminar);
  const [pestania, setPestania] = useState<Pestania>('objetivos');
  const [definiendo, setDefiniendo] = useState(false);

  const quitar = (objetivo: Objetivo) =>
    abrirModal({
      tipo: 'confirmar',
      titulo: '¿Quitar este objetivo?',
      mensaje: `Dejás de seguir «${objetivo.texto}» (${formatoPesos(objetivo.meta)} por mes). Tus cuadernos no cambian.`,
      etiqueta: 'Quitar objetivo',
      onConfirmar: () => eliminar(objetivo.id),
    });

  // «Usar en mi plan» lleva al plan del primer objetivo; sin objetivos, primero hay que definir uno.
  const usarEnElPlan = () => {
    if (objetivos[0]) void navegacion.irAObjetivo(objetivos[0].id);
    else setPestania('objetivos');
  };

  const sinObjetivos = objetivos.length === 0;

  return (
    <>
      <CabeceraComun active="goals" />
      <main id="main" tabIndex={-1} className="page-wrap objetivos-page reveal">
        <div className="objetivos-pestanias" role="tablist" aria-label="Secciones de objetivos">
          {PESTANIAS.map(({ clave, nombre }) => (
            <button
              type="button"
              role="tab"
              key={clave}
              id={`pestania-${clave}`}
              aria-selected={pestania === clave}
              aria-controls={`panel-${clave}`}
              className={pestania === clave ? 'active' : ''}
              onClick={() => setPestania(clave)}
            >
              {nombre}
            </button>
          ))}
        </div>

        {pestania === 'objetivos' && (
          <div role="tabpanel" id="panel-objetivos" aria-labelledby="pestania-objetivos">
            {sinObjetivos ? (
              <>
                <Encabezado
                  etiqueta="Objetivos"
                  titulo="Definí tu primer objetivo"
                  texto="Contale a Sami qué querés lograr con tu negocio. Lo convierte en una meta medible y te muestra cómo vas, semana a semana."
                  sami="Te ayuda a ponerle número a lo que querés"
                />
                <DefinirObjetivo onDefinido={() => setDefiniendo(false)} />
              </>
            ) : (
              <>
                <Encabezado
                  etiqueta="Objetivos"
                  titulo="Cómo van tus objetivos"
                  texto="Acá ves cuánto avanzaste, cómo fue cambiando tu resultado y qué te recomienda Sami para llegar."
                  sami="Mide tu avance cada semana"
                />
                <div className="objetivos-lista">
                  {objetivos.map((objetivo) => (
                    <ObjetivoCard
                      key={objetivo.id}
                      objetivo={objetivo}
                      onPlan={() => void navegacion.irAObjetivo(objetivo.id)}
                      onProbar={() => void navegacion.irASamiAcademy('escenario', objetivo.id)}
                      onQuitar={() => quitar(objetivo)}
                    />
                  ))}
                </div>
                {definiendo ? (
                  <DefinirObjetivo
                    onDefinido={() => setDefiniendo(false)}
                    onCancelar={() => setDefiniendo(false)}
                  />
                ) : (
                  <button
                    type="button"
                    className="btn secondary"
                    onClick={() => setDefiniendo(true)}
                  >
                    <PlusIcon aria-hidden size={18} /> Definir otro objetivo
                  </button>
                )}
              </>
            )}
          </div>
        )}

        {pestania === 'zona' && (
          <div role="tabpanel" id="panel-zona" aria-labelledby="pestania-zona">
            <CruceConZona onPlan={usarEnElPlan} />
          </div>
        )}
      </main>
    </>
  );
}
