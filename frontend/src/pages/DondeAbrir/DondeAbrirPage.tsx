import type { ReactNode } from 'react';
import { PageFooter } from '@/components/sistema/PageFooter';
import {
  useRecorridoDondeAbrir,
  type Paso,
  type Recorrido,
} from '@/hooks/dondeAbrir/useRecorridoDondeAbrir';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { ConfirmarNegocio } from './ConfirmarNegocio';
import { EncabezadoDondeAbrir, VolverAMiZona } from './EncabezadoDondeAbrir';
import { PreguntasDeSami } from './PreguntasDeSami';
import { ResultadoDondeAbrir } from './ResultadoDondeAbrir';

function PasoConfirmar({ recorrido }: { recorrido: Recorrido }) {
  return (
    <ConfirmarNegocio rubro={recorrido.filtros.rubro} onConfirmar={recorrido.confirmarRubro} />
  );
}

function PasoPreguntas({ recorrido }: { recorrido: Recorrido }) {
  return (
    <PreguntasDeSami
      respuestas={recorrido.respuestas}
      onResponder={recorrido.responder}
      onTerminar={recorrido.irAlResultado}
    />
  );
}

const VISTA_DEL_PASO: Record<Paso, (props: { recorrido: Recorrido }) => ReactNode> = {
  confirmar: PasoConfirmar,
  preguntas: PasoPreguntas,
  resultado: ResultadoDondeAbrir,
};

// «Dónde abrir»: Sami confirma el negocio, pregunta, recomienda zonas y explica qué hay que tramitar para abrir en la
// que se elija. Cada filtro que se mueve recalcula todo al instante. Es una demostración con datos de ejemplo.
export function DondeAbrirPage() {
  useTituloPagina('Tu próximo negocio');
  const recorrido = useRecorridoDondeAbrir();
  const VistaDelPaso = VISTA_DEL_PASO[recorrido.paso];
  return (
    <main id="main" tabIndex={-1} className="page-wrap objetivos-page donde-page reveal">
      <VolverAMiZona />
      <EncabezadoDondeAbrir paso={recorrido.paso} />
      <VistaDelPaso recorrido={recorrido} />
      <PageFooter />
    </main>
  );
}
