import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { Modal } from '@/components/sistema/Modal';
import { useIniciarPrueba } from '@/hooks/planes/mutations';
import { usePlanes } from '@/hooks/planes/queries';
import { useSistemaStore } from '@/stores/sistemaStore';

export function ModalPruebaPremium() {
  const cerrar = useSistemaStore((estado) => estado.cerrarModal);
  const activacion = useIniciarPrueba();
  const { data } = usePlanes();
  const activa = data?.suscripcion.pruebaActiva;

  return (
    <Modal clase="plan-trial-modal">
      <span className="plan-trial-symbol">
        <Icon name="spark" />
      </span>
      <h2 id="modal-title">Probá Premium durante 7 días</h2>
      <p>Explorá las herramientas premium con los datos de tu negocio.</p>
      <ul className="plan-trial-conditions">
        <li>
          <strong>7 días exactos</strong> desde que confirmás.
        </li>
        <li>
          <strong>10 páginas de IA en total</strong> para toda la prueba, sin renovación mensual.
        </li>
        <li>Una sola prueba por negocio. Sin tarjeta ni cobro automático.</li>
        <li>Al terminar volvés al gratuito y tus datos se conservan.</li>
      </ul>
      {activa ? (
        <p role="status" className="plan-success">
          Tu prueba Premium ya está activa.
        </p>
      ) : null}
      {activacion.isError && !activa ? (
        <p role="alert" className="plan-error">
          {activacion.mensajeError}
        </p>
      ) : null}
      <div className="modal-footer">
        <Btn cls="secondary" onClick={cerrar}>
          {activa ? 'Listo' : 'Ahora no'}
        </Btn>
        {!activa ? (
          <Btn
            cls="primary"
            disabled={activacion.isPending || !data?.suscripcion.puedeIniciarPrueba}
            aria-busy={activacion.isPending}
            onClick={() => activacion.mutate(undefined, { onSuccess: cerrar })}
          >
            {activacion.isPending ? 'Activando…' : 'Activar prueba gratis'}
          </Btn>
        ) : null}
      </div>
    </Modal>
  );
}
