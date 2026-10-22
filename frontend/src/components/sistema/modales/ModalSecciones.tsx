import { useState, type FormEvent } from 'react';
import { moverSeccion, quitarSeccion, renombrarSecciones } from '@/stores/accionesDelEditor';
import { seleccionarSistemaActivo, useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { IconBtn } from '../IconBtn';
import { Modal } from '../Modal';
import { varsDeColor } from '../colores';

// Renombrar, ordenar y quitar las secciones del sistema.
export function ModalSecciones() {
  const sistema = useSistemaStore(seleccionarSistemaActivo);
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
    renombrarSecciones(nombres());
    setEscritos({});
    accion();
  };

  const guardar = (evento: FormEvent) => {
    evento.preventDefault();
    if (nombres().some(([, nombre]) => !nombre)) return;
    renombrarSecciones(nombres());
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
        Las secciones son páginas de tu sistema. Podés renombrarlas, moverlas y repartir los bloques
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
                aria-label={`Nombre de la sección ${indice + 1}`}
                onChange={(evento) =>
                  setEscritos({ ...escritos, [seccion.id]: evento.target.value })
                }
              />
              <IconBtn
                name="up"
                label="Mover sección arriba"
                disabled={indice === 0}
                onClick={() => conNombres(() => moverSeccion(seccion.id, 'arriba'))}
              />
              <IconBtn
                name="down"
                label="Mover sección abajo"
                disabled={indice === sistema.sections.length - 1}
                onClick={() => conNombres(() => moverSeccion(seccion.id, 'abajo'))}
              />
              <IconBtn
                name="trash"
                label="Quitar sección y conservar bloques"
                disabled={sistema.sections.length === 1}
                onClick={() => conNombres(() => quitarSeccion(seccion.id))}
              />
            </div>
          ))}
        </div>
        <p className="inspector-hint">
          Si quitás una sección, sus bloques pasan a la primera sección disponible. Sus datos se
          conservan.
        </p>
        <div className="modal-footer spread">
          <Btn cls="secondary" onClick={() => abrirModal({ tipo: 'agregarSeccion' })}>
            <Icon name="plus" /> Nueva sección
          </Btn>
          <button className="btn primary" type="submit">
            Guardar nombres
          </button>
        </div>
      </form>
    </Modal>
  );
}
