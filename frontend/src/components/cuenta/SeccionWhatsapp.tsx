import { useState } from 'react';
import { leerErrorServidor } from '@/hooks/errores';
import { useVinculacionWhatsapp } from '@/hooks/whatsapp/queries';
import type { VerificacionWhatsapp } from '@/types/whatsapp';
import { Btn } from '../sistema/Btn';
import { CargarNumero } from './whatsapp/CargarNumero';
import { IngresarCodigo } from './whatsapp/IngresarCodigo';
import { NumeroVinculado } from './whatsapp/NumeroVinculado';
import { VincularConCodigo } from './whatsapp/VincularConCodigo';

type Vinculacion = ReturnType<typeof useVinculacionWhatsapp>;

function ConsultandoElNumero() {
  return <p role="status">Consultando tu número…</p>;
}

function ErrorAlConsultar({ vinculacion }: { vinculacion: Vinculacion }) {
  return (
    <div role="alert">
      <p>{leerErrorServidor(vinculacion.error)?.mensaje}</p>
      <Btn cls="secondary small" onClick={() => void vinculacion.refetch()}>
        Reintentar
      </Btn>
    </div>
  );
}

function PasosParaVincular() {
  const [verificacion, setVerificacion] = useState<VerificacionWhatsapp>();
  const paso = verificacion ? (
    <IngresarCodigo
      verificacion={verificacion}
      onReenviada={setVerificacion}
      onCambiarNumero={() => setVerificacion(undefined)}
    />
  ) : (
    <CargarNumero onEnviada={setVerificacion} />
  );

  return (
    <>
      {paso}
      <VincularConCodigo />
    </>
  );
}

function contenidoSegun(vinculacion: Vinculacion) {
  if (vinculacion.isPending) return <ConsultandoElNumero />;
  if (vinculacion.isError) return <ErrorAlConsultar vinculacion={vinculacion} />;
  if (vinculacion.data) return <NumeroVinculado vinculo={vinculacion.data} />;
  return <PasosParaVincular />;
}

export function SeccionWhatsapp() {
  const vinculacion = useVinculacionWhatsapp();

  return (
    <section className="cuenta-tarjeta" aria-labelledby="cuenta-whatsapp-titulo">
      <h2 id="cuenta-whatsapp-titulo">WhatsApp</h2>
      {contenidoSegun(vinculacion)}
    </section>
  );
}
