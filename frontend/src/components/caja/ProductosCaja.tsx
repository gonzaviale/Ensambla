import { Icon } from '@/components/sistema/Icon';
import { dineroCaja, modoVenta } from '@/lib/caja';
import { normalize } from '@/lib/sistema/core';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type { Fila } from '@/lib/sistema/tipos';
import type { ModoVentaCaja } from '@/types/caja';

type PropsDeProductos = {
  productos: Fila[];
  consulta: string;
  favoritos: boolean;
  bloqueada: boolean;
  onConsulta: (valor: string) => void;
  onElegir: (producto: Fila) => void;
  onFavorito: (producto: Fila) => void;
  onEditar: (producto: Fila) => void;
  onGestionar: () => void;
};

type AccionesDeTarjeta = Pick<PropsDeProductos, 'bloqueada' | 'onElegir' | 'onFavorito' | 'onEditar'>;

const ICONO_POR_MODO: Record<ModoVentaCaja, NombreDeIcono> = { unidad: 'box', peso: 'grid' };
const UNIDAD_POR_MODO: Record<ModoVentaCaja, string> = { unidad: 'unidad', peso: 'kg' };

export function ProductosCaja(props: PropsDeProductos) {
  const mostrados = productosMostrados(props);
  return (
    <section
      className="caja-productos"
      aria-label={props.favoritos ? 'Productos favoritos' : 'Buscar productos'}
    >
      <CabeceraDeProductos {...props} />
      <BuscadorDeProductos {...props} />
      <ListaDeProductos productos={mostrados} favoritos={props.favoritos} acciones={props} />
      {!mostrados.length && <SinProductos consulta={props.consulta} />}
    </section>
  );
}

function productosMostrados({ productos, favoritos, consulta }: PropsDeProductos) {
  const buscada = normalize(consulta);
  return productos.filter(
    (producto) => (!favoritos || esFavorito(producto)) && textoBuscable(producto).includes(buscada),
  );
}

const esFavorito = (producto: Fila) => producto.favorite === true;

const textoBuscable = (producto: Fila) => normalize(`${producto.name} ${producto.barcode ?? ''}`);

function CabeceraDeProductos({ favoritos, bloqueada, onGestionar }: PropsDeProductos) {
  return (
    <div className="caja-entre caja-productos-cabecera">
      <div>
        <h3>{favoritos ? 'Tus favoritos' : 'Buscar en tu stock'}</h3>
        <p className="caja-ayuda">
          {favoritos
            ? 'Los artículos de todos los días, a un toque.'
            : 'Elegí un artículo para agregarlo a la compra.'}
        </p>
      </div>
      {favoritos && (
        <button type="button" className="btn secondary small" disabled={bloqueada} onClick={onGestionar}>
          <Icon name="edit" /> Organizar
        </button>
      )}
    </div>
  );
}

function BuscadorDeProductos({ favoritos, consulta, bloqueada, onConsulta }: PropsDeProductos) {
  return (
    <label className="caja-buscador">
      <Icon name="search" />
      <input
        aria-label="Buscar producto en tu stock"
        placeholder={favoritos ? 'Buscar entre tus favoritos' : 'Nombre o código del artículo'}
        value={consulta}
        onChange={(evento) => onConsulta(evento.target.value)}
        disabled={bloqueada}
      />
    </label>
  );
}

type PropsDeLista = { productos: Fila[]; favoritos: boolean; acciones: AccionesDeTarjeta };

function ListaDeProductos({ productos, favoritos, acciones }: PropsDeLista) {
  return (
    <div className={`caja-favoritos ${favoritos ? '' : 'caja-resultados'}`}>
      {productos.map((producto) => (
        <TarjetaDeProducto key={producto.id} producto={producto} acciones={acciones} />
      ))}
    </div>
  );
}

function TarjetaDeProducto({ producto, acciones }: { producto: Fila; acciones: AccionesDeTarjeta }) {
  return (
    <div className="caja-tarjeta-producto">
      <BotonDeElegirProducto producto={producto} acciones={acciones} />
      <div className="caja-tarjeta-acciones">
        <BotonDeFavorito producto={producto} acciones={acciones} />
        <BotonDeEditarArticulo producto={producto} acciones={acciones} />
      </div>
    </div>
  );
}

type PropsDeBoton = { producto: Fila; acciones: AccionesDeTarjeta };

function BotonDeElegirProducto({ producto, acciones }: PropsDeBoton) {
  const modo = modoVenta(producto);
  return (
    <button
      type="button"
      className="caja-elegir-producto"
      onClick={() => acciones.onElegir(producto)}
      disabled={acciones.bloqueada}
    >
      <span className={`caja-simbolo ${modo === 'peso' ? 'peso' : ''}`}>
        <Icon name={ICONO_POR_MODO[modo]} />
      </span>
      <strong>{String(producto.name)}</strong>
      <span>
        {precioDe(producto)} / {UNIDAD_POR_MODO[modo]}
      </span>
    </button>
  );
}

function precioDe(producto: Fila) {
  const precio = Number(producto.price);
  return precio > 0 ? dineroCaja(Math.round(precio * 100)) : 'Completar precio';
}

function BotonDeFavorito({ producto, acciones }: PropsDeBoton) {
  const accion = accionDeFavorito(producto);
  return (
    <button
      type="button"
      className="icon-btn caja-favorito"
      disabled={acciones.bloqueada}
      aria-pressed={esFavorito(producto)}
      aria-label={`${accion} favoritos: ${String(producto.name)}`}
      title={`${accion} favoritos`}
      onClick={() => acciones.onFavorito(producto)}
    >
      <Icon name={iconoDeFavorito(producto)} />
    </button>
  );
}

const accionDeFavorito = (producto: Fila) => (esFavorito(producto) ? 'Quitar de' : 'Agregar a');

const iconoDeFavorito = (producto: Fila): NombreDeIcono => (esFavorito(producto) ? 'star-fill' : 'star');

function BotonDeEditarArticulo({ producto, acciones }: PropsDeBoton) {
  return (
    <button
      type="button"
      className="icon-btn"
      disabled={acciones.bloqueada}
      aria-label={`Editar artículo ${String(producto.name)}`}
      onClick={() => acciones.onEditar(producto)}
    >
      <Icon name="edit" />
    </button>
  );
}

function SinProductos({ consulta }: { consulta: string }) {
  if (consulta) return <AvisoSinProductos icono="search" titulo="Sin resultados" detalle="Probá otro nombre o agregá un artículo." />;
  return (
    <AvisoSinProductos
      icono="box"
      titulo="Tus favoritos aparecerán aquí"
      detalle="Tocá Organizar para elegirlos o crear uno nuevo."
    />
  );
}

function AvisoSinProductos({ icono, titulo, detalle }: { icono: NombreDeIcono; titulo: string; detalle: string }) {
  return (
    <div className="caja-vacio">
      <Icon name={icono} />
      <h3>{titulo}</h3>
      <p>{detalle}</p>
    </div>
  );
}
