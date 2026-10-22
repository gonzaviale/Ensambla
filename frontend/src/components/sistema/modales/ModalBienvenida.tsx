import { useNombre } from '@/hooks/sistemas/useNavegacion';
import { useAccionesDelTutorial } from '@/hooks/tutorial/useAccionesDelTutorial';
import { useSistemaStore } from '@/stores/sistemaStore';
import { BrandMark } from '../BrandMark';
import { Icon } from '../Icon';
import { Modal } from '../Modal';
import { Sami } from '../Sami';

// La bienvenida de la primera vez: empezar con la guía o explorar por cuenta propia.
export function ModalBienvenida() {
  const nombre = useNombre();
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const { empezar } = useAccionesDelTutorial();

  return (
    <Modal ancho clase="welcome-modal">
      <div className="demo-welcome">
        <div className="welcome-brand" aria-label="Ensambla">
          <BrandMark />
          <span>Ensambla</span>
        </div>
        <div className="welcome-hello">
          <Sami />
          <span className="welcome-bubble">Lo armamos juntos.</span>
        </div>
        <span className="eyebrow">TU SISTEMA, UN BLOQUE A LA VEZ.</span>
        <h2 id="modal-title">
          ¡Hola, {nombre}!
          <br />
          Bienvenido a Ensambla.
        </h2>
        <p>
          Soy Sami. Te ayudo a darle forma a tu manera de trabajar.
          <br />
          ¿Cómo querés empezar?
        </p>
        <div className="welcome-choices">
          <button type="button" className="welcome-choice guided" autoFocus onClick={empezar}>
            <span className="icon-tile">
              <Icon name="spark" />
            </span>
            <span>
              <strong>Hacer mi primer sistema con guía</strong>
              <small>Lo probamos paso a paso, haciendo de verdad.</small>
            </span>
            <Icon name="arrow" />
          </button>
          <button type="button" className="welcome-choice" onClick={cerrarModal}>
            <span className="icon-tile">
              <Icon name="grid" />
            </span>
            <span>
              <strong>Explorar por mi cuenta</strong>
              <small>Recorré las opciones. Sami sigue a mano.</small>
            </span>
            <Icon name="arrow" />
          </button>
        </div>
        <p className="welcome-note">
          Podés crear con Sami, elegir una plantilla o empezar de cero.
        </p>
      </div>
    </Modal>
  );
}
