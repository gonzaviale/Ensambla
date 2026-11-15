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
import { useSistemaStore } from '@/stores/sistemaStore';
import { EntradaDondeAbrir } from '@/pages/DondeAbrir/EntradaDondeAbrir';
import { NegocioHoy } from './NegocioHoy';
import { NuevoObjetivo } from './NuevoObjetivo';
import { PendientesDeRespuesta } from './PendientesDeRespuesta';
import { CuotaEnObjetivos } from './sami/CuotaEnObjetivos';
import { ObjetivoCard } from './ObjetivoCard';
import { Encabezado } from './piezas';

const mensajeParaQuitar = (objetivo: Objetivo) =>
  objetivo.meta === null
    ? `Dejás de seguir «${objetivo.texto}». Tus cuadernos no cambian.`
    : `Dejás de seguir «${objetivo.texto}» (${formatoPesos(objetivo.meta)} por mes). Tus cuadernos no cambian.`;

function useQuitarObjetivo() {
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const eliminar = useEliminarObjetivo().mutate;
  return (objetivo: Objetivo) =>
    abrirModal({
      tipo: 'confirmar',
      titulo: '¿Quitar este objetivo?',
      mensaje: mensajeParaQuitar(objetivo),
      etiqueta: 'Quitar objetivo',
      onConfirmar: () => eliminar(objetivo.id),
    });
}

function NuevoDesafio({ abierto, onNuevo }: { abierto: boolean; onNuevo: () => void }) {
  return (
    <div className="objetivos-nuevo-desafio">
      <button type="button" className="btn primary" disabled={abierto} onClick={onNuevo}>
        <PlusIcon aria-hidden size={18} /> Nuevo desafío
      </button>
      <CuotaEnObjetivos />
    </div>
  );
}

function PrimerObjetivo({ onDefinido }: { onDefinido: () => void }) {
  return (
    <>
      <Encabezado
        etiqueta="Objetivos"
        titulo="Definí tu primer objetivo"
        texto="Contale a Sami qué querés lograr, aunque no sea plata. Te arma pasos chicos para cada semana."
        samiTitulo="Sami te ayuda"
        sami="Le pone número a lo que querés lograr"
      />
      <NuevoObjetivo onDefinido={onDefinido} />
    </>
  );
}

function ObjetivosDelNegocio({ objetivos }: { objetivos: Objetivo[] }) {
  const [definiendo, setDefiniendo] = useState(false);
  const { negocio } = useComoVieneTuNegocio();
  const navegacion = useNavegacion();
  const quitar = useQuitarObjetivo();
  const cerrarDefinicion = () => setDefiniendo(false);
  return (
    <>
      <Encabezado
        etiqueta="Objetivos"
        titulo="Cómo van tus objetivos"
        texto="Acá ves cuánto te falta para llegar. En «Ver mi semana» están los mini-objetivos de Sami."
        accion={<NuevoDesafio abierto={definiendo} onNuevo={() => setDefiniendo(true)} />}
      />
      {definiendo && <NuevoObjetivo onDefinido={cerrarDefinicion} onCancelar={cerrarDefinicion} />}
      <PendientesDeRespuesta />
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
    </>
  );
}

function ContenidoDeObjetivos({
  objetivos,
  cargando,
}: {
  objetivos: Objetivo[];
  cargando: boolean;
}) {
  if (cargando) {
    return (
      <p role="status" className="objetivos-cargando">
        Cargando tus objetivos…
      </p>
    );
  }
  if (objetivos.length === 0) return <PrimerObjetivo onDefinido={() => undefined} />;
  return <ObjetivosDelNegocio objetivos={objetivos} />;
}

// Todo en una pantalla: los objetivos, cómo vienen y, arriba, el botón para sumar un desafío nuevo.
export function ObjetivosPage() {
  useTituloPagina('Objetivos');
  useMedirObjetivos();
  const { objetivos, cargando } = useObjetivos();
  return (
    <>
      <CabeceraComun active="goals" />
      <main id="main" tabIndex={-1} className="page-wrap objetivos-page reveal">
        <ContenidoDeObjetivos objetivos={objetivos} cargando={cargando} />
        <EntradaDondeAbrir />
      </main>
    </>
  );
}
