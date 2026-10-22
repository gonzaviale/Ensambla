import { pesos } from '@/lib/sistema/alertas';
import {
  posicionDelPrecio,
  type PosicionDelPrecio,
  type ProductoDeZona,
  type ProductoPropio,
} from '@/lib/sistema/zona';
import { varsDeColor } from '../colores';
import { RangoDePrecios } from './RangoDePrecios';

const TEXTO_DE_POSICION: Record<PosicionDelPrecio, string> = {
  bajo: 'por debajo del promedio de tu zona',
  justo: 'en línea con tu zona',
  alto: 'por encima del promedio de tu zona',
};

const COLOR_DE_POSICION = { bajo: 'yellow', justo: 'blue', alto: 'coral' } as const;

// Un producto de la zona: su rango de precios agregado y, si lo tenés, dónde está el tuyo.
export function TarjetaDePrecio({
  producto,
  propio,
}: {
  producto: ProductoDeZona;
  propio?: ProductoPropio;
}) {
  const posicion = propio ? posicionDelPrecio(propio.precio, producto.precio.promedio) : undefined;
  return (
    <article
      className={`zona-tarjeta ${propio ? 'mio' : ''}`.trim()}
      style={varsDeColor(posicion ? COLOR_DE_POSICION[posicion] : 'blue')}
    >
      <div className="zona-tarjeta-cabeza">
        <div>
          <strong>{producto.nombre}</strong>
          <div className="zona-meta">{producto.comercios} comercios aportan este precio</div>
        </div>
        <div className="zona-precio">
          {pesos(producto.precio.promedio)}
          <small>promedio</small>
        </div>
      </div>
      <RangoDePrecios
        minimo={producto.precio.minimo}
        promedio={producto.precio.promedio}
        maximo={producto.precio.maximo}
        mio={propio?.precio}
      />
      {propio && posicion && (
        <span className={`zona-etiqueta ${posicion}`}>
          Tu precio: {pesos(propio.precio)} · {TEXTO_DE_POSICION[posicion]}
        </span>
      )}
    </article>
  );
}
