import { useState, type FormEvent } from 'react';
import { Icon } from '../Icon';
import { Modal } from '../Modal';
import { varsDeColor } from '../colores';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { allBlocks } from '@/lib/sistema/core';
import { seleccionarSistemaActivo, useSistemaStore } from '@/stores/sistemaStore';

const registros = (cantidad: number) => (cantidad === 1 ? '1 registro' : `${cantidad} registros`);

// Eliminar un cuaderno no tiene vuelta atrás: se muestran sus tablas y hay que escribir el nombre exacto
// para habilitar el botón. El borrado en el servidor lo manda useSincronizarSistemas.
export function ModalEliminarCuaderno() {
  const sistema = useSistemaStore(seleccionarSistemaActivo);
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const navegacion = useNavegacion();
  const [escrito, setEscrito] = useState('');

  if (!sistema) return null;
  const tablas = allBlocks(sistema);
  const coincide = escrito.trim() === sistema.name.trim();

  const eliminar = (evento: FormEvent) => {
    evento.preventDefault();
    if (!coincide) return;
    const { id, name } = sistema;
    // Primero se sale de la pantalla del cuaderno; si no, al desaparecer manda al inicio.
    void navegacion.irAMisSistemas().then(() => {
      const estado = useSistemaStore.getState();
      estado.eliminarSistema(id);
      estado.avisar(`Eliminamos «${name}».`);
    });
  };

  return (
    <Modal clase="modal-eliminar-cuaderno">
      <div className="icon-tile large" style={varsDeColor('coral')}>
        <Icon name="trash" />
      </div>
      <h2 id="modal-title">¿Eliminar «{sistema.name}»?</h2>
      <p>
        Se borra el cuaderno con todo lo que tiene adentro. <strong>No se puede deshacer.</strong>
      </p>
      {tablas.length > 0 ? (
        <>
          <p className="eliminar-cuaderno-subtitulo">
            {tablas.length === 1
              ? 'Esta tabla se pierde:'
              : `Estas ${tablas.length} tablas se pierden:`}
          </p>
          <ul className="eliminar-cuaderno-tablas" aria-label="Tablas que se eliminan">
            {tablas.map((tabla) => (
              <li key={tabla.id}>
                <span className="eliminar-cuaderno-punto" style={varsDeColor(tabla.color)} />
                <span className="eliminar-cuaderno-nombre">{tabla.name}</span>
                {tabla.fields.length > 0 && (
                  <span className="eliminar-cuaderno-cantidad">{registros(tabla.rows.length)}</span>
                )}
              </li>
            ))}
          </ul>
        </>
      ) : (
        <p className="eliminar-cuaderno-subtitulo">Este cuaderno no tiene tablas.</p>
      )}
      <form onSubmit={eliminar}>
        <div className="field">
          <label htmlFor="confirmar-nombre">
            Para confirmar, escribí <strong>{sistema.name}</strong>
          </label>
          <input
            className="input"
            id="confirmar-nombre"
            name="confirmar"
            autoComplete="off"
            spellCheck={false}
            value={escrito}
            onChange={(evento) => setEscrito(evento.target.value)}
          />
        </div>
        <div className="modal-footer">
          <button type="button" className="btn secondary" onClick={cerrarModal}>
            Cancelar
          </button>
          <button type="submit" className="btn peligro" disabled={!coincide}>
            Eliminar para siempre
          </button>
        </div>
      </form>
    </Modal>
  );
}
