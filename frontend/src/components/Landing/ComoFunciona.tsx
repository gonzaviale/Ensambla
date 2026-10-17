import { variables } from './estilos';

const PASOS = [
  {
    clave: 'pieza',
    figura: 1,
    num: '01',
    titulo: 'Cargá como te salga',
    sub: 'Foto, voz o texto',
    texto:
      'Contale a Sami qué vendés y cómo trabajás, y él arma tu sistema. Después cargás ventas, productos y precios con una foto del cuaderno, un mensaje o una nota de voz.',
    retraso: '0s',
  },
  {
    clave: 'flujo',
    figura: 3,
    num: '02',
    titulo: 'Ensambla lo ordena',
    sub: 'Piezas conectadas',
    texto:
      'La IA interpreta lo que cargaste y lo guarda ordenado después de que lo confirmás. Cada pieza se conecta con las demás: una venta sabe qué cliente la hizo y qué stock descuenta.',
    retraso: '.12s',
  },
  {
    clave: 'sistema',
    figura: 5,
    num: '03',
    titulo: 'Recibí avisos para decidir',
    sub: 'Un sistema que trabaja por vos',
    texto:
      'El motor de monitoreo cruza tus ventas, precios y stock con los datos de tu zona y te avisa qué hacer. Cada aviso trae una acción sugerida que podés aplicar o descartar.',
    retraso: '.24s',
  },
] as const;

export function ComoFunciona() {
  return (
    <section className="seccion seccion--tono" id="como-funciona">
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
            una tarea aislada y pasa a formar parte de un sistema propio, con tres pasos simples.
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
