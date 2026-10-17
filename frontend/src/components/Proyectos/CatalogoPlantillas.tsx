import { useNavigate } from '@tanstack/react-router';
import { Badge, Button, Panel } from '@/components/ui';
import { usePlantillas } from '@/hooks/plantillas/queries';
import { useCrearDesdePlantilla } from '@/hooks/proyectos/mutations';
import { ArrowRightIcon, CheckCircleIcon, TableIcon } from '@phosphor-icons/react';

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
    <section
      id="plantillas"
      aria-labelledby="plantillas-titulo"
      className="proyectos-catalogo flex flex-col gap-5"
    >
      <div className="proyectos-catalogo-heading">
        <span className="proyectos-eyebrow">Piezas que ya encajan</span>
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
        <ul className="proyectos-card-grid">
          {plantillas.map((plantilla) => (
            <li key={plantilla.id} className="min-w-0">
              <Panel interactive className="proyectos-plantilla flex h-full flex-col gap-3">
                <div className="proyectos-plantilla-visual" aria-hidden="true">
                  <div className="proyectos-plantilla-window">
                    <i />
                    <i />
                    <i />
                  </div>
                  <div className="proyectos-plantilla-pieces">
                    {[0, 1, 2].map((pieza) => (
                      <div key={pieza}>
                        <TableIcon size={22} />
                        <i />
                        <i />
                      </div>
                    ))}
                  </div>
                </div>
                <Badge tone="ok" icon={<CheckCircleIcon size={16} weight="fill" />}>
                  Ya viene armada
                </Badge>
                <h3 className="m-0 text-h3 break-words">{plantilla.nombre}</h3>
                <p className="m-0 flex-1 text-body text-soft">{plantilla.descripcion}</p>
                <Button
                  variant="primary"
                  onClick={() => void usar(plantilla.id)}
                  disabled={crear.isPending}
                  aria-label={`Usar plantilla ${plantilla.nombre}`}
                  className="proyectos-plantilla-action"
                >
                  {crear.isPending && crear.variables === plantilla.id
                    ? 'Creando…'
                    : 'Usar esta plantilla'}
                  <ArrowRightIcon size={19} aria-hidden />
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
