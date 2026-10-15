import { Link } from '@tanstack/react-router';
import { ArrowRightIcon } from '@phosphor-icons/react';
import { Panel } from '@/components/ui';
import { useListas, useProveedores } from '@/hooks/red/useRed';
import { fechaLarga, pesos } from '@/lib/red/formato';
import type { CambioAplicado } from '@/types/red';

type FilaCambioProps = {
  cambio: CambioAplicado;
  onCambiar: (trasladado: boolean) => void;
};

// Un precio para cambiar en el local: grande y legible, con la casilla «ya lo cambié» y de qué lista salió.
export function FilaCambio({ cambio, onCambiar }: FilaCambioProps) {
  const listas = useListas();
  const proveedores = useProveedores();
  const lista = listas.find((l) => l.id === cambio.listaId);
  const proveedor = proveedores.find((p) => p.id === lista?.proveedorId);
  const idCasilla = `trasladado-${cambio.id}`;

  return (
    <Panel tight className={`flex flex-col gap-3 ${cambio.trasladado ? 'bg-canvas' : ''}`.trim()}>
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <h3 className="m-0 min-w-0 text-h3 break-words">{cambio.nombre}</h3>
        <p className="m-0 flex flex-wrap items-center gap-2 text-h3">
          <span className="text-soft line-through decoration-2">
            {pesos(cambio.precioAnterior)}
          </span>
          <ArrowRightIcon aria-hidden size={22} weight="bold" />
          <span className="sr-only">pasa a</span>
          <strong className="text-h2 font-extrabold">{pesos(cambio.precioNuevo)}</strong>
        </p>
      </div>

      <p className="m-0 text-hint text-soft">
        {cambio.listaId ? (
          <>
            De la lista de {proveedor?.nombre ?? 'tu proveedor'} del {fechaLarga(cambio.fecha)}.{' '}
            <Link to="/listas/$listaId" params={{ listaId: cambio.listaId }}>
              Ver la lista
              <span className="sr-only"> de {cambio.nombre}</span>
            </Link>
          </>
        ) : (
          <>Cambio que hiciste vos el {fechaLarga(cambio.fecha)}.</>
        )}
      </p>

      <label
        htmlFor={idCasilla}
        className="flex min-h-12 w-fit cursor-pointer items-center gap-3 text-body font-semibold"
      >
        <input
          id={idCasilla}
          type="checkbox"
          checked={cambio.trasladado}
          onChange={(event) => onCambiar(event.target.checked)}
          className="size-6 shrink-0 cursor-pointer accent-accent-700"
        />
        <span>
          Ya lo cambié en el local<span className="sr-only"> ({cambio.nombre})</span>
        </span>
      </label>
    </Panel>
  );
}
