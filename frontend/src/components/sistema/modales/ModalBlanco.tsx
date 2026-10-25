import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { IconTile } from '../IconTile';
import { Modal } from '../Modal';
import { varsDeColor } from '../colores';
import { makeSystem } from '@/lib/sistema/core';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';

export function ModalBlanco() {
  const { irATaller, irAPlantillas, crearYAbrir } = useNavegacion();
  return (
    <Modal>
      <div className="icon-tile large" style={varsDeColor('petrol')}>
        <Icon name="grid" />
      </div>
      <h2 id="modal-title">Un lienzo en blanco, todo tuyo.</h2>
      <p>Vas a poder arrastrar piezas, modificar sus campos y organizar tu sistema en secciones.</p>
      <div className="blank-warning">
        <strong>¿Es tu primera vez?</strong>
        Te recomendamos empezar con la guía de Sami o una plantilla. Después podés personalizar todo
        igual.
      </div>
      <button type="button" className="suggestion" onClick={() => void irATaller()}>
        <IconTile tipo="form" />
        <div>
          <strong>Mejor, voy con Sami</strong>
          <p>Contale tu idea y recibí una propuesta.</p>
        </div>
        <Icon name="arrow" />
      </button>
      <button type="button" className="suggestion" onClick={() => void irAPlantillas()}>
        <IconTile tipo="inventory" />
        <div>
          <strong>Quiero ver las plantillas</strong>
          <p>Depósito, Almacén o kiosco, o Pedidos y clientes.</p>
        </div>
        <Icon name="arrow" />
      </button>
      <div className="modal-footer">
        <Btn cls="primary" onClick={() => void crearYAbrir(makeSystem('Mi cuaderno'), 'build')}>
          Seguir desde cero <Icon name="arrow" />
        </Btn>
      </div>
    </Modal>
  );
}
