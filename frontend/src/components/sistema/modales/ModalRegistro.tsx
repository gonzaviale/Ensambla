import { useEffect, useRef, useState, type FormEvent } from 'react';
import { CATALOG } from '@/lib/sistema/catalogo';
import { findBlock } from '@/lib/sistema/core';
import { hoy } from '@/lib/sistema/formato';
import type { Fila } from '@/lib/sistema/tipos';
import {
  guardarRegistro,
  guardarRegistroDelBorrador,
  pedirQuitarRegistro,
} from '@/stores/accionesDelEditor';
import { seleccionarSistemaActivo, useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { CampoDeFormulario } from '../editor/CampoDeFormulario';
import { enfocarPrimerError, leerValores } from '../editor/leerFormulario';
import { Icon } from '../Icon';
import { IconTile } from '../IconTile';
import { Modal } from '../Modal';

const AYUDA: Record<string, string> = {
  entries: 'La cantidad se suma al stock del producto.',
  exits: 'La cantidad se descuenta del stock disponible.',
  sales: 'La cantidad se descuenta del stock del producto.',
};

type Props = {
  bloqueId: string;
  filaId?: string | null;
  /** El registro es de la propuesta de Sami (todavía no es un sistema creado), no del sistema abierto. */
  enBorrador?: boolean;
};

// «Registrar» y «Editar» un registro de un bloque.
export function ModalRegistro({ bloqueId, filaId, enBorrador = false }: Props) {
  const sistema = useSistemaStore((estado) =>
    enBorrador ? (estado.taller?.borrador ?? undefined) : seleccionarSistemaActivo(estado),
  );
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const bloque = findBlock(sistema, bloqueId);
  const formulario = useRef<HTMLFormElement>(null);
  const [errores, setErrores] = useState<Record<string, string>>({});

  // Una fila nueva arranca con la fecha de hoy y la primera opción de cada lista.
  const [inicial] = useState<Fila | undefined>(() => {
    if (!bloque) return undefined;
    if (filaId) return bloque.rows.find((fila) => fila.id === filaId);
    const nueva: Fila = { id: '', date: hoy(), time: '09:00' };
    for (const campo of bloque.fields)
      if (campo.type === 'select') nueva[campo.key] = campo.options?.[0] ?? '';
    return nueva;
  });

  useEffect(() => {
    if (!bloque || !inicial) cerrarModal();
  }, [bloque, inicial, cerrarModal]);
  if (!bloque || !inicial) return null;

  const guardar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const resultado = (enBorrador ? guardarRegistroDelBorrador : guardarRegistro)(
      bloque.id,
      leerValores(evento.currentTarget, bloque),
      filaId ?? null,
    );
    if (resultado) {
      setErrores(resultado);
      requestAnimationFrame(() => enfocarPrimerError(formulario.current));
    }
  };

  return (
    <Modal>
      <IconTile tipo={bloque.type} />
      <h2 id="modal-title">
        {filaId ? 'Editar' : 'Registrar'} {CATALOG[bloque.type].singular}
      </h2>
      <p>
        {AYUDA[bloque.type] ??
          'Los campos con * son obligatorios. Completá los datos que te sirven.'}
      </p>
      <form ref={formulario} data-tour="fillRecord" onSubmit={guardar} noValidate>
        <div className="record-form-grid">
          {bloque.fields.map((campo) => (
            <CampoDeFormulario
              key={campo.key}
              sistema={sistema}
              campo={campo}
              valor={inicial[campo.key]}
              prefijo="record"
              error={errores[campo.key]}
            />
          ))}
        </div>
        <div className="form-error" role="alert">
          {errores._form}
        </div>
        <div className="modal-footer spread">
          {filaId && !enBorrador ? (
            <Btn cls="danger small" onClick={() => pedirQuitarRegistro(bloque.id, filaId)}>
              <Icon name="trash" /> Eliminar
            </Btn>
          ) : (
            <span />
          )}
          <div className="row">
            <Btn cls="secondary" data-accion="close-modal" onClick={cerrarModal}>
              Cancelar
            </Btn>
            <button type="submit" className="btn primary">
              <Icon name="check" />
              Guardar
            </button>
          </div>
        </div>
      </form>
    </Modal>
  );
}
