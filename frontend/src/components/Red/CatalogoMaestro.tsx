import { useState } from 'react';
import { BarcodeIcon } from '@phosphor-icons/react';
import { Button, EmptyState, InfoTip, Panel } from '@/components/ui';
import { useListas, useProductos, useProveedores } from '@/hooks/red/useRed';

const VISIBLES_AL_INICIO = 4;

// «Un mismo producto, muchos nombres»: el catálogo maestro que arma la red. Los nombres salen de las
// líneas de las listas que reenviaron los proveedores y que ya se emparejaron con tus productos.
export function CatalogoMaestro() {
  const productos = useProductos();
  const listas = useListas();
  const proveedores = useProveedores();
  const [verTodos, setVerTodos] = useState(false);

  const filas = productos
    .map((producto) => {
      const nombres = new Map<string, string>();
      for (const lista of listas) {
        for (const item of lista.items) {
          if (item.productoId !== producto.id || nombres.has(item.textoOriginal)) continue;
          const proveedor = proveedores.find((p) => p.id === lista.proveedorId);
          nombres.set(item.textoOriginal, proveedor?.nombre ?? 'Un proveedor');
        }
      }
      return { producto, nombres: [...nombres.entries()] };
    })
    .filter((fila) => fila.nombres.length > 0);

  const visibles = verTodos ? filas : filas.slice(0, VISIBLES_AL_INICIO);

  return (
    <section aria-labelledby="red-catalogo" className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <div className="flex items-center gap-1">
          <h2 id="red-catalogo" className="m-0 text-h3">
            Un mismo producto, muchos nombres
          </h2>
          <InfoTip tema="catálogo maestro">
            Es la lista única que arma la red: junta en un solo producto todas las formas en que
            cada proveedor lo escribe. Así «Aceite girasol 900 cc» y «ACEITE GIR. 0,9 L» son el
            mismo producto.
          </InfoTip>
        </div>
        <p className="m-0 text-body text-soft">
          Cada proveedor escribe el producto a su manera. Ensambla los junta en un solo catálogo.
          Cuando confirmás un emparejamiento, sirve para todos los comercios que compran ese
          producto.
        </p>
      </div>

      {filas.length === 0 ? (
        <EmptyState
          icon={<BarcodeIcon aria-hidden size={32} />}
          title="Todavía no hay nombres emparejados"
        >
          Cuando cargues la lista de un proveedor, vas a ver acá con qué nombres escribe cada
          producto.
        </EmptyState>
      ) : (
        <>
          <ul className="m-0 flex list-none flex-col gap-3 p-0">
            {visibles.map(({ producto, nombres }) => (
              <li key={producto.id}>
                <Panel tight className="flex flex-col gap-2">
                  <h3 className="m-0 text-body font-bold">{producto.nombre}</h3>
                  <ul
                    aria-label={`Nombres con los que lo escriben los proveedores: ${producto.nombre}`}
                    className="m-0 flex list-none flex-col gap-1.5 p-0"
                  >
                    {nombres.map(([texto, proveedor]) => (
                      <li key={texto} className="flex min-w-0 items-baseline gap-2 text-hint">
                        <span aria-hidden className="font-bold text-accent-800">
                          =
                        </span>
                        <span className="min-w-0 break-words">
                          <span className="font-semibold text-fg">{texto}</span>
                          <span className="text-soft"> · {proveedor}</span>
                        </span>
                      </li>
                    ))}
                  </ul>
                </Panel>
              </li>
            ))}
          </ul>
          {filas.length > VISIBLES_AL_INICIO && (
            <div>
              <Button
                variant="secondary"
                aria-expanded={verTodos}
                onClick={() => setVerTodos((actual) => !actual)}
              >
                {verTodos ? 'Ver menos' : `Ver los ${filas.length} productos`}
              </Button>
            </div>
          )}
        </>
      )}
    </section>
  );
}
