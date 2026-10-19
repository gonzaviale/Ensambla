import { useState, type FormEvent } from 'react';
import { PRESETS_DE_SECCION } from '@/lib/sistema/catalogo';
import { agregarSeccion } from '@/stores/accionesDelEditor';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { Modal } from '../Modal';
import { varsDeColor } from '../colores';

export function ModalAgregarSeccion() {
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const [nombre, setNombre] = useState('');
  const [inicio, setInicio] = useState('empty');

  const guardar = (evento: FormEvent) => {
    evento.preventDefault();
    const limpio = nombre.trim();
    if (limpio) agregarSeccion(limpio, inicio);
  };

  return (
    <Modal>
      <div className="icon-tile" style={varsDeColor('petrol')}>
        <Icon name="folder" />
      </div>
      <h2 id="modal-title">Agregá una sección.</h2>
      <p>Separá lo que querés ver en distintas páginas. Por ejemplo: Ventas, Stock o Clientes.</p>
      <form onSubmit={guardar}>
        <div className="field">
          <label htmlFor="section-name">Nombre de la sección</label>
          <input
            className="input"
            id="section-name"
            name="name"
            placeholder="Por ejemplo, Ventas"
            required
            maxLength={50}
            value={nombre}
            onChange={(evento) => setNombre(evento.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="section-starter">¿Con qué querés empezar?</label>
          <select
            id="section-starter"
            name="starter"
            value={inicio}
            onChange={(evento) => setInicio(evento.target.value)}
          >
            <option value="empty">Una sección vacía</option>
            {Object.entries(PRESETS_DE_SECCION).map(([clave, preset]) => (
              <option key={clave} value={clave}>
                {preset.label}
              </option>
            ))}
          </select>
        </div>
        <div className="modal-footer">
          <Btn cls="secondary" onClick={cerrarModal}>
            Cancelar
          </Btn>
          <button className="btn primary" type="submit">
            <Icon name="plus" />
            Agregar sección
          </button>
        </div>
      </form>
    </Modal>
  );
}
