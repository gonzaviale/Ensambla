import { useRouterState } from '@tanstack/react-router';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { Modal } from '../Modal';
import { Sami } from '../Sami';
import { varsDeColor } from '../colores';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useSistemaStore } from '@/stores/sistemaStore';

const RESPUESTAS: Record<string, string> = {
  blocks:
    'En Construir, traé una pieza de la izquierda. Para poner dos juntas, soltá una en el lado izquierdo o derecho de otra. Los botones ½, ⅔ y 1 cambian el ancho. También podés arrastrar la esquina inferior de cada bloque.',
  sections:
    'Una sección es una página de tu sistema. Podés tener, por ejemplo, Ventas en una y Stock en otra. En Construir → Secciones podés crearlas, renombrarlas y reordenarlas. En los ajustes de un bloque elegís a qué sección pertenece.',
  fields:
    'Abrí los ajustes de un bloque. En Campos podés cambiar nombres y tipos, agregar opciones o marcar un dato como obligatorio. Un vínculo te permite elegir, por ejemplo, un cliente al registrar una venta.',
  data: 'Usá Registrar para cargar un dato. Para probar la carga con una foto, elegí Traer datos de una foto: revisás los registros de ejemplo y confirmás qué incorporar.',
};

type Tema = [clave: string, icono: NombreDeIcono, etiqueta: string];

const TEMAS_DEL_EDITOR: Tema[] = [
  ['blocks', 'box', 'Agregar y mover bloques'],
  ['sections', 'folder', 'Separar el sistema en secciones'],
  ['fields', 'settings', 'Cambiar los campos'],
  ['data', 'image', 'Cargar mis primeros datos'],
];

const TEMAS_DEL_INICIO: Tema[] = [
  ['guide', 'spark', 'Crear un sistema conmigo'],
  ['templates', 'grid', 'Encontrar una plantilla simple'],
];

export function ModalAyuda({ respuesta }: { respuesta?: string }) {
  const enElEditor = useRouterState({
    select: (estado) => estado.location.pathname.startsWith('/sistemas/'),
  });
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const { irATaller, irAPlantillas } = useNavegacion();

  const elegir = (clave: string) => {
    if (clave === 'guide') void irATaller();
    else if (clave === 'templates') void irAPlantillas();
    else abrirModal({ tipo: 'ayuda', respuesta: clave });
  };

  return (
    <Modal>
      <div className="sami-intro">
        <Sami />
        <div>
          <h2 id="modal-title">¿En qué te doy una mano?</h2>
          <p>Te ayudo a encontrar el próximo paso.</p>
        </div>
      </div>
      {respuesta && RESPUESTAS[respuesta] && (
        <div className="help-answer">{RESPUESTAS[respuesta]}</div>
      )}
      {(enElEditor ? TEMAS_DEL_EDITOR : TEMAS_DEL_INICIO).map(([clave, icono, etiqueta]) => (
        <button key={clave} type="button" className="suggestion" onClick={() => elegir(clave)}>
          <span className="icon-tile" style={varsDeColor('petrol')}>
            <Icon name={icono} />
          </span>
          <strong>{etiqueta}</strong>
          <Icon name="chevron" />
        </button>
      ))}
      {enElEditor && (
        <div className="modal-footer">
          <Btn
            cls="secondary"
            onClick={() =>
              useSistemaStore.getState().avisar('La carga desde una foto llega con el editor.')
            }
          >
            <Icon name="camera" /> Probar una foto
          </Btn>
          <Btn cls="primary" onClick={cerrarModal}>
            Listo, sigo armando
          </Btn>
        </div>
      )}
    </Modal>
  );
}
