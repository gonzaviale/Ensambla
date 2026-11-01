import { CONSEJOS_DE_FOTO } from '@/lib/sistema/consejosDeFoto';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { Modal } from '../Modal';
import { varsDeColor } from '../colores';

// Antes de elegir la foto para Sami: cómo sacarla para que se lea bien. «Tomar foto» abre el selector.
export function ModalConsejosDeFoto({ onTomarFoto }: { onTomarFoto: () => void }) {
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);

  const tomarFoto = () => {
    cerrarModal();
    onTomarFoto();
  };

  return (
    <Modal>
      <div className="icon-tile" style={varsDeColor('petrol')}>
        <Icon name="bulb" />
      </div>
      <h2 id="modal-title">Para que Sami lea bien tu foto</h2>
      <p>Unos consejos rápidos antes de sacarla:</p>
      <ul className="photo-tips-list">
        {CONSEJOS_DE_FOTO.map((consejo) => (
          <li key={consejo}>{consejo}</li>
        ))}
      </ul>
      <div className="modal-footer">
        <Btn cls="primary" data-accion="guide-photo-take" onClick={tomarFoto}>
          <Icon name="camera" /> Tomar foto
        </Btn>
      </div>
    </Modal>
  );
}
