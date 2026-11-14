import { Icon } from '@/components/sistema/Icon';
import { dineroCaja, modoVenta } from '@/lib/caja';
import { normalize } from '@/lib/sistema/core';
import type { Fila } from '@/lib/sistema/tipos';

export function ProductosCaja({
  productos,
  consulta,
  favoritos,
  bloqueada,
  onConsulta,
  onElegir,
  onFavorito,
  onEditar,
  onGestionar,
}: {
  productos: Fila[];
  consulta: string;
  favoritos: boolean;
  bloqueada: boolean;
  onConsulta: (valor: string) => void;
  onElegir: (producto: Fila) => void;
  onFavorito: (producto: Fila) => void;
  onEditar: (producto: Fila) => void;
  onGestionar: () => void;
}) {
  const mostrados = productos.filter(
    (p) =>
      (!favoritos || p.favorite === true) &&
      normalize(`${p.name} ${p.barcode ?? ''}`).includes(normalize(consulta)),
  );
  return (
    <section
      className="caja-productos"
      aria-label={favoritos ? 'Productos favoritos' : 'Buscar productos'}
    >
      <div className="caja-entre caja-productos-cabecera">
        <div>
          <h3>{favoritos ? 'Tus favoritos' : 'Buscar en el inventario'}</h3>
          <p className="caja-ayuda">
            {favoritos
              ? 'Los artículos de todos los días, a un toque.'
              : 'Elegí un artículo para agregarlo a la compra.'}
          </p>
        </div>
        {favoritos && (
          <button
            type="button"
            className="btn secondary small"
            disabled={bloqueada}
            onClick={onGestionar}
          >
            <Icon name="edit" /> Organizar
          </button>
        )}
      </div>
      <label className="caja-buscador">
        <Icon name="search" />
        <input
          aria-label="Buscar artículo en el inventario"
          placeholder={favoritos ? 'Buscar entre tus favoritos' : 'Nombre o código del artículo'}
          value={consulta}
          onChange={(e) => onConsulta(e.target.value)}
          disabled={bloqueada}
        />
      </label>
      <div className={`caja-favoritos ${favoritos ? '' : 'caja-resultados'}`}>
        {mostrados.map((producto) => (
          <div key={producto.id} className="caja-tarjeta-producto">
            <button
              type="button"
              className="caja-elegir-producto"
              onClick={() => onElegir(producto)}
              disabled={bloqueada}
            >
              <span className={`caja-simbolo ${modoVenta(producto) === 'peso' ? 'peso' : ''}`}>
                <Icon name={modoVenta(producto) === 'peso' ? 'grid' : 'box'} />
              </span>
              <strong>{String(producto.name)}</strong>
              <span>
                {Number(producto.price) > 0
                  ? dineroCaja(Math.round(Number(producto.price) * 100))
                  : 'Completar precio'}{' '}
                / {modoVenta(producto) === 'peso' ? 'kg' : 'unidad'}
              </span>
            </button>
            <div className="caja-tarjeta-acciones">
              <button
                type="button"
                className="icon-btn"
                disabled={bloqueada}
                aria-label={`${producto.favorite === true ? 'Quitar de' : 'Agregar a'} favoritos: ${String(producto.name)}`}
                onClick={() => onFavorito(producto)}
              >
                <Icon name={producto.favorite === true ? 'check' : 'plus'} />
              </button>
              <button
                type="button"
                className="icon-btn"
                disabled={bloqueada}
                aria-label={`Editar artículo ${String(producto.name)}`}
                onClick={() => onEditar(producto)}
              >
                <Icon name="edit" />
              </button>
            </div>
          </div>
        ))}
      </div>
      {!mostrados.length && (
        <div className="caja-vacio">
          <Icon name={consulta ? 'search' : 'box'} />
          <h3>{consulta ? 'Sin resultados' : 'Tus favoritos aparecerán aquí'}</h3>
          <p>
            {consulta
              ? 'Probá otro nombre o agregá un artículo.'
              : 'Tocá Organizar para elegirlos o crear uno nuevo.'}
          </p>
        </div>
      )}
    </section>
  );
}
