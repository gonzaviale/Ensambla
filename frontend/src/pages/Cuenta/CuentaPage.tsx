import { useState } from 'react';
import { AyudaComoFunciona } from '@/components/Cuenta/AyudaComoFunciona';
import { DatosComercio } from '@/components/Cuenta/DatosComercio';
import { ModoDemostracion } from '@/components/Cuenta/ModoDemostracion';
import { Privacidad } from '@/components/Cuenta/Privacidad';
import { TuPlan } from '@/components/Cuenta/TuPlan';
import { PageHeading, Toast } from '@/components/ui';
import { useUsuario } from '@/hooks/auth/queries';

// Mi cuenta (T-041): datos del comercio, plan, ayuda, privacidad y modo demostración.
export function CuentaPage() {
  const usuario = useUsuario();
  const [aviso, setAviso] = useState<string | null>(null);

  return (
    <div className="mx-auto flex w-full max-w-3xl animate-fade-in flex-col gap-6 px-4 py-8">
      <PageHeading
        title="Mi cuenta"
        lead={`Ingresaste como ${usuario.nombre} (${usuario.email}).`}
      />
      <DatosComercio onGuardado={setAviso} />
      <TuPlan />
      <AyudaComoFunciona />
      <Privacidad />
      <ModoDemostracion onHecho={setAviso} />
      {aviso && <Toast tipo="exito" mensaje={aviso} onClose={() => setAviso(null)} />}
    </div>
  );
}
