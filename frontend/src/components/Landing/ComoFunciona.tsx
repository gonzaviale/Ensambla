import { variables } from './estilos';

const PASOS = [
  {
    clave: 'pieza',
    figura: 1,
    num: '01',
    titulo: 'Pieza',
    sub: 'Unidad modular',
    texto:
      'Cada dato, persona, paso o regla funciona como una pieza que se combina con otras. Arrancá con un bloque y sus campos.',
    retraso: '0s',
  },
  {
    clave: 'flujo',
    figura: 3,
    num: '02',
    titulo: 'Flujo',
    sub: 'Proceso conectado',
    texto:
      'Las piezas se relacionan con estados, responsables y permisos. Un pedido sabe qué cliente lo hizo y qué productos descuenta.',
    retraso: '.12s',
  },
  {
    clave: 'sistema',
    figura: 5,
    num: '03',
    titulo: 'Sistema',
    sub: 'Herramienta propia',
    texto:
      'El conjunto se convierte en la aplicación interna de tu negocio, con formularios y tablas listos para usar.',
    retraso: '.24s',
  },
] as const;

export function ComoFunciona() {
  return (
    <section className="seccion" id="como-funciona">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">Cómo funciona</p>
          <h2>
            El sistema crece
            <br />
            bloque a bloque.
          </h2>
          <p>
            Cada bloque representa una parte de tu negocio. Cuando se conecta con otros, deja de ser
            una tarea aislada y pasa a formar parte de un sistema propio.
          </p>
        </header>

        <ol className="pasos">
          {PASOS.map(({ clave, figura, num, titulo, sub, texto, retraso }) => (
            <li key={clave} className="paso revelar" style={variables({ '--d': retraso })}>
              <div className={`paso__figura paso__figura--${clave}`} aria-hidden="true">
                {Array.from({ length: figura }, (_, i) => (
                  <span key={i} />
                ))}
              </div>
              <span className="paso__num">{num}</span>
              <h3>{titulo}</h3>
              <p className="paso__sub">{sub}</p>
              <p>{texto}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
