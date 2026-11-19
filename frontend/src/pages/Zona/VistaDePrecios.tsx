import { useState } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { ResumenFrenteALaZona } from '@/components/sistema/zona/ResumenFrenteALaZona';
import { TarjetaDePrecio } from '@/components/sistema/zona/TarjetaDePrecio';
import {
  productosBuscados,
  propioDe,
  sinSuficientesComercios,
  type ProductoDeZona,
  type ProductoPropio,
  type Zona,
} from '@/lib/sistema/zona';
import { SinDatosSuficientes } from './SinDatosSuficientes';

type Props = { zona: Zona; propios: ProductoPropio[]; titulo: string };

type PropsDeBusqueda = { consulta: string; onConsulta: (texto: string) => void };

function BuscadorDeProductos({ consulta, onConsulta }: PropsDeBusqueda) {
  return (
    <label className="searchbox zona-busca">
      <Icon name="search" />
      <input
        className="input"
        type="search"
        placeholder="Buscá un producto. Ej.: yerba"
        aria-label="Buscar un producto"
        value={consulta}
        onChange={(evento) => onConsulta(evento.target.value)}
      />
    </label>
  );
}

function SinResultados() {
  return (
    <div className="empty">
      <Icon name="search" />
      <p>No hay datos de ese producto en tu zona. Probá con otra palabra.</p>
    </div>
  );
}

function ListaDePrecios({
  productos,
  propios,
}: {
  productos: ProductoDeZona[];
  propios: ProductoPropio[];
}) {
  if (productos.length === 0) return <SinResultados />;
  return productos.map((producto) => (
    <TarjetaDePrecio
      key={producto.nombre}
      producto={producto}
      propio={propioDe(producto, propios)}
    />
  ));
}

export function VistaDePrecios({ zona, propios, titulo }: Props) {
  const [consulta, setConsulta] = useState('');
  if (sinSuficientesComercios(zona)) return <SinDatosSuficientes zona={zona} />;
  const productos = productosBuscados(zona, propios, consulta);
  return (
    <div className="zona-grid">
      <section className="zona-panel">
        <BuscadorDeProductos consulta={consulta} onConsulta={setConsulta} />
        <div className="row between">
          <h2>{titulo}</h2>
          <span className="tag outline">{productos.length}</span>
        </div>
        <div className="zona-lista">
          <ListaDePrecios productos={productos} propios={propios} />
        </div>
      </section>
      <aside className="zona-lateral">
        <ResumenFrenteALaZona productos={zona.productos} propios={propios} zona={zona.nombre} />
      </aside>
    </div>
  );
}
