import { PageHeading, Panel } from '@/components/ui';
import { PendingTask } from '@/components/ui/PendingTask';
import { usePlantillas } from '@/hooks/plantillas/queries';

// T-015 (listado de proyectos y estado vacío) y T-016 (catálogo de plantillas).
// Por ahora muestra las plantillas del backend: sirve de ejemplo de query → hook → página.
export function ProyectosPage() {
  const plantillas = usePlantillas();

  return (
    <div className="flex flex-col gap-6">
      <PageHeading
        kicker="Mis proyectos"
        title="¿Por dónde querés empezar?"
        lead="Elegí una plantilla o empezá un proyecto en blanco."
      />
      <PendingTask tarea="T-015" title="Listado de proyectos y estado vacío" />
      <section aria-labelledby="plantillas-titulo" className="flex flex-col gap-3">
        <h2 id="plantillas-titulo" className="m-0 text-[17px]">
          Plantillas disponibles
        </h2>
        {plantillas.length === 0 ? (
          <p className="text-soft">Todavía no hay plantillas cargadas (seeds de T-013).</p>
        ) : (
          <ul className="m-0 grid list-none gap-3 p-0 sm:grid-cols-2 lg:grid-cols-3">
            {plantillas.map((plantilla) => (
              <li key={plantilla.id}>
                <Panel className="h-full">
                  <h3 className="m-0 text-[16px]">{plantilla.nombre}</h3>
                  <p className="mt-1 text-soft">{plantilla.descripcion}</p>
                </Panel>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
