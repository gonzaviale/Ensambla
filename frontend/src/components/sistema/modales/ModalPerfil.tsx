import { useState, type FormEvent } from 'react';
import { Link } from '@tanstack/react-router';
import { Btn } from '../Btn';
import { Modal } from '../Modal';
import { useCerrarSesion } from '@/hooks/auth/mutations';
import { useGuardarPreferencias } from '@/hooks/preferencias/mutations';
import { usePreferencias } from '@/hooks/preferencias/queries';
import { useNombre } from '@/hooks/sistemas/useNavegacion';
import { useSistemaStore } from '@/stores/sistemaStore';

// «¿Cómo te llamás?» del mockup. Con la sesión real, acá también está la salida de la cuenta.
export function ModalPerfil() {
  const nombreActual = useNombre();
  const preferencias = usePreferencias();
  const guardarPreferencias = useGuardarPreferencias();
  const { cerrarSesion, cerrando } = useCerrarSesion();
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const [nombre, setNombre] = useState(nombreActual);

  const guardar = (evento: FormEvent) => {
    evento.preventDefault();
    const limpio = nombre.trim();
    if (!limpio) return;
    guardarPreferencias.mutate({ ...preferencias, nombre: limpio });
    cerrarModal();
  };

  return (
    <Modal>
      <h2 id="modal-title">¿Cómo te llamás?</h2>
      <p>Así podemos darte la bienvenida en tu espacio.</p>
      <form onSubmit={guardar}>
        <div className="field">
          <label htmlFor="profile-name">Tu nombre</label>
          <input
            className="input"
            id="profile-name"
            name="name"
            required
            maxLength={40}
            value={nombre}
            onChange={(evento) => setNombre(evento.target.value)}
          />
        </div>
        <div className="modal-footer spread">
          <Btn cls="secondary" disabled={cerrando} onClick={() => void cerrarSesion()}>
            Cerrar sesión
          </Btn>
          <button className="btn primary" type="submit">
            Guardar
          </button>
        </div>
      </form>
      <Link className="text-link" to="/ubicacion" onClick={cerrarModal}>
        Ubicación y rubro de mi comercio
      </Link>
    </Modal>
  );
}
