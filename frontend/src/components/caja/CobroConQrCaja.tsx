import type { ComponentType } from 'react';
import { Link } from '@tanstack/react-router';
import { CodigoQr } from '@/components/cobro/CodigoQr';
import { useCancelarCobroQr } from '@/hooks/caja/useCancelarCobroQr';
import { useCobroConQr, type EstadoDelCobroQr } from '@/hooks/caja/useCobroConQr';
import { useCopiarAlPortapapeles } from '@/hooks/useCopiarAlPortapapeles';
import { dineroCaja } from '@/lib/caja';
import { horaDelVencimiento } from '@/lib/caja/cobroQr';
import type { CobroQr } from '@/types/caja';
import { DialogoCaja } from './DialogoCaja';

interface Props {
  importeCentavos: number;
  ocupada: boolean;
  incierta: boolean;
  error: string;
  onYaTransfirieron: (cobroQrId: string) => void;
  onCerrar: () => void;
}

interface PropsDeLaPantalla extends Props {
  cobro?: CobroQr;
}

const PANTALLAS: Record<EstadoDelCobroQr, ComponentType<PropsDeLaPantalla>> = {
  cargando: PantallaGenerando,
  sinAlias: PantallaSinAlias,
  fallo: PantallaConFallo,
  listo: PantallaDelCobro,
};

export function CobroConQrCaja(props: Props) {
  const { id, estado, cobro } = useCobroConQr(props.importeCentavos);
  const { cancelar } = useCancelarCobroQr();
  const Pantalla = PANTALLAS[estado];
  const cerrar = () => {
    if (cobro) cancelar(id);
    props.onCerrar();
  };

  return (
    <DialogoCaja
      titulo="Cobrar con QR"
      clase="caja-dialogo-qr"
      onCerrar={cerrar}
      bloqueado={props.ocupada}
    >
      <Pantalla {...props} onCerrar={cerrar} cobro={cobro} />
    </DialogoCaja>
  );
}

function PantallaGenerando({ onCerrar }: PropsDeLaPantalla) {
  return (
    <>
      <p role="status">Generando el QR…</p>
      <AccionVolver onCerrar={onCerrar} />
    </>
  );
}

function PantallaSinAlias({ onCerrar }: PropsDeLaPantalla) {
  return (
    <>
      <p>
        Todavía no cargaste tu alias o CVU. Lo necesitamos para mostrarle al cliente dónde pagar.
      </p>
      <Link className="text-link" to="/cuenta" search={{ seccion: 'perfil' }}>
        Cargar mi alias o CVU
      </Link>
      <AccionVolver onCerrar={onCerrar} />
    </>
  );
}

function PantallaConFallo({ onCerrar }: PropsDeLaPantalla) {
  return (
    <>
      <p role="alert" className="caja-error">
        No pudimos generar el QR. Probá de nuevo en un rato.
      </p>
      <AccionVolver onCerrar={onCerrar} />
    </>
  );
}

function AccionVolver({ onCerrar }: { onCerrar: () => void }) {
  return (
    <div className="caja-acciones">
      <button type="button" className="btn secondary" onClick={onCerrar}>
        Volver
      </button>
    </div>
  );
}

function PantallaDelCobro(props: PropsDeLaPantalla) {
  const { cobro, importeCentavos, ocupada, incierta, error, onYaTransfirieron, onCerrar } = props;
  if (!cobro) return null;

  return (
    <>
      <div className="caja-qr">
        <span className="caja-antetitulo">MONTO A TRANSFERIR</span>
        <p className="caja-qr-monto">{dineroCaja(importeCentavos)}</p>
        <CodigoQr texto={cobro.alias} descripcion="QR con tu alias o CVU para transferir" />
        <DatosParaTransferir cobro={cobro} />
        <p className="caja-nota">
          Ensambla no verificó el pago. Revisá en tu cuenta que te llegó antes de confirmar.
        </p>
        {error && (
          <p className="caja-error" role="alert">
            {error}
          </p>
        )}
      </div>
      <div className="caja-acciones">
        <button type="button" className="btn secondary" disabled={ocupada} onClick={onCerrar}>
          Cancelar cobro con QR
        </button>
        <button
          type="button"
          className="btn primary"
          disabled={ocupada}
          onClick={() => onYaTransfirieron(cobro.id)}
        >
          {textoDelBotonDeConfirmar(ocupada, incierta)}
        </button>
      </div>
    </>
  );
}

function textoDelBotonDeConfirmar(ocupada: boolean, incierta: boolean): string {
  if (ocupada) return 'Confirmando…';
  if (incierta) return 'Verificar misma operación';
  return 'Ya me transfirieron';
}

function DatosParaTransferir({ cobro }: { cobro: CobroQr }) {
  const { copiar, resultado } = useCopiarAlPortapapeles('Alias copiado.');

  return (
    <div className="caja-qr-datos">
      <span>Alias o CVU</span>
      <strong>{cobro.alias}</strong>
      <button type="button" className="btn secondary" onClick={() => void copiar(cobro.alias)}>
        Copiar alias
      </button>
      {cobro.titular && <span>Titular: {cobro.titular}</span>}
      <span role="status">{resultado}</span>
      <span>Vale hasta las {horaDelVencimiento(cobro.venceEn)}.</span>
    </div>
  );
}
