import { NOMBRE_RUBRO } from '@/lib/catalogo/piezas';
import { variables } from './estilos';

const RUBROS = [
  {
    nombre: NOMBRE_RUBRO.comercio,
    color: 'naranja',
    texto: 'Vendés productos y querés saber cuánto te queda y quién te debe.',
    ejemplos: 'Almacén, kiosco, despensa, verdulería',
  },
  {
    nombre: NOMBRE_RUBRO.gastronomia,
    color: 'coral',
    texto: 'Cocinás por encargo, cobrás una seña y querés saber cuánto te cuesta cada plato.',
    ejemplos: 'Viandas, tortas, catering, panadería',
  },
  {
    nombre: 'Tu negocio, sea cual sea',
    color: 'celeste',
    texto: 'Contanos cómo trabajás y Ensambla arma las piezas que te sirven. El catálogo crece.',
    ejemplos: 'Servicios, oficios, venta online',
  },
] as const;

export function ParaQuien() {
  return (
    <section className="seccion" id="para-quien" aria-labelledby="para-quien-titulo">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">Para quién es</p>
          <h2 id="para-quien-titulo">No sólo el almacén. Tu negocio también.</h2>
          <p>Si hoy anotás a mano, en un cuaderno o en WhatsApp, Ensambla es para vos.</p>
        </header>

        <ul className="rubros">
          {RUBROS.map(({ nombre, color, texto, ejemplos }, i) => (
            <li
              key={nombre}
              className="rubro revelar"
              data-color={color}
              style={variables({ '--d': `${i / 10}s` })}
            >
              <h3>{nombre}</h3>
              <p>{texto}</p>
              <p className="rubro__ejemplos">{ejemplos}</p>
            </li>
          ))}
        </ul>

        <p className="para-quien__frase revelar">
          Sirve para cualquier negocio que hoy anota a mano o en WhatsApp.
        </p>
      </div>
    </section>
  );
}
