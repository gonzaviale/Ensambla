import { useParams } from '@tanstack/react-router';
import { PageHeading, Panel } from '@/components/ui';
import { useProyecto } from '@/hooks/proyectos/queries';

export function ConstructorPage() {
  const { proyectoId } = useParams({ from: '/_app/proyectos/$proyectoId/construir' });
  const proyecto = useProyecto(proyectoId);
  return (
    <div className="flex flex-col gap-6">
      <PageHeading
        kicker="Modo construir"
        title={proyecto.nombre}
        lead={proyecto.descripcion || 'Organizá tu negocio con bloques y campos.'}
      />
      <h2 className="m-0 text-xl">Constructor de bloques y campos</h2>
      {proyecto.bloques.length === 0 ? (
        <Panel>
          <h3 className="mt-0">Primer paso: agregá un bloque</h3>
          <p>
            Un bloque agrupa información, como Productos, Clientes o Pedidos. Después agregá campos,
            como Nombre, Precio o Teléfono.
          </p>
          <p className="mb-0 text-soft">
            El editor de bloques estará disponible próximamente. Podés volver al inicio y crear otro
            proyecto desde una plantilla con la estructura ya preparada.
          </p>
        </Panel>
      ) : (
        <section aria-label="Bloques del proyecto" className="grid gap-4 sm:grid-cols-2">
          {proyecto.bloques.map((bloque) => (
            <Panel key={bloque.id}>
              <h3 className="mt-0">{bloque.nombre}</h3>
              <ul className="m-0 flex list-none flex-col gap-3 p-0">
                {bloque.campos.map((campo) => (
                  <li
                    key={campo.id}
                    className="flex flex-wrap justify-between gap-2 border-b border-line pb-2"
                  >
                    <span>
                      {campo.nombre}
                      {campo.obligatorio && (
                        <span className="ml-2 text-sm text-muted">(obligatorio)</span>
                      )}
                    </span>
                    <span className="text-sm text-soft">
                      {
                        {
                          TEXTO: 'Texto',
                          NUMERO: 'Número',
                          FECHA: 'Fecha',
                          OPCION: 'Opciones',
                          SI_NO: 'Sí / No',
                        }[campo.tipo]
                      }
                    </span>
                  </li>
                ))}
              </ul>
            </Panel>
          ))}
        </section>
      )}
    </div>
  );
}
