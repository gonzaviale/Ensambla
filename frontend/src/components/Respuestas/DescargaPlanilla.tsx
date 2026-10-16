import { DownloadSimpleIcon } from '@phosphor-icons/react';
import { Button, Panel, Toast } from '@/components/ui';
import { RequierePlan } from '@/components/Plan/PlanGate';
import { useDescargarReporte } from '@/hooks/reportes/mutations';

// Planilla (XLSX) de una tabla del sistema: es del plan Negocio.
export function DescargaPlanilla({ bloqueId, nombre }: { bloqueId: string; nombre: string }) {
  return (
    <RequierePlan
      funcion="descarga-xlsx"
      beneficio="Descargá tus datos en una planilla para guardarlos o mandárselos a tu contador."
    >
      <Descargar bloqueId={bloqueId} nombre={nombre} />
    </RequierePlan>
  );
}

function Descargar({ bloqueId, nombre }: { bloqueId: string; nombre: string }) {
  const descarga = useDescargarReporte(bloqueId);
  return (
    <Panel flat aria-labelledby="descarga-planilla" className="flex flex-col items-start gap-3">
      <h2 id="descarga-planilla" className="m-0 text-h3">
        Llevate tus datos
      </h2>
      <p className="m-0 text-body text-soft">Descargá {nombre} en una planilla.</p>
      <Button variant="primary" loading={descarga.isPending} onClick={() => descarga.mutate({})}>
        <DownloadSimpleIcon aria-hidden size={20} weight="bold" />
        Descargar planilla de {nombre}
      </Button>
      {descarga.isError && (
        <Toast
          tipo="error"
          mensaje="No pudimos armar la planilla. Probá de nuevo."
          onClose={() => descarga.reset()}
        />
      )}
    </Panel>
  );
}
