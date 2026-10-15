import { Isotipo } from '@/components/ui';
import { variables } from './estilos';

const GARANTIAS = [
  {
    clave: 'privacidad',
    titulo: 'Tus datos son tuyos',
    texto: 'Lo que cargás nunca se le muestra a otro comercio.',
  },
  {
    clave: 'anonimo',
    titulo: 'Comparaciones anónimas',
    texto: 'Sólo se comparan datos agrupados, y recién cuando hay 5 comercios o más en la zona.',
  },
  {
    clave: 'neutral',
    titulo: 'Neutralidad',
    texto:
      'Un mayorista paga por llegar a más comercios, nunca por esconder información: si otro vende más barato, lo vas a ver.',
  },
] as const;

export function RedQueTrabaja() {
  return (
    <section className="seccion seccion--oscura" id="red" aria-labelledby="red-titulo">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">Una red que trabaja para vos</p>
          <h2 id="red-titulo">Cada comercio que se suma mejora la red.</h2>
          <p>
            Ensambla cruza precios, stock y ofertas entre comercios y proveedores, y te avisa solo.
            Es la única herramienta que te dice cuánto cobrar y cuánto pagar.
          </p>
        </header>

        <div className="red revelar">
          <div className="red__nodo">
            <p className="red__rol">Proveedores y mayoristas</p>
            <p className="red__texto">Suben su lista y sus ofertas una sola vez.</p>
          </div>
          <span className="red__flecha" aria-hidden="true">
            →
          </span>
          <div className="red__nodo red__nodo--centro">
            <Isotipo className="red__iso" />
            <p className="red__rol">Ensambla</p>
            <p className="red__texto">
              Empareja productos, cruza costos, stock y ofertas, y decide qué avisar.
            </p>
          </div>
          <span className="red__flecha" aria-hidden="true">
            →
          </span>
          <div className="red__nodo">
            <p className="red__rol">Cada comercio</p>
            <p className="red__texto">
              Recibe sus precios, con sus reglas, sus avisos y las ofertas que le sirven.
            </p>
          </div>
        </div>

        <p className="red__bucle revelar">
          Más comercios, mejores comparaciones. Mejores comparaciones, más proveedores que suben su
          lista. Y más listas, más precios al día para todos.
        </p>

        <ul className="garantias">
          {GARANTIAS.map(({ clave, titulo, texto }, i) => (
            <li key={clave} className="garantia revelar" style={variables({ '--d': `${i / 10}s` })}>
              <span className="garantia__tilde" aria-hidden="true">
                ✓
              </span>
              <h3>{titulo}</h3>
              <p>{texto}</p>
            </li>
          ))}
        </ul>

        <p className="red__oficios revelar">
          El mismo motor sirve para los oficios: un instalador de aires, en vez de productos, vende
          trabajos, y Ensambla le avisa cuando un presupuesto quedó desactualizado. Es lo que viene
          después; hoy empezamos por los comercios.
        </p>
      </div>
    </section>
  );
}
