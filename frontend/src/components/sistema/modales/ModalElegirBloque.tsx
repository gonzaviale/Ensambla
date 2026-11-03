import { CATALOG } from '@/lib/sistema/catalogo';
import type { TipoBloque } from '@/lib/sistema/tipos';
import { agregarBloque } from '@/stores/accionesDelEditor';
import { sumarBloqueAlBorrador } from '@/stores/accionesDelTaller';
import { IconTile } from '../IconTile';
import { Modal } from '../Modal';

// «¿Qué pieza necesitás?»: todo el catálogo de bloques en tarjetas. Con `enBorrador` suma a la propuesta de Sami.
export function ModalElegirBloque({ enBorrador = false }: { enBorrador?: boolean }) {
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
              onClick={() => (enBorrador ? sumarBloqueAlBorrador(tipo) : agregarBloque(tipo))}
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
