import { Link } from '@tanstack/react-router';

const PASOS = [
  {
    titulo: 'Sacale una foto',
    texto: 'Desde el celular, a la hoja de tu cuaderno, tu planilla o tu libreta de pedidos.',
  },
  {
    titulo: 'Revisá lo que leyó',
    texto:
      'La IA arma la tabla con sus columnas y marca los datos que no pudo leer con seguridad.',
  },
  {
    titulo: 'Guardalo y listo',
    texto: 'Queda un sistema propio, con tus filas cargadas, para seguir usándolo sin papel.',
  },
] as const;

const ANOTACIONES = ['Harina 000 · 12 kg', 'Azúcar · 8 kg', 'Torta de ricota · 5'] as const;

const FILAS = [
  { producto: 'Harina 000', cantidad: '12', duda: false },
  { producto: 'Azúcar', cantidad: '8', duda: false },
  { producto: 'Torta de ricota', cantidad: '5', duda: true },
] as const;

export function DeLaFoto() {
  return (
    <section className="seccion" id="camara">
      <div className="contenedor foto">
        <div className="foto__texto revelar">
          <p className="sobretitulo">Con tu cámara</p>
          <h2>De la foto del cuaderno a tu sistema.</h2>
          <p className="foto__bajada">
            Si hoy anotás todo en papel, no hace falta empezar de cero: sacale una foto y Ensambla
            lo pasa a tablas listas para usar.
          </p>
          <ol className="foto__pasos">
            {PASOS.map(({ titulo, texto }) => (
              <li key={titulo}>
                <h3>{titulo}</h3>
                <p>{texto}</p>
              </li>
            ))}
          </ol>
          <Link to="/registro" className="boton boton--primario">
            Probalo con tu cuaderno
          </Link>
        </div>

        <div className="foto__visual revelar" aria-hidden="true">
          <div className="foto__hoja">
            <span className="foto__esquina foto__esquina--1" />
            <span className="foto__esquina foto__esquina--2" />
            <span className="foto__esquina foto__esquina--3" />
            <span className="foto__esquina foto__esquina--4" />
            {ANOTACIONES.map((linea) => (
              <p key={linea}>{linea}</p>
            ))}
          </div>
          <span className="foto__flecha">→</span>
          <div className="foto__tabla">
            <div className="foto__fila foto__fila--cabeza">
              <span>Producto</span>
              <span>Cantidad</span>
            </div>
            {FILAS.map(({ producto, cantidad, duda }) => (
              <div key={producto} className="foto__fila">
                <span>{producto}</span>
                <span className={duda ? 'foto__celda foto__celda--duda' : 'foto__celda'}>
                  {cantidad}
                </span>
              </div>
            ))}
            <p className="foto__aviso">Revisá este dato: la IA no estuvo segura.</p>
          </div>
        </div>
      </div>
    </section>
  );
}
