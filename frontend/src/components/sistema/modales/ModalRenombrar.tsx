import { useState, type FormEvent } from 'react';
import { Icon } from '../Icon';
import { Modal } from '../Modal';
import { seleccionarSistemaActivo, useSistemaStore } from '@/stores/sistemaStore';

// El lápiz al lado del nombre: se configura el cuaderno (el nombre) y, más abajo, se puede eliminar.
export function ModalRenombrar() {
  const sistema = useSistemaStore(seleccionarSistemaActivo);
  const commit = useSistemaStore((estado) => estado.commit);
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const [nombre, setNombre] = useState(sistema?.name ?? '');

  const guardar = (evento: FormEvent) => {
    evento.preventDefault();
    const limpio = nombre.trim();
    if (!limpio) return;
    commit((s) => {
      s.name = limpio;
    });
    cerrarModal();
  };

  return (
    <Modal clase="modal-configurar-cuaderno">
      <h2 id="modal-title">Configurar cuaderno</h2>
      <p>Elegí un nombre que reconozcas fácilmente.</p>
      <form onSubmit={guardar}>
        <div className="field">
          <label htmlFor="system-name">Nombre</label>
          <input
            className="input"
            id="system-name"
            name="name"
            required
            maxLength={100}
            value={nombre}
            onChange={(evento) => setNombre(evento.target.value)}
          />
        </div>
          <button className="btn primary" type="submit">
            Guardar nombre
          </button>
        <div className="modal-footer">
        </div>
      </form>
      <section className="zona-peligro" aria-labelledby="zona-peligro-titulo">
        <div>
          <h3 id="zona-peligro-titulo">Eliminar este cuaderno</h3>
          <p>Se borra con todas sus tablas y registros. No se puede deshacer.</p>
        </div>
        <button
          type="button"
          className="btn peligro"
          onClick={() => abrirModal({ tipo: 'eliminarCuaderno' })}
        >
          <Icon name="trash" /> Eliminar cuaderno
        </button>
      </section>
    </Modal>
  );
}
