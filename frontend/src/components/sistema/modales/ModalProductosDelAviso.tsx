import { useState } from 'react';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { Modal } from '../Modal';
import { varsDeColor } from '../colores';
import { useSistemaStore } from '@/stores/sistemaStore';

const sinTildes = (texto: string) =>
  texto
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase();

// Los productos detrás del aviso económico. Pueden ser cientos, así que van en una lista que se desplaza y,
// pasados unos pocos, con un buscador.
export function ModalProductosDelAviso({ productos }: { productos: string[] }) {
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const [busqueda, setBusqueda] = useState('');
  const buscado = sinTildes(busqueda.trim());
  const visibles = buscado
    ? productos.filter((producto) => sinTildes(producto).includes(buscado))
    : productos;

  return (
    <Modal>
      <div className="icon-tile large" style={varsDeColor('petrol')}>
        <Icon name="chart" />
      </div>
      <h2 id="modal-title">Productos involucrados</h2>
      <p>
        {productos.length === 1
          ? 'Este es el producto que mueve el aviso.'
          : `Estos ${productos.length} productos son los que mueven el aviso.`}
      </p>
      {productos.length > 8 && (
        <input
          type="search"
          className="productos-aviso-buscador"
          aria-label="Buscar producto"
          placeholder="Buscar producto"
          value={busqueda}
          onChange={(evento) => setBusqueda(evento.target.value)}
        />
      )}
      {visibles.length === 0 ? (
        <p role="status">Ningún producto coincide con «{busqueda.trim()}».</p>
      ) : (
        <ul className="productos-aviso-lista" aria-label="Productos involucrados">
          {visibles.map((producto) => (
            <li key={producto}>{producto}</li>
          ))}
        </ul>
      )}
      <div className="modal-footer">
        <Btn cls="primary" onClick={cerrarModal}>
          Listo
        </Btn>
      </div>
    </Modal>
  );
}
