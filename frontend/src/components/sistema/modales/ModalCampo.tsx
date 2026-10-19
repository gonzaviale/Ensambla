import { useState, type FormEvent } from 'react';
import { FIELD_TYPES } from '@/lib/sistema/catalogo';
import { allBlocks, findBlock } from '@/lib/sistema/core';
import type { Campo } from '@/lib/sistema/tipos';
import { guardarCampo, pedirQuitarCampo } from '@/stores/accionesDelEditor';
import { seleccionarSistemaActivo, useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { Modal } from '../Modal';
import { varsDeColor } from '../colores';

// Agregar o editar un campo del bloque elegido.
export function ModalCampo({ bloqueId, clave }: { bloqueId: string; clave?: string | null }) {
  const sistema = useSistemaStore(seleccionarSistemaActivo);
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const bloque = findBlock(sistema, bloqueId);
  const existente = clave ? bloque?.fields.find((campo) => campo.key === clave) : undefined;

  const [label, setLabel] = useState(existente?.label ?? '');
  const [tipo, setTipo] = useState<Campo['type']>(existente?.type ?? 'text');
  const [opciones, setOpciones] = useState((existente?.options ?? []).join(', '));
  const [objetivo, setObjetivo] = useState(existente?.target ?? '');
  const [requerido, setRequerido] = useState(existente?.required ?? false);
  const [error, setError] = useState('');

  if (!bloque || !sistema) return null;
  const destinos = allBlocks(sistema).filter(
    (otro) => otro.id !== bloque.id && otro.fields.length && otro.type !== 'summary',
  );

  const guardar = (evento: FormEvent) => {
    evento.preventDefault();
    const problema = guardarCampo(bloque.id, clave ?? null, {
      label,
      type: tipo,
      required: requerido,
      options: opciones,
      target: objetivo,
    });
    if (problema) setError(problema);
  };

  return (
    <Modal>
      <div className="icon-tile" style={varsDeColor('petrol')}>
        <Icon name="form" />
      </div>
      <h2 id="modal-title">{clave ? 'Editar' : 'Agregar'} campo</h2>
      <p>Elegí qué dato querés registrar en {bloque.name.toLowerCase()}.</p>
      <form onSubmit={guardar} noValidate>
        <div className="field">
          <label htmlFor="field-name">Nombre del campo</label>
          <input
            className="input"
            id="field-name"
            name="label"
            maxLength={60}
            required
            placeholder="Por ejemplo, Fecha de entrega"
            value={label}
            onChange={(evento) => setLabel(evento.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="field-type">Tipo de dato</label>
          <select
            id="field-type"
            name="type"
            value={tipo}
            onChange={(evento) => setTipo(evento.target.value as Campo['type'])}
          >
            {Object.entries(FIELD_TYPES).map(([valor, etiqueta]) => (
              <option key={valor} value={valor}>
                {etiqueta}
              </option>
            ))}
          </select>
        </div>
        <div className="field" id="options-field" hidden={tipo !== 'select'}>
          <label htmlFor="field-options">Opciones, separadas por coma</label>
          <textarea
            id="field-options"
            name="options"
            placeholder="Pendiente, En curso, Terminado"
            value={opciones}
            onChange={(evento) => setOpciones(evento.target.value)}
          />
        </div>
        <div className="field" id="target-field" hidden={tipo !== 'relation'}>
          <label htmlFor="field-target">Bloque para vincular</label>
          <select
            id="field-target"
            name="target"
            value={objetivo}
            onChange={(evento) => setObjetivo(evento.target.value)}
          >
            <option value="">Elegir bloque…</option>
            {destinos.map((destino) => (
              <option key={destino.id} value={destino.id}>
                {destino.name}
              </option>
            ))}
          </select>
          {!destinos.length && (
            <span className="input-hint">
              Agregá otro bloque con registros para poder vincularlo.
            </span>
          )}
        </div>
        <label className="check-row">
          <input
            type="checkbox"
            name="required"
            checked={requerido}
            onChange={(evento) => setRequerido(evento.target.checked)}
          />
          Este dato es obligatorio
        </label>
        <div className="form-error" role="alert">
          {error}
        </div>
        <div className="modal-footer spread">
          {clave ? (
            <Btn cls="danger small" onClick={() => pedirQuitarCampo(bloque.id, clave)}>
              <Icon name="trash" /> Quitar campo
            </Btn>
          ) : (
            <span />
          )}
          <div className="row">
            <Btn cls="secondary" onClick={cerrarModal}>
              Cancelar
            </Btn>
            <button className="btn primary" type="submit">
              Guardar campo
            </button>
          </div>
        </div>
      </form>
    </Modal>
  );
}
