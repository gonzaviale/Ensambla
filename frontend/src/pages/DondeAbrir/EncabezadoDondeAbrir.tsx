import { ArrowLeftIcon } from '@phosphor-icons/react';
import type { Paso } from '@/hooks/dondeAbrir/useRecorridoDondeAbrir';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';

const PASOS: { clave: Paso; titulo: string }[] = [
  { clave: 'confirmar', titulo: 'Confirmá tu negocio' },
  { clave: 'preguntas', titulo: 'Contanos qué buscás' },
  { clave: 'resultado', titulo: 'Compará zonas' },
];

export function VolverAMiZona() {
  const navegacion = useNavegacion();
  return (
    <button type="button" className="objetivos-volver" onClick={() => void navegacion.irAZona()}>
      <ArrowLeftIcon aria-hidden size={20} /> Volver a Mi zona
    </button>
  );
}

function ProgresoDelRecorrido({ paso }: { paso: Paso }) {
  return (
    <ol className="donde-pasos-cabeza" aria-label="Progreso de Tu próximo negocio">
      {PASOS.map((item) => (
        <li key={item.clave} aria-current={item.clave === paso ? 'step' : undefined}>
          {item.titulo}
        </li>
      ))}
    </ol>
  );
}

export function EncabezadoDondeAbrir({ paso }: { paso: Paso }) {
  return (
    <header className="donde-encabezado">
      <div>
        <h1>Tu próximo negocio</h1>
        <p className="donde-bajada">
          Compará zonas y entendé qué necesitás para abrir un nuevo local.
        </p>
      </div>
      <ProgresoDelRecorrido paso={paso} />
    </header>
  );
}
