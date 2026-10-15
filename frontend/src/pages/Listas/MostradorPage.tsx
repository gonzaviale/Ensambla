import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { CaretDownIcon, CheckCircleIcon, ListChecksIcon } from '@phosphor-icons/react';
import { EtiquetasParaImprimir } from '@/components/Listas/EtiquetasParaImprimir';
import { FilaCambio } from '@/components/Listas/FilaCambio';
import { Button, EmptyState, InfoTip, PageHeading, Toast, buttonClasses } from '@/components/ui';
import { useAccionesRed, useCambios, useNegocio } from '@/hooks/red/useRed';
import { plural } from '@/lib/red/formato';

// Cambios para el mostrador: los precios que ya se cambiaron en el sistema y falta cambiar en el local.
export function MostradorPage() {
  const cambios = useCambios();
  const negocio = useNegocio();
  const { marcarTrasladado, marcarTodosTrasladados } = useAccionesRed();
  const [deshacer, setDeshacer] = useState<string[] | null>(null);

  // Un cambio de sólo el costo (el precio quedó igual) no obliga a tocar ninguna etiqueta.
  const conPrecioNuevo = cambios.filter((c) => c.precioNuevo !== c.precioAnterior);
  const pendientes = conPrecioNuevo.filter((c) => !c.trasladado);
  const trasladados = conPrecioNuevo.filter((c) => c.trasladado);

  function marcarTodos() {
    const ids = pendientes.map((c) => c.id);
    marcarTodosTrasladados();
    setDeshacer(ids);
  }

  return (
    <div className="flex flex-col gap-8">
      <PageHeading
        kicker="En el local"
        title="Cambios para el mostrador"
        lead="Estos son los precios que ya cambiamos en tu sistema. Falta que los cambies en las etiquetas del local."
        actions={
          pendientes.length > 0 ? (
            <Button variant="solid" onClick={marcarTodos}>
              <CheckCircleIcon aria-hidden size={22} weight="bold" />
              Marcar todos como cambiados
            </Button>
          ) : undefined
        }
      />

      <section
        aria-labelledby="por-cambiar"
        data-recorrido="mostrador-lista"
        className="flex flex-col gap-4"
      >
        <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
          <div className="flex items-center gap-1">
            <h2 id="por-cambiar" className="m-0 text-h2">
              Por cambiar
            </h2>
            <InfoTip tema="traslado al mostrador">
              Trasladar es cambiar la etiqueta o el cartel en el local para que coincida con el
              precio nuevo del sistema. Marcalo cuando lo hacés y así sabés qué te falta.
            </InfoTip>
          </div>
          <p className="m-0 text-body font-semibold" aria-live="polite">
            {pendientes.length > 0
              ? `Te ${pendientes.length === 1 ? 'falta' : 'faltan'} ${pendientes.length}`
              : 'No te falta ninguno'}
          </p>
        </div>

        {pendientes.length === 0 ? (
          <EmptyState
            icon={<ListChecksIcon aria-hidden size={32} />}
            title="No te falta cambiar ninguna etiqueta"
          >
            <p className="m-0">
              Cuando apliques una lista, los precios nuevos aparecen acá para que los cambies en el
              local.
            </p>
            <Link to="/listas" className={buttonClasses({ variant: 'primary', className: 'mt-3' })}>
              Ver las listas
            </Link>
          </EmptyState>
        ) : (
          <ul className="m-0 flex list-none flex-col gap-3 p-0">
            {pendientes.map((cambio) => (
              <li key={cambio.id}>
                <FilaCambio
                  cambio={cambio}
                  onCambiar={(trasladado) => marcarTrasladado(cambio.id, trasladado)}
                />
              </li>
            ))}
          </ul>
        )}
      </section>

      <EtiquetasParaImprimir cambios={pendientes} nombreDelNegocio={negocio.nombre} />

      {trasladados.length > 0 && (
        <details className="group rounded-lg border border-line bg-surface">
          <summary className="flex min-h-14 cursor-pointer list-none items-center justify-between gap-3 rounded-lg px-5 py-3 text-h3 font-bold marker:hidden">
            <span>Ya trasladados ({trasladados.length})</span>
            <CaretDownIcon
              aria-hidden
              size={24}
              weight="bold"
              className="transition-transform group-open:rotate-180"
            />
          </summary>
          <div className="flex flex-col gap-3 px-5 pb-5">
            <p className="m-0 text-hint text-soft">
              {plural(trasladados.length, 'precio ya cambiado', 'precios ya cambiados')} en el
              local. Si te equivocaste, desmarcá la casilla y vuelve a la lista de arriba.
            </p>
            <ul className="m-0 flex list-none flex-col gap-3 p-0">
              {trasladados.map((cambio) => (
                <li key={cambio.id}>
                  <FilaCambio
                    cambio={cambio}
                    onCambiar={(trasladado) => marcarTrasladado(cambio.id, trasladado)}
                  />
                </li>
              ))}
            </ul>
          </div>
        </details>
      )}

      {deshacer && (
        <Toast
          tipo="exito"
          mensaje={`Marcaste ${plural(deshacer.length, 'precio', 'precios')} como cambiados.`}
          accion={{
            etiqueta: 'Deshacer',
            onClick: () => {
              deshacer.forEach((id) => marcarTrasladado(id, false));
              setDeshacer(null);
            },
          }}
          onClose={() => setDeshacer(null)}
        />
      )}
    </div>
  );
}
