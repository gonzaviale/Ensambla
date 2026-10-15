import { Link } from '@tanstack/react-router';
import { variables } from './estilos';

const PLANTILLAS = [
  {
    nombre: 'Inventario',
    color: 'naranja',
    texto: 'Productos, stock, precios y alertas cuando algo se está por terminar.',
    campos: ['Texto', 'Número', 'Precio', 'Categoría'],
    retraso: '0s',
  },
  {
    nombre: 'Pedidos',
    color: 'coral',
    texto: 'Seguí cada pedido desde que entra hasta que se entrega, con estados claros.',
    campos: ['Fecha', 'Estado', 'Cliente', 'Total'],
    retraso: '.1s',
  },
  {
    nombre: 'Clientes',
    color: 'pizarra',
    texto: 'Datos de contacto, historial de compras y notas de cada cliente en un solo lugar.',
    campos: ['Nombre', 'Email', 'Teléfono', 'Notas'],
    retraso: '.2s',
  },
] as const;

export function Plantillas() {
  return (
    <section className="seccion" id="plantillas">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">Plantillas</p>
          <h2>No arranques de cero.</h2>
          <p>
            Elegí una plantilla pensada para un comercio de barrio y adaptala a tu forma de
            trabajar. O empezá en blanco, como prefieras.
          </p>
        </header>

        <div className="plantillas">
          {PLANTILLAS.map(({ nombre, color, texto, campos, retraso }) => (
            <article
              key={nombre}
              className="plantilla revelar"
              style={variables({ '--d': retraso })}
            >
              <div className={`plantilla__mini plantilla__mini--${color}`} aria-hidden="true">
                <span />
                <span />
                <span />
                <span />
              </div>
              <h3>{nombre}</h3>
              <p>{texto}</p>
              <ul className="campos">
                {campos.map((campo) => (
                  <li key={campo}>{campo}</li>
                ))}
              </ul>
              <Link to="/registro" className="enlace">
                Usar plantilla <span aria-hidden="true">→</span>
              </Link>
            </article>
          ))}

          <article
            className="plantilla plantilla--blanco revelar"
            style={variables({ '--d': '.3s' })}
          >
            <div className="plantilla__mini plantilla__mini--vacia" aria-hidden="true">
              <span>+</span>
            </div>
            <h3>En blanco</h3>
            <p>Tu negocio es único. Armá tu sistema desde la primera pieza.</p>
            <Link to="/registro" className="enlace">
              Empezar en blanco <span aria-hidden="true">→</span>
            </Link>
          </article>
        </div>
      </div>
    </section>
  );
}
