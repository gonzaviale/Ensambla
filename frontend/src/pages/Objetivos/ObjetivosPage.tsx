import { useState } from 'react';
import { PlusIcon } from '@phosphor-icons/react';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { useEliminarObjetivo } from '@/hooks/objetivos/mutations';
import { useObjetivos } from '@/hooks/objetivos/queries';
import { useMedirObjetivos } from '@/hooks/objetivos/useMedirObjetivos';
import { useComoVieneTuNegocio } from '@/hooks/objetivos/useComoVieneTuNegocio';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { formatoPesos, type Objetivo } from '@/lib/objetivos';
import { diaCorto, proximoCierre } from '@/lib/objetivosAutogestion';
import { hoy } from '@/lib/sistema/formato';
import { useSistemaStore } from '@/stores/sistemaStore';
import { DefinirObjetivo } from './DefinirObjetivo';
import { NegocioHoy } from './NegocioHoy';
import { ObjetivoCard } from './ObjetivoCard';
import { Encabezado } from './piezas';

// Todo en una pantalla: los objetivos, cómo vienen y, abajo, cómo sumar otro.
export function ObjetivosPage() {
  useTituloPagina('Objetivos');
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  useMedirObjetivos();
  const { objetivos, cargando } = useObjetivos();
  const eliminar = useEliminarObjetivo().mutate;
  const [definiendo, setDefiniendo] = useState(false);
  const { negocio } = useComoVieneTuNegocio();
  const navegacion = useNavegacion();

  const quitar = (objetivo: Objetivo) =>
    abrirModal({
      tipo: 'confirmar',
      titulo: '¿Quitar este objetivo?',
      mensaje: `Dejás de seguir «${objetivo.texto}» (${formatoPesos(objetivo.meta)} por mes). Tus cuadernos no cambian.`,
      etiqueta: 'Quitar objetivo',
      onConfirmar: () => eliminar(objetivo.id),
    });

  return (
    <>
      <CabeceraComun active="goals" />
      <main id="main" tabIndex={-1} className="page-wrap objetivos-page reveal">
        {cargando ? (
          <p role="status" className="objetivos-cargando">
            Cargando tus objetivos…
          </p>
        ) : objetivos.length === 0 ? (
          <>
            <Encabezado
              etiqueta="Objetivos"
              titulo="Definí tu primer objetivo"
              texto="Contale a Sami qué querés lograr con tu negocio. Te dice qué hacer con tus productos para llegar."
              samiTitulo="Sami te ayuda"
              sami="Le pone número a lo que querés lograr"
            />
            <DefinirObjetivo onDefinido={() => setDefiniendo(false)} />
          </>
        ) : (
          <>
            <Encabezado
              etiqueta="Objetivos"
              titulo="Cómo van tus objetivos"
              texto="Acá ves cuánto te falta para llegar. En «Ver mi semana» están los mini-objetivos de Sami."
              samiTitulo="Sami sigue tus objetivos"
              sami={`Mide cada día · cierra el domingo ${diaCorto(proximoCierre(hoy()))}`}
            />
            <NegocioHoy negocio={negocio} />
            <div className="objetivos-lista">
              {objetivos.map((objetivo) => (
                <ObjetivoCard
                  key={objetivo.id}
                  objetivo={objetivo}
                  onQuitar={() => quitar(objetivo)}
                  onVerSemana={() => void navegacion.irAObjetivo(objetivo.id)}
                />
              ))}
            </div>
            {definiendo ? (
              <DefinirObjetivo
                onDefinido={() => setDefiniendo(false)}
                onCancelar={() => setDefiniendo(false)}
              />
            ) : (
              <button type="button" className="btn secondary" onClick={() => setDefiniendo(true)}>
                <PlusIcon aria-hidden size={18} /> Definir otro objetivo
              </button>
            )}
          </>
        )}
      </main>
    </>
  );
}
