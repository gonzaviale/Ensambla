import { useState } from 'react';
import { AyudaComoFunciona } from '@/components/Cuenta/AyudaComoFunciona';
import { ModoDemostracion } from '@/components/Cuenta/ModoDemostracion';
import { Privacidad } from '@/components/Cuenta/Privacidad';
import { TuNegocio } from '@/components/Cuenta/TuNegocio';
import { TuPlan } from '@/components/Cuenta/TuPlan';
import { PageHeading, Toast } from '@/components/ui';

// Mi cuenta: tu negocio (nombre del sistema), tu plan, ayuda, privacidad y modo demostración.
export function CuentaPage() {
  const [aviso, setAviso] = useState<string | null>(null);

  return (
    <div className="mx-auto flex w-full max-w-3xl animate-fade-in flex-col gap-6 py-2">
      <PageHeading title="Mi cuenta" lead="Tu negocio, tu plan y la ayuda, en un solo lugar." />
      <TuNegocio onGuardado={setAviso} />
      <TuPlan />
      <AyudaComoFunciona />
      <Privacidad />
      <ModoDemostracion onHecho={setAviso} />
      {aviso && <Toast tipo="exito" mensaje={aviso} onClose={() => setAviso(null)} />}
    </div>
  );
}
