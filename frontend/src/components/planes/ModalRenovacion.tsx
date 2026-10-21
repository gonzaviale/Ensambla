import { Btn } from '@/components/sistema/Btn';
import { Modal } from '@/components/sistema/Modal';
import { useOperacionPago } from '@/hooks/pagos/mutations';
import { useRenovacion } from '@/hooks/pagos/queries';
import { usePlanes } from '@/hooks/planes/queries';
import { useSistemaStore } from '@/stores/sistemaStore';

export function ModalRenovacion({ cancelar = false }: { cancelar?: boolean }) {
  const cerrar = useSistemaStore((estado) => estado.cerrarModal);
  const operacion = useOperacionPago(cancelar ? 'cancelar' : 'contratar');
  const { data: renovacion } = useRenovacion();
  const { data: planes } = usePlanes();
  const premium = planes?.planes.find((plan) => plan.codigo === 'PREMIUM');
  const precio = premium?.precioMensual;
  const precioTexto =
    precio == null
      ? 'Precio por definir'
      : new Intl.NumberFormat('es-AR', {
          style: 'currency',
          currency: 'ARS',
          maximumFractionDigits: 0,
        }).format(precio);
  const finActual =
    planes?.suscripcion.codigoPlanActual === 'PREMIUM' ? planes.suscripcion.finEn : null;
  const fecha = finActual
    ? new Intl.DateTimeFormat('es-AR', {
        dateStyle: 'long',
        timeStyle: 'short',
        hour12: false,
      }).format(new Date(finActual))
    : null;

  return (
    <Modal clase="plan-trial-modal">
      <h2 id="modal-title">{cancelar ? 'Cancelar renovación automática' : 'Contratar Premium'}</h2>
      {cancelar ? (
        <>
          <p>
            Vamos a pedir la cancelación a Mercado Pago. La baja queda confirmada cuando el
            proveedor la acepte.
          </p>
          <p>
            Conservás los días de prueba o el período ya pagado. Tus sistemas y registros se
            mantienen.
          </p>
          <p>Cancelar futuras renovaciones no solicita un reembolso del período actual.</p>
        </>
      ) : (
        <>
          <p>
            <strong>{precioTexto} finales cada 30 días</strong>, con renovación automática en pesos
            argentinos.
          </p>
          <p>
            {fecha ? (
              <>
                El primer cobro se programa para el {fecha}. Respetamos el tiempo que ya tenés
                disponible.
              </>
            ) : (
              <>El primer cobro se solicita al autorizar la suscripción en Mercado Pago.</>
            )}
          </p>
          <p>La autorización no habilita un período pago hasta confirmar el cobro aprobado.</p>
          <p>
            Durante la prueba conservás sus diez páginas totales. Premium pago incluye cien páginas
            por mes calendario UTC.
          </p>
          <p>Podés cancelar las próximas renovaciones desde Planes.</p>
        </>
      )}
      {operacion.isError ? (
        <p className="plan-error" role="alert">
          {operacion.mensajeError}
        </p>
      ) : null}
      <div className="modal-footer">
        <Btn cls="secondary" onClick={cerrar}>
          Volver
        </Btn>
        <Btn
          cls="primary"
          disabled={
            operacion.isPending || (!cancelar && (!renovacion?.habilitada || precio == null))
          }
          aria-busy={operacion.isPending}
          onClick={() => operacion.mutate(undefined, { onSuccess: cerrar })}
        >
          {operacion.isPending
            ? 'Confirmando…'
            : cancelar
              ? 'Confirmar cancelación'
              : 'Preparar contratación'}
        </Btn>
      </div>
    </Modal>
  );
}
