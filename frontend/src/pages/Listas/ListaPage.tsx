import { useState } from 'react';
import { Link, useParams } from '@tanstack/react-router';
import { ListMagnifyingGlassIcon } from '@phosphor-icons/react';
import { ListaCerrada } from '@/components/Listas/ListaCerrada';
import { RevisionDeLista } from '@/components/Listas/RevisionDeLista';
import { EmptyState, PageHeading, buttonClasses } from '@/components/ui';
import { useLista } from '@/hooks/red/useRed';

// Revisar una lista de un proveedor: qué cambió → dudas → tus precios → confirmar.
export function ListaPage() {
  const { listaId } = useParams({ from: '/_app/listas/$listaId' });
  // Con otra lista se vuelve a empezar desde el primer paso.
  return <DetalleDeLista key={listaId} listaId={listaId} />;
}

function DetalleDeLista({ listaId }: { listaId: string }) {
  const lista = useLista(listaId);
  // Si se abrió para revisarla, la revisión sigue montada al aplicarla: ahí se muestra el «Listo».
  const [abiertaParaRevisar] = useState(lista?.estado === 'por-revisar');

  if (!lista) {
    return (
      <div className="flex flex-col gap-6">
        <PageHeading title="No encontramos esa lista" />
        <EmptyState
          icon={<ListMagnifyingGlassIcon aria-hidden size={32} />}
          title="Puede que ya no esté o que el enlace sea viejo"
        >
          <p className="m-0">Mirá las listas de tus proveedores para encontrar la que buscás.</p>
          <Link to="/listas" className={buttonClasses({ variant: 'solid', className: 'mt-3' })}>
            Ir a Listas
          </Link>
        </EmptyState>
      </div>
    );
  }

  if (abiertaParaRevisar || lista.estado === 'por-revisar')
    return <RevisionDeLista lista={lista} />;
  return <ListaCerrada lista={lista} />;
}
