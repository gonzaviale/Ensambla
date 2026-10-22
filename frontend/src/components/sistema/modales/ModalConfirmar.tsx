import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { Modal } from '../Modal';
import { varsDeColor } from '../colores';
import { useSistemaStore } from '@/stores/sistemaStore';

type ModalConfirmarProps = {
  titulo: string;
  mensaje: string;
  etiqueta: string;
  onConfirmar: () => void;
};

export function ModalConfirmar({ titulo, mensaje, etiqueta, onConfirmar }: ModalConfirmarProps) {
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  return (
    <Modal>
      <div className="icon-tile large" style={varsDeColor('coral')}>
        <Icon name="help" />
      </div>
      <h2 id="modal-title">{titulo}</h2>
      <p>{mensaje}</p>
      <div className="modal-footer">
        <Btn cls="secondary" onClick={cerrarModal}>
          Cancelar
        </Btn>
        <Btn
          cls="danger"
          onClick={() => {
            cerrarModal();
            onConfirmar();
          }}
        >
          {etiqueta}
        </Btn>
      </div>
    </Modal>
  );
}
