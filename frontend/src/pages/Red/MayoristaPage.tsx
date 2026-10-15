import { Link } from '@tanstack/react-router';
import { InfoIcon } from '@phosphor-icons/react';
import { DemandaZona } from '@/components/Red/DemandaZona';
import { PROVEEDOR_DEMO_ID } from '@/components/Red/datosDemo';
import { PublicarOferta } from '@/components/Red/PublicarOferta';
import { ReglasDeOro } from '@/components/Red/ReglasDeOro';
import { SubirListaMayorista } from '@/components/Red/SubirListaMayorista';
import { EmptyState, PageHeading, buttonClasses } from '@/components/ui';
import { useProveedor } from '@/hooks/red/useRed';

// Vista del mayorista (demostración): muestra el otro lado de la red, el que hace que cada lista
// subida llegue a todos los comercios que le compran.
export function MayoristaPage() {
  const proveedor = useProveedor(PROVEEDOR_DEMO_ID);

  return (
    <div className="flex flex-col gap-8">
      <PageHeading
        kicker="Para mayoristas"
        title="Vista del mayorista"
        lead={
          proveedor
            ? `Así trabaja ${proveedor.nombre} en Ensambla: sube su lista una vez y publica ofertas que llegan sólo a quien las necesita.`
            : 'Así trabaja un mayorista en Ensambla.'
        }
        actions={
          <Link to="/red" className={buttonClasses({ variant: 'secondary' })}>
            Volver a Mi red
          </Link>
        }
      />

      <p
        role="note"
        className="m-0 flex items-start gap-3 rounded-lg border border-amber/60 bg-amber-100 p-4 text-body font-semibold text-amber-700"
      >
        <InfoIcon aria-hidden size={26} weight="fill" className="shrink-0" />
        Vista de demostración: así ve Ensambla un mayorista
      </p>

      {!proveedor ? (
        <EmptyState
          icon={<InfoIcon aria-hidden size={32} />}
          title="No encontramos al mayorista de la demostración"
        >
          Reiniciá la demostración desde Mi cuenta para volver a cargarlo.
        </EmptyState>
      ) : (
        <>
          <SubirListaMayorista />
          <PublicarOferta />
          <DemandaZona />
          <ReglasDeOro />
        </>
      )}
    </div>
  );
}
