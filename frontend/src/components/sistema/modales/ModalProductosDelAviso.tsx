import { useState } from 'react';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { Modal } from '../Modal';
import { varsDeColor } from '../colores';
import { formatoPesos } from '@/lib/sistema/formato';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { AvisoEconomico } from '@/types/avisoEconomico';

const sinTildes = (texto: string) => texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

// Qué pasó con cada producto, en palabras de todos los días: «Te dejó $4.200 menos de ganancia» o «Vendiste $800 más».
// Sin costos cargados se cuentan las ventas.
const queCambio = (monto: number, categoria: string) => {
  const plata = formatoPesos(Math.abs(monto));
  const comparacion = monto < 0 ? 'menos' : 'más';
  return categoria === 'Ventas'
    ? `Vendiste ${plata} ${comparacion} que la semana pasada`
    : `Te dejó ${plata} ${comparacion} de ganancia que la semana pasada`;
};

// Los productos detrás del aviso económico y qué cambió en cada uno. Pasados unos pocos, con un buscador.
export function ModalProductosDelAviso({
  cambios,
  categoria,
  titulo,
}: {
  cambios: AvisoEconomico['cambios'];
  categoria: string;
  titulo: string;
}) {
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const [busqueda, setBusqueda] = useState('');
  const buscado = sinTildes(busqueda.trim());
  const visibles = buscado
    ? cambios.filter((cambio) => sinTildes(cambio.nombre).includes(buscado))
    : cambios;

  return (
    <Modal>
      <div className="icon-tile large" style={varsDeColor('petrol')}>
        <Icon name="chart" />
      </div>
      <h2 id="modal-title">{titulo}</h2>
      <p>Así les fue esta semana, uno por uno.</p>
      {cambios.length > 8 && (
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
          {visibles.map((cambio) => (
            <li key={cambio.nombre}>
              <span className="productos-aviso-nombre">{cambio.nombre}</span>
              <span className="productos-aviso-cambio" data-baja={cambio.monto < 0}>
                <Icon name={cambio.monto < 0 ? 'baja' : 'sube'} />
                {queCambio(cambio.monto, categoria)}
              </span>
            </li>
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
