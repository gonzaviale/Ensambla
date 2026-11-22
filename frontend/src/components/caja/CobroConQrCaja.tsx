import { useEffect, useRef, type ComponentType } from 'react';
import { CodigoQrDiferido } from '@/components/cobro/CodigoQrDiferido';
import { useCancelarCobroQr } from '@/hooks/caja/useCancelarCobroQr';
import { useCobroConQr, type EstadoDelCobroQr } from '@/hooks/caja/useCobroConQr';
import { useVigenciaCobroQr } from '@/hooks/caja/useVigenciaCobroQr';
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
  fallo: PantallaConFallo,
  listo: PantallaDelCobro,
};

export function CobroConQrCaja(props: Props) {
  const { id, estado, cobro } = useCobroConQr(props.importeCentavos);
  const { cancelar } = useCancelarCobroQr();
  const Pantalla = PANTALLAS[estado];
  const confirmado = useRef(false);
  useEffect(() => {
    if (cobro?.estado === 'COBRADO' && !confirmado.current) {
      confirmado.current = true;
      props.onYaTransfirieron(cobro.id);
    }
  }, [cobro, props]);
  const cerrar = () => {
    if (props.ocupada || props.incierta) return;
    if (cobro?.estado === 'PENDIENTE' || cobro?.estado === 'VENCIDO') cancelar(id);
    props.onCerrar();
  };

  return (
    <DialogoCaja
      titulo="Cobrar con QR"
      clase="caja-dialogo-qr"
      onCerrar={cerrar}
      bloqueado={props.ocupada || props.incierta}
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
  const { cobro, importeCentavos, ocupada, incierta, error, onCerrar, onYaTransfirieron } = props;
  if (!cobro) return null;
  const permiteReintentarGuardado = cobro.estado === 'COBRADO' && !!error && !ocupada;

  return (
    <>
      <div className="caja-qr">
        <span className="caja-antetitulo">MONTO A PAGAR</span>
        <p className="caja-qr-monto">{dineroCaja(importeCentavos)}</p>
        {cobro.qrDinamico && (
          <CodigoQrDiferido
            texto={cobro.qrDinamico}
            descripcion="QR dinámico de Mercado Pago con el importe de la compra"
          />
        )}
        <p className="caja-ayuda">
          Escanealo con Mercado Pago. El importe ya está cargado y no hace falta escribirlo.
        </p>
        {cobro.qrEstatico && (
          <details className="caja-qr-datos">
            <summary>Usar el QR fijo del mostrador</summary>
            <CodigoQrDiferido
              texto={cobro.qrEstatico}
              descripcion="QR fijo de la caja de Mercado Pago"
            />
            <span>El QR fijo y el dinámico cobran esta misma compra. Solo uno puede pagarse.</span>
          </details>
        )}
        <EstadoDelPago cobro={cobro} />
        <p className="caja-nota">
          Ensambla confirmará la venta automáticamente cuando Mercado Pago acredite el pago.
        </p>
        {error && (
          <p className="caja-error" role="alert">
            {error}
          </p>
        )}
      </div>
      <div className="caja-acciones">
        <button
          type="button"
          className="btn secondary"
          disabled={ocupada || incierta}
          onClick={onCerrar}
        >
          Cancelar cobro con QR
        </button>
        <button
          type="button"
          className="btn primary"
          disabled={!permiteReintentarGuardado}
          onClick={() => onYaTransfirieron(cobro.id)}
        >
          {ocupada
            ? 'Guardando venta…'
            : permiteReintentarGuardado
              ? incierta
                ? 'Verificar y guardar venta'
                : 'Volver a guardar la venta'
              : cobro.estado === 'COBRADO'
                ? 'Pago acreditado'
                : 'Esperando el pago…'}
        </button>
      </div>
    </>
  );
}

function EstadoDelPago({ cobro }: { cobro: CobroQr }) {
  const vencido = useVigenciaCobroQr(cobro.venceEn);
  if (cobro.estado === 'COBRADO') return <p role="status">Pago acreditado por Mercado Pago.</p>;
  if (cobro.estado === 'CANCELADO') return <p role="status">Este cobro fue cancelado.</p>;
  if (cobro.estado === 'REEMBOLSADO') return <p role="status">Este pago fue reembolsado.</p>;
  if (vencido || cobro.estado === 'VENCIDO') {
    return <p role="status">Este QR venció. Cancelalo y generá un cobro nuevo.</p>;
  }
  return <p role="status">Esperando el pago. Vale hasta las {horaDelVencimiento(cobro.venceEn)}.</p>;
}
