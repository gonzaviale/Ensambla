import type { RefObject } from 'react';
import type { ProductoSugerido, ResultadoCatalogos } from '@/types/escaneo';

const NOMBRES = { sepa: 'SEPA · Argentina', openfoodfacts: 'Open Food Facts' };
const ESTADOS = {
  encontrado: 'Encontrado',
  no_encontrado: 'Sin coincidencia',
  no_disponible: 'No disponible',
};

export function FuentesDelProducto({
  resultado,
  sugerencia,
  dialogo,
  onCerrar,
}: {
  resultado: ResultadoCatalogos;
  sugerencia: ProductoSugerido;
  dialogo: RefObject<HTMLDialogElement | null>;
  onCerrar: () => void;
}) {
  return (
    <dialog ref={dialogo} className="escaneo-dialogo-fuentes" aria-label="Origen de los datos">
      <h2>Origen de los datos</h2>
      <p>Código {resultado.codigo}</p>
      <ul>
        {resultado.fuentes.map((fuente) => (
          <li key={fuente.fuente}>
            <strong>{NOMBRES[fuente.fuente]}</strong>
            <span>{ESTADOS[fuente.estado]}</span>
            {sugerencia.fuenteDescripcion === fuente.fuente && <small>Descripción sugerida</small>}
            {sugerencia.fuenteImagen === fuente.fuente && <small>Foto de referencia</small>}
          </li>
        ))}
      </ul>
      <p className="escaneo-ayuda">
        Elegimos la descripción más completa y agregamos marca y presentación cuando faltan.
        Corroborá el envase. El precio y las existencias son los de tu comercio.
      </p>
      <button type="button" className="btn primary" onClick={onCerrar}>
        Cerrar detalle
      </button>
    </dialog>
  );
}
