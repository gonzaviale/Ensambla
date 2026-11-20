import { leerErrorServidor } from '@/hooks/errores';
import { useEliminarNegocio } from '@/hooks/negocios/mutations';
import { useNegociosDeLaCuenta } from '@/hooks/negocios/queries';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Icon } from '../Icon';
import { Modal } from '../Modal';

export function ModalEliminarNegocio({ negocioId, nombre }: { negocioId: string; nombre: string }) {
  const cerrar = useSistemaStore((estado) => estado.cerrarModal);
  const { data } = useNegociosDeLaCuenta();
  const eliminar = useEliminarNegocio();
  const error = leerErrorServidor(eliminar.error);
  const esUltimo = data.negocios.length === 1;

  const confirmar = async () => {
    try {
      await eliminar.mutateAsync(negocioId);
      cerrar();
    } catch {
      // La mutación conserva el mensaje del servidor para mostrarlo sin cerrar el diálogo.
    }
  };

  return (
    <Modal clase="modal-negocio modal-eliminar-negocio">
      <div className="icon-tile large"><Icon name="trash" /></div>
      <h2 id="modal-title">¿Eliminar {nombre}?</h2>
      <p>Esta acción elimina el negocio y todo lo que anotaste en él.</p>
      <div className="negocio-eliminar-detalle">
        <Icon name="help" />
        <p>
          También se eliminan sus <strong>tablas y todo lo que cargaste en ellas</strong>. Tu cuenta y tus otros
          negocios no cambian.
        </p>
      </div>
      {esUltimo && <p className="form-error" role="alert">No podés eliminar el único negocio de tu cuenta.</p>}
      {error && <p className="form-error" role="alert">{error.mensaje}</p>}
      <div className="modal-footer">
        <button type="button" className="btn ghost" disabled={eliminar.isPending} onClick={cerrar}>
          No, conservarlo
        </button>
        <button type="button" className="btn danger" disabled={esUltimo || eliminar.isPending}
          onClick={() => void confirmar()}>
          {eliminar.isPending ? 'Eliminando…' : 'Sí, eliminar negocio'}
        </button>
      </div>
    </Modal>
  );
}
