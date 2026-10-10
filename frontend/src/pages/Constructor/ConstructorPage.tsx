import { useParams } from '@tanstack/react-router';
import { PageHeading } from '@/components/ui';
import { PendingTask } from '@/components/ui/PendingTask';

// T-019 a T-021: lista de bloques a la izquierda, campos del bloque seleccionado a la derecha.
export function ConstructorPage() {
  const { proyectoId } = useParams({ from: '/_app/proyectos/$proyectoId/construir' });

  return (
    <div className="flex flex-col gap-6">
      <PageHeading
        kicker="Modo construir"
        title="Constructor de bloques y campos"
        lead={`Proyecto ${proyectoId}`}
      />
      <PendingTask tarea="T-019" title="Layout del constructor" />
    </div>
  );
}
