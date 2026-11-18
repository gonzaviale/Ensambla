import type { ProductoSugerido, ResultadoCatalogos } from '@/types/escaneo';
import { useState } from 'react';
import { Icon } from '@/components/sistema/Icon';

export function ProductoDetectado({
  resultado,
  sugerencia,
  nombre,
  existente,
  buscando,
  consultandoReferencia = false,
  onFuentes,
}: {
  resultado: ResultadoCatalogos | null;
  sugerencia: ProductoSugerido | null;
  nombre: string;
  existente: boolean;
  buscando: boolean;
  consultandoReferencia?: boolean;
  onFuentes: () => void;
}) {
  const [imagenFallida, setImagenFallida] = useState('');
  const imagen = sugerencia?.imagen;
  const hayImagen = imagen && imagen !== imagenFallida;
  const hayFuenteSinRespuesta = resultado?.fuentes.some(
    (fuente) => fuente.estado === 'no_disponible',
  );
  return (
    <section
      className="escaneo-producto-detectado"
      aria-label="Producto detectado"
      aria-busy={buscando}
    >
      <div
        className={`escaneo-foto-producto ${hayImagen ? '' : 'escaneo-foto-ausente'} ${consultandoReferencia ? 'escaneo-foto-consultando' : ''}`}
      >
        {hayImagen ? (
          <img
            src={imagen}
            alt="Foto de referencia del producto"
            referrerPolicy="no-referrer"
            onError={() => setImagenFallida(imagen)}
          />
        ) : (
          <>
            <Icon name={consultandoReferencia ? 'search' : 'image'} />
            <span>
              {consultandoReferencia ? 'Buscando foto de referencia…' : 'Sin foto de referencia'}
            </span>
          </>
        )}
      </div>
      <div className="escaneo-descripcion-producto">
        <span className="escaneo-estado-producto">
          <Icon name={buscando ? 'clock' : existente ? 'box' : nombre ? 'check-circle' : 'edit'} />
          {buscando
            ? 'Buscando producto'
            : existente
              ? 'En tu inventario'
              : sugerencia?.nombre
                ? 'Producto encontrado'
                : 'Carga manual'}
        </span>
        <h2>{nombre || (buscando ? 'Buscando la descripción…' : 'Completá el producto a mano')}</h2>
        <p>
          {buscando
            ? 'Estamos consultando la descripción y la foto para este código.'
            : existente
              ? 'Corroborá que sea el producto al que querés agregar unidades.'
              : sugerencia?.nombre
                ? 'Corroborá que la foto y la descripción coincidan con el envase.'
                : hayFuenteSinRespuesta
                  ? 'No pudimos obtener una descripción. Algún catálogo no respondió.'
                  : 'No encontramos una descripción para este código en los catálogos.'}
        </p>
        {resultado && (
          <button type="button" className="text-link" onClick={onFuentes}>
            <Icon name="help" /> Ver origen de los datos
          </button>
        )}
      </div>
    </section>
  );
}
