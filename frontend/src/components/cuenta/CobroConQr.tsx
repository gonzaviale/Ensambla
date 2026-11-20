import type { FormEvent } from 'react';
import { CodigoQrDiferido } from '@/components/cobro/CodigoQrDiferido';
import { useConfiguracionNegocio } from '@/hooks/negocio/queries';
import { useBorradorDelCobro } from '@/hooks/negocio/useBorradorDelCobro';
import { useGuardarCobroConQr } from '@/hooks/negocio/useGuardarCobroConQr';
import type { ConfiguracionNegocio } from '@/types/negocio';

type Borrador = ReturnType<typeof useBorradorDelCobro>;

export function CobroConQr() {
  const negocio = useConfiguracionNegocio();
  if (!negocio.data) return null;
  return <FormularioDelCobro negocio={negocio.data} />;
}

function FormularioDelCobro({ negocio }: { negocio: ConfiguracionNegocio }) {
  const borrador = useBorradorDelCobro(negocio);
  const { guardar, guardando, fallo } = useGuardarCobroConQr();
  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    guardar(borrador.cambios);
  };

  return (
    <form className="cuenta-tarjeta" onSubmit={enviar} aria-labelledby="cuenta-cobro-qr-titulo">
      <h2 id="cuenta-cobro-qr-titulo">Cobro con QR</h2>
      <p className="cuenta-ayuda">
        Cargá dónde te transfieren. En la caja mostramos un QR y el monto; vos confirmás cuando te
        llega.
      </p>
      <CamposDelCobro borrador={borrador} />
      <VistaPreviaDelQr alias={borrador.cambios.cobroAlias ?? ''} />
      {fallo && <AvisoDeFalloAlGuardar />}
      <div className="cuenta-acciones">
        <button className="btn primary" type="submit" disabled={!borrador.hayCambios || guardando}>
          Guardar cobro con QR
        </button>
      </div>
      <p className="input-hint">
        Ensambla no verifica el pago ni toca tu plata: va directo a tu cuenta.
      </p>
    </form>
  );
}

function CamposDelCobro({ borrador }: { borrador: Borrador }) {
  return (
    <>
      <CampoDelCobro
        id="cuenta-cobro-alias"
        etiqueta="Alias o CVU"
        ayuda="Escribilo igual que lo usás para cobrar."
        valor={borrador.alias}
        alCambiar={borrador.setAlias}
      />
      <CampoDelCobro
        id="cuenta-cobro-titular"
        etiqueta="Nombre del titular (opcional)"
        valor={borrador.titular}
        alCambiar={borrador.setTitular}
      />
    </>
  );
}

interface PropsDelCampo {
  id: string;
  etiqueta: string;
  ayuda?: string;
  valor: string;
  alCambiar: (valor: string) => void;
}

function CampoDelCobro({ id, etiqueta, ayuda, valor, alCambiar }: PropsDelCampo) {
  return (
    <div className="field">
      <label htmlFor={id}>{etiqueta}</label>
      <input
        className="input"
        id={id}
        maxLength={120}
        value={valor}
        onChange={(evento) => alCambiar(evento.target.value)}
      />
      {ayuda && <span className="input-hint">{ayuda}</span>}
    </div>
  );
}

function VistaPreviaDelQr({ alias }: { alias: string }) {
  if (!alias)
    return <p className="input-hint">Cuando cargues tu alias o CVU, acá ves un QR de prueba.</p>;
  return (
    <figure className="cobro-qr-previa">
      <CodigoQrDiferido texto={alias} descripcion="QR de prueba con tu alias o CVU" />
      <figcaption>
        QR de prueba. Escanealo con tu celular: tiene que aparecer tu alias o CVU.
      </figcaption>
    </figure>
  );
}

function AvisoDeFalloAlGuardar() {
  return (
    <p role="alert" className="input-hint">
      No pudimos guardar tu cobro con QR. Probá de nuevo en un rato.
    </p>
  );
}
