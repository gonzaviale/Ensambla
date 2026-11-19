import {
  contarPosiciones,
  fraseDeComparados,
  type PosicionDelPrecio,
  type ProductoDeZona,
  type ProductoPropio,
} from '@/lib/sistema/zona';
import { Icon } from '../Icon';

const POSICIONES: { posicion: PosicionDelPrecio; etiqueta: string }[] = [
  { posicion: 'bajo', etiqueta: 'más baratos' },
  { posicion: 'justo', etiqueta: 'en línea' },
  { posicion: 'alto', etiqueta: 'más caros' },
];

function NumerosPorPosicion({ cuenta }: { cuenta: Record<PosicionDelPrecio, number> }) {
  return (
    <div className="zona-ref-nums">
      {POSICIONES.map(({ posicion, etiqueta }) => (
        <div key={posicion} data-posicion={posicion}>
          <strong>{cuenta[posicion]}</strong>
          <span>{etiqueta}</span>
        </div>
      ))}
    </div>
  );
}

function DatosAnonimos() {
  return (
    <div className="zona-privado">
      <Icon name="users" />
      <span>
        <strong>Datos agregados y anónimos.</strong> Nunca se muestra un comercio solo ni dónde
        queda.
      </span>
    </div>
  );
}

type Props = {
  productos: ProductoDeZona[];
  propios: ProductoPropio[];
  zona: string;
};

// Cuántos de tus productos están más baratos, en línea o más caros que el promedio de la zona.
export function ResumenFrenteALaZona({ productos, propios, zona }: Props) {
  const cuenta = contarPosiciones(productos, propios);
  const comparados = cuenta.bajo + cuenta.justo + cuenta.alto;
  return (
    <>
      <div className="zona-ref">
        <span className="eyebrow">Tu stock frente a {zona || 'la zona'}</span>
        <h2>{fraseDeComparados(comparados)}</h2>
        <NumerosPorPosicion cuenta={cuenta} />
        <p>
          Se compara con el promedio de la zona: más de 8 % por debajo o 12 % por encima ya se marca
          como distinto.
        </p>
      </div>
      <DatosAnonimos />
    </>
  );
}
