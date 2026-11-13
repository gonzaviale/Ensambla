import { useState, type FormEvent } from 'react';
import { moverSeccion, quitarSeccion, renombrarSecciones } from '@/stores/accionesDelEditor';
import {
  moverSeccionDelBorrador,
  quitarSeccionDelBorrador,
  renombrarSeccionesDelBorrador,
} from '@/stores/accionesDelTaller';
import { seleccionarSistemaActivo, useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { IconBtn } from '../IconBtn';
import { Modal } from '../Modal';
import { varsDeColor } from '../colores';

// Renombrar, ordenar y quitar las pestañas del sistema (o de la propuesta de Sami, con `enBorrador`).
export function ModalSecciones({ enBorrador = false }: { enBorrador?: boolean }) {
  const sistema = useSistemaStore((estado) =>
    enBorrador ? estado.taller?.borrador : seleccionarSistemaActivo(estado),
  );
  const acciones = enBorrador
    ? {
        renombrar: renombrarSeccionesDelBorrador,
        mover: moverSeccionDelBorrador,
        quitar: quitarSeccionDelBorrador,
      }
    : { renombrar: renombrarSecciones, mover: moverSeccion, quitar: quitarSeccion };
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const avisar = useSistemaStore((estado) => estado.avisar);
  // Lo que se va escribiendo en cada nombre, aunque todavía no se haya guardado.
  const [escritos, setEscritos] = useState<Record<string, string>>({});
  if (!sistema) return null;

  const nombreDe = (id: string, original: string) => escritos[id] ?? original;
  const nombres = (): [string, string][] =>
    sistema.sections.map((seccion) => [seccion.id, nombreDe(seccion.id, seccion.name).trim()]);

  // Mover o quitar guarda antes los nombres que ya se escribieron, como hace el mockup.
  const conNombres = (accion: () => void) => {
    acciones.renombrar(nombres());
    setEscritos({});
    accion();
  };

  const guardar = (evento: FormEvent) => {
    evento.preventDefault();
    if (nombres().some(([, nombre]) => !nombre)) return;
    acciones.renombrar(nombres());
    cerrarModal();
    avisar('Nombres actualizados.');
  };

  return (
    <Modal>
      <div className="icon-tile" style={varsDeColor('petrol')}>
        <Icon name="folder" />
      </div>
      <h2 id="modal-title">Un lugar para cada cosa.</h2>
      <p>
        Las pestañas son páginas de tu sección. Podés renombrarlas, moverlas y repartir los bloques
        entre ellas.
      </p>
      <form onSubmit={guardar}>
        <div className="section-manage">
          {sistema.sections.map((seccion, indice) => (
            <div className="section-row" key={seccion.id}>
              <input
                className="input"
                name={`section-${seccion.id}`}
                data-section-name={seccion.id}
                value={nombreDe(seccion.id, seccion.name)}
                maxLength={50}
                required
                aria-label={`Nombre de la pestaña ${indice + 1}`}
                onChange={(evento) =>
                  setEscritos({ ...escritos, [seccion.id]: evento.target.value })
                }
              />
              <IconBtn
                name="up"
                label="Mover pestaña arriba"
                disabled={indice === 0}
                onClick={() => conNombres(() => acciones.mover(seccion.id, 'arriba'))}
              />
              <IconBtn
                name="down"
                label="Mover pestaña abajo"
                disabled={indice === sistema.sections.length - 1}
                onClick={() => conNombres(() => acciones.mover(seccion.id, 'abajo'))}
              />
              <IconBtn
                name="trash"
                label="Quitar pestaña y conservar bloques"
                disabled={sistema.sections.length === 1}
                onClick={() => conNombres(() => acciones.quitar(seccion.id))}
              />
            </div>
          ))}
        </div>
        <p className="inspector-hint">
          Si quitás una pestaña, sus bloques pasan a la primera pestaña disponible. Sus datos se
          conservan.
        </p>
        <div className="modal-footer spread">
          <Btn cls="secondary" onClick={() => abrirModal({ tipo: 'agregarSeccion', enBorrador })}>
            <Icon name="plus" /> Nueva pestaña
          </Btn>
          <button className="btn primary" type="submit">
            Guardar nombres
          </button>
        </div>
      </form>
    </Modal>
  );
}
