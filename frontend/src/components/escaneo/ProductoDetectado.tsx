import type { ProductoSugerido, ResultadoCatalogos } from '@/types/escaneo';
import { Icon } from '@/components/sistema/Icon';

export function ProductoDetectado({
  resultado,
  sugerencia,
  onFuentes,
}: {
  resultado: ResultadoCatalogos;
  sugerencia: ProductoSugerido;
  onFuentes: () => void;
}) {
  const hayFuenteSinRespuesta = resultado.fuentes.some(
    (fuente) => fuente.estado === 'no_disponible',
  );
  return (
    <section className="escaneo-producto-detectado" aria-label="Producto detectado">
      {sugerencia.imagen && (
        <img
          src={sugerencia.imagen}
          alt="Foto de referencia del producto"
          referrerPolicy="no-referrer"
          loading="lazy"
        />
      )}
      <div>
        <strong>{sugerencia.nombre ? 'Producto reconocido' : 'Completá el producto a mano'}</strong>
        <p>
          {sugerencia.nombre
            ? 'Revisá el nombre y la presentación en el formulario.'
            : hayFuenteSinRespuesta
              ? 'No pudimos obtener una descripción. Algún catálogo no respondió.'
              : 'No encontramos una descripción para este código en los catálogos.'}
        </p>
        <button type="button" className="text-link" onClick={onFuentes}>
          <Icon name="help" /> Ver origen de los datos
        </button>
      </div>
    </section>
  );
}
