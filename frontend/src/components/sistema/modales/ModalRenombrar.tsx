import { useState, type FormEvent } from 'react';
import { Modal } from '../Modal';
import { seleccionarSistemaActivo, useSistemaStore } from '@/stores/sistemaStore';

export function ModalRenombrar() {
  const sistema = useSistemaStore(seleccionarSistemaActivo);
  const commit = useSistemaStore((estado) => estado.commit);
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
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
    <Modal>
      <h2 id="modal-title">Tu sistema tiene nombre.</h2>
      <p>Elegí uno que reconozcas fácilmente.</p>
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
        <div className="modal-footer">
          <button className="btn primary" type="submit">
            Guardar nombre
          </button>
        </div>
      </form>
    </Modal>
  );
}
