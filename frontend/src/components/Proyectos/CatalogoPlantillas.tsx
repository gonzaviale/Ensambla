import { useNavigate } from '@tanstack/react-router';
import { Badge, Button, Panel } from '@/components/ui';
import { usePlantillas } from '@/hooks/plantillas/queries';
import { useCrearDesdePlantilla } from '@/hooks/proyectos/mutations';
import { CheckCircleIcon } from '@phosphor-icons/react';

export function CatalogoPlantillas() {
  const plantillas = usePlantillas();
  const crear = useCrearDesdePlantilla();
  const navigate = useNavigate();
  async function usar(id: string) {
    try {
      const proyecto = await crear.mutateAsync(id);
      // La plantilla ya trae tablas y columnas: se cae directo en Cargar y ver, con un aviso para cambiarla.
      await navigate({
        to: '/proyectos/$proyectoId/usar',
        params: { proyectoId: proyecto.id },
        search: { nuevo: true },
      });
    } catch {
      /* El error se anuncia debajo del catálogo. */
    }
  }
  return (
    <section id="plantillas" aria-labelledby="plantillas-titulo" className="flex flex-col gap-4">
      <div>
        <h2 id="plantillas-titulo" className="m-0 text-h3">
          Plantillas disponibles
        </h2>
        <p className="m-0 mt-1 text-body text-soft">
          Elegí una base ya armada y adaptala a tu negocio. Tu copia es independiente.
        </p>
      </div>
      {plantillas.length === 0 ? (
        <p className="m-0">Todavía no hay plantillas disponibles. Podés empezar en blanco.</p>
      ) : (
        <ul className="m-0 grid list-none grid-cols-[repeat(auto-fill,minmax(min(100%,18rem),1fr))] gap-4 p-0">
          {plantillas.map((plantilla) => (
            <li key={plantilla.id} className="min-w-0">
              <Panel interactive className="flex h-full flex-col gap-3">
                <Badge tone="ok" icon={<CheckCircleIcon size={16} weight="fill" />}>
                  Ya viene armada
                </Badge>
                <h3 className="m-0 text-h3 break-words">{plantilla.nombre}</h3>
                <p className="m-0 flex-1 text-body text-soft">{plantilla.descripcion}</p>
                <Button
                  onClick={() => void usar(plantilla.id)}
                  disabled={crear.isPending}
                  aria-label={`Usar plantilla ${plantilla.nombre}`}
                >
                  {crear.isPending && crear.variables === plantilla.id
                    ? 'Creando…'
                    : 'Usar esta plantilla'}
                </Button>
              </Panel>
            </li>
          ))}
        </ul>
      )}
      {crear.error && (
        <p role="alert" className="m-0 font-semibold text-bad">
          {crear.error.message}
        </p>
      )}
    </section>
  );
}
