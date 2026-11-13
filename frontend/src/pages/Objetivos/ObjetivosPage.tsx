import { useState } from 'react';
import { PlusIcon } from '@phosphor-icons/react';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { useEliminarObjetivo } from '@/hooks/objetivos/mutations';
import { useObjetivos } from '@/hooks/objetivos/queries';
import { useComoVieneTuNegocio } from '@/hooks/objetivos/useComoVieneTuNegocio';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { formatoPesos, type Objetivo } from '@/lib/objetivos';
import { useSistemaStore } from '@/stores/sistemaStore';
import { EntradaDondeAbrir } from '@/pages/DondeAbrir/EntradaDondeAbrir';
import { DefinirObjetivo } from './DefinirObjetivo';
import { NegocioHoy } from './NegocioHoy';
import { ObjetivoCard } from './ObjetivoCard';
import { Encabezado } from './piezas';

// Todo en una pantalla: los objetivos, qué hacer con cada uno y, abajo, cómo sumar otro.
export function ObjetivosPage() {
  useTituloPagina('Objetivos');
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const { objetivos, cargando } = useObjetivos();
  const eliminar = useEliminarObjetivo().mutate;
  const [definiendo, setDefiniendo] = useState(false);
  const { negocio } = useComoVieneTuNegocio();

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
              sami="Te ayuda a ponerle número a lo que querés"
            />
            <DefinirObjetivo onDefinido={() => setDefiniendo(false)} />
          </>
        ) : (
          <>
            <Encabezado
              etiqueta="Objetivos"
              titulo="Cómo van tus objetivos"
              texto="Acá ves cuánto te falta y qué podés hacer, producto por producto, para llegar."
              sami="Mide tu avance cada semana"
            />
            <NegocioHoy negocio={negocio} />
            <div className="objetivos-lista">
              {objetivos.map((objetivo) => (
                <ObjetivoCard
                  key={objetivo.id}
                  objetivo={objetivo}
                  abierta={objetivos.length === 1}
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
              <button type="button" className="btn secondary" onClick={() => setDefiniendo(true)}>
                <PlusIcon aria-hidden size={18} /> Definir otro objetivo
              </button>
            )}
          </>
        )}
        <EntradaDondeAbrir />
      </main>
    </>
  );
}
