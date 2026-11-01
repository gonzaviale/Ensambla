import {
  posicionDelPrecio,
  propioDe,
  type ProductoDeZona,
  type ProductoPropio,
} from '@/lib/sistema/zona';
import { Icon } from '../Icon';

// Cuántos de tus productos están más baratos, en línea o más caros que el promedio de la zona.
export function ResumenFrenteALaZona({
  productos,
  propios,
  zona,
}: {
  productos: ProductoDeZona[];
  propios: ProductoPropio[];
  zona: string;
}) {
  const cuenta = { bajo: 0, justo: 0, alto: 0 };
  for (const producto of productos) {
    const propio = propioDe(producto, propios);
    if (propio) cuenta[posicionDelPrecio(propio.precio, producto.precio.promedio)]++;
  }
  const comparados = cuenta.bajo + cuenta.justo + cuenta.alto;
  return (
    <>
      <div className="zona-ref">
        <span className="eyebrow">TU INVENTARIO FRENTE A {zona.toUpperCase() || 'LA ZONA'}</span>
        <h2>
          {comparados === 0
            ? 'Todavía no hay productos para comparar'
            : comparados === 1
              ? '1 producto comparado'
              : `${comparados} productos comparados`}
        </h2>
        <div className="zona-ref-nums">
          <div>
            <strong>{cuenta.bajo}</strong>
            <span>más baratos</span>
          </div>
          <div>
            <strong>{cuenta.justo}</strong>
            <span>en línea</span>
          </div>
          <div>
            <strong>{cuenta.alto}</strong>
            <span>más caros</span>
          </div>
        </div>
        <p>
          Se compara con el promedio de la zona: más de 8 % por debajo o 12 % por encima ya se marca
          como distinto.
        </p>
      </div>
      <div className="zona-privado">
        <Icon name="users" />
        <span>
          <strong>Datos agregados y anónimos.</strong> Nunca se muestra un comercio solo ni dónde
          queda.
        </span>
      </div>
    </>
  );
}
