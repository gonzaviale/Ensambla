import { useParams } from '@tanstack/react-router';
import { PageHeading } from '@/components/ui';
import { PendingTask } from '@/components/ui/PendingTask';

// T-031 y T-032: vista de operación (formulario de carga y tabla de registros), sin herramientas de construcción.
export function UsarPage() {
  const { proyectoId } = useParams({ from: '/_app/proyectos/$proyectoId/usar' });

  return (
    <div className="flex flex-col gap-6">
      <PageHeading
        kicker="Modo usar"
        title="Cargá y consultá tus datos"
        lead={`Proyecto ${proyectoId}`}
      />
      <PendingTask tarea="T-031" title="Modo usar" />
    </div>
  );
}
