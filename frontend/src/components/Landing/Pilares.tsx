import { variables } from './estilos';

const PILARES = [
  {
    num: '01',
    tag: 'Sencillez',
    titulo: 'Una línea, un toque',
    texto:
      'Sin tablas ni menús: ves tus productos, tus proveedores y tus avisos con las palabras de siempre. Todo se confirma de un toque.',
    retraso: '0s',
  },
  {
    num: '02',
    tag: 'Red',
    titulo: 'Cada lista sirve a todos',
    texto:
      'Cuando un producto queda emparejado, sirve para toda la red: comparás costos y ofertas sin cargar nada dos veces.',
    retraso: '.1s',
  },
  {
    num: '03',
    tag: 'Crecimiento',
    titulo: 'Crece con tu negocio',
    texto:
      'Empezá por los precios. Después sumá stock, pedidos, clientes o proveedores, un bloque a la vez.',
    retraso: '.2s',
  },
] as const;

export function Pilares() {
  return (
    <section className="seccion seccion--oscura" id="por-que">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">Por qué Ensambla</p>
          <h2>
            Simple hoy.
            <br />
            Listo para crecer mañana.
          </h2>
        </header>

        <div className="pilares">
          {PILARES.map(({ num, tag, titulo, texto, retraso }) => (
            <article key={num} className="pilar revelar" style={variables({ '--d': retraso })}>
              <span className="pilar__num">{num}</span>
              <span className="pilar__tag">{tag}</span>
              <h3>{titulo}</h3>
              <p>{texto}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
