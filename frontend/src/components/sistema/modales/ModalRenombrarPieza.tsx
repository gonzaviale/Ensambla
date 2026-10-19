import { useState, type FormEvent } from 'react';
import { findBlock } from '@/lib/sistema/core';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Modal } from '../Modal';

// «Ponelo en tus palabras»: el nombre de una pieza de la propuesta de Sami.
export function ModalRenombrarPieza({ bloqueId }: { bloqueId: string }) {
  const borrador = useSistemaStore((estado) => estado.taller?.borrador);
  const editarBorrador = useSistemaStore((estado) => estado.editarBorrador);
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const [nombre, setNombre] = useState(findBlock(borrador ?? undefined, bloqueId)?.name ?? '');

  const guardar = (evento: FormEvent) => {
    evento.preventDefault();
    const limpio = nombre.trim();
    if (!limpio) return;
    editarBorrador((copia) => {
      const bloque = findBlock(copia, bloqueId);
      if (bloque) bloque.name = limpio;
    });
    cerrarModal();
  };

  return (
    <Modal>
      <h2 id="modal-title">Ponelo en tus palabras.</h2>
      <p>Elegí un nombre que te resulte familiar.</p>
      <form onSubmit={guardar}>
        <div className="field">
          <label htmlFor="draft-block-name">Nombre de la pieza</label>
          <input
            className="input"
            id="draft-block-name"
            name="name"
            required
            maxLength={80}
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
