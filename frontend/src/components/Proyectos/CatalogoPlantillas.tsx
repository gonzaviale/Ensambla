import { useNavigate } from '@tanstack/react-router';
import { Button, Panel } from '@/components/ui';
import { usePlantillas } from '@/hooks/plantillas/queries';
import { useCrearDesdePlantilla } from '@/hooks/proyectos/mutations';

export function CatalogoPlantillas() {
  const plantillas = usePlantillas();
  const crear = useCrearDesdePlantilla();
  const navigate = useNavigate();
  async function usar(id: string) {
    try {
      const proyecto = await crear.mutateAsync(id);
      // La plantilla ya trae bloques y datos: se cae directo en Usar, con un aviso para cambiarla.
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
    <section
      id="plantillas"
      aria-labelledby="plantillas-titulo"
      className="flex scroll-mt-6 flex-col gap-4"
    >
      <div>
        <h2 id="plantillas-titulo" className="m-0 text-xl">
          Plantillas disponibles
        </h2>
        <p className="mb-0 text-soft">
          Elegí una base y adaptala a tu negocio. Tu copia es independiente.
        </p>
      </div>
      {plantillas.length === 0 ? (
        <p>Todavía no hay plantillas disponibles. Podés empezar en blanco.</p>
      ) : (
        <ul className="m-0 grid list-none gap-4 p-0 sm:grid-cols-2 lg:grid-cols-3">
          {plantillas.map((plantilla) => (
            <li key={plantilla.id}>
              <Panel className="flex h-full flex-col gap-3">
                <h3 className="m-0 text-lg">{plantilla.nombre}</h3>
                <p className="m-0 flex-1 text-soft">{plantilla.descripcion}</p>
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
        <p role="alert" className="text-bad">
          {crear.error.message}
        </p>
      )}
    </section>
  );
}
