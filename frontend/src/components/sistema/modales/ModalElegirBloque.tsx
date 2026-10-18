import { CATALOG } from '@/lib/sistema/catalogo';
import type { TipoBloque } from '@/lib/sistema/tipos';
import { agregarBloque } from '@/stores/accionesDelEditor';
import { IconTile } from '../IconTile';
import { Modal } from '../Modal';

// «¿Qué pieza necesitás?»: todo el catálogo de bloques en tarjetas.
export function ModalElegirBloque() {
  return (
    <Modal>
      <h2 id="modal-title">¿Qué pieza necesitás?</h2>
      <p>Los campos ya están preparados. Después podés cambiarlos.</p>
      <div className="needs-grid">
        {(Object.entries(CATALOG) as [TipoBloque, (typeof CATALOG)[TipoBloque]][]).map(
          ([tipo, definicion]) => (
            <button
              key={tipo}
              type="button"
              className="need-card"
              onClick={() => agregarBloque(tipo)}
            >
              <IconTile tipo={tipo} />
              <div>
                <h3>{definicion.name}</h3>
                <p>{definicion.description}</p>
              </div>
            </button>
          ),
        )}
      </div>
    </Modal>
  );
}
