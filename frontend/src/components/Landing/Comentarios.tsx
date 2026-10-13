import { useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useComentarios } from '@/hooks/landing/useComentarios';
import type { Comentario } from '@/hooks/landing/useComentarios';
import { cn } from '@/lib/cn';
import { variables } from './estilos';

const MUESTRAS: (Comentario & { color: string })[] = [
  {
    nombre: 'Laura M.',
    negocio: 'Almacén de barrio',
    puntaje: 5,
    color: 'naranja',
    texto:
      'Antes llevaba el stock en un cuaderno. Ahora armé el inventario en una tarde y sé qué reponer sin contar cajas.',
  },
  {
    nombre: 'Diego R.',
    negocio: 'Distribuidora de bebidas',
    puntaje: 5,
    color: 'coral',
    texto:
      'Conectamos pedidos con clientes y se terminaron los mensajes perdidos. Todo el equipo ve el mismo estado.',
  },
  {
    nombre: 'Carla S.',
    negocio: 'Estudio de diseño',
    puntaje: 4,
    color: 'pizarra',
    texto:
      'No sé programar y no lo necesité. Empecé con la plantilla de clientes y la fui adaptando a mi estudio.',
  },
];

const COLORES_AVATAR = ['naranja', 'coral', 'arena', 'ambar', 'pizarra'];
const MINIMO_COMENTARIO = 5;

function valorDe(form: HTMLFormElement, nombre: string): string {
  const campo = form.elements.namedItem(nombre);
  return campo && 'value' in campo ? campo.value : '';
}

type Mensaje = { tipo: 'ok' | 'error'; texto: string } | null;

function TarjetaComentario({
  comentario,
  color,
  retraso,
  nuevo,
}: {
  comentario: Comentario;
  color: string;
  retraso?: string;
  nuevo?: boolean;
}) {
  const { nombre, negocio, puntaje, texto } = comentario;
  return (
    <article
      // Los comentarios propios ya están a la vista: sólo las muestras se revelan al hacer scroll.
      className={cn('comentario revelar', retraso === undefined && 'visible', nuevo && 'nuevo')}
      style={retraso === undefined ? undefined : variables({ '--d': retraso })}
    >
      <div className="puntaje" role="img" aria-label={`Puntaje: ${puntaje} de 5`}>
        {[1, 2, 3, 4, 5].map((i) => (
          <i key={i} className={i > puntaje ? 'off' : undefined} />
        ))}
      </div>
      <blockquote>{`“${texto}”`}</blockquote>
      <footer>
        <span className={`avatar avatar--${color}`} aria-hidden="true">
          {nombre.charAt(0).toUpperCase()}
        </span>
        <div>
          <b>{nombre}</b>
          <small>{negocio || 'Comunidad Ensambla'}</small>
        </div>
      </footer>
    </article>
  );
}

export function Comentarios() {
  const { propios, agregar } = useComentarios();
  const [nuevo, setNuevo] = useState<Comentario | null>(null);
  const [mensaje, setMensaje] = useState<Mensaje>(null);
  const [invalido, setInvalido] = useState({ nombre: false, comentario: false });
  const lista = useRef<HTMLDivElement>(null);

  const publicar = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const nombre = valorDe(form, 'nombre').trim();
    const negocio = valorDe(form, 'negocio').trim();
    const texto = valorDe(form, 'comentario').trim();
    const puntaje = Number(valorDe(form, 'puntaje')) || 5;

    const faltaNombre = !nombre;
    const textoCorto = texto.length < MINIMO_COMENTARIO;
    setInvalido({ nombre: faltaNombre, comentario: textoCorto });

    if (faltaNombre || textoCorto) {
      setMensaje({
        tipo: 'error',
        texto: faltaNombre
          ? 'Contanos tu nombre.'
          : `El comentario necesita al menos ${MINIMO_COMENTARIO} caracteres.`,
      });
      const campo = form.elements.namedItem(faltaNombre ? 'nombre' : 'comentario');
      if (campo instanceof HTMLElement) campo.focus();
      return;
    }

    const comentario = { nombre, negocio, puntaje, texto };
    agregar(comentario);
    setNuevo(comentario);
    form.reset();
    setMensaje({ tipo: 'ok', texto: '¡Gracias! Tu comentario ya es parte del sistema.' });
    lista.current?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
  };

  const limpiarError = (campo: 'nombre' | 'comentario') => {
    if (invalido[campo]) setInvalido((prev) => ({ ...prev, [campo]: false }));
  };

  return (
    <section className="seccion" id="comentarios">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">Comentarios</p>
          <h2>
            Lo que dicen quienes
            <br />
            ya ensamblan.
          </h2>
          <p>Negocios reales que ordenaron su día a día, un bloque a la vez.</p>
        </header>

        <div className="comentarios" ref={lista}>
          {[...propios].reverse().map((c, i) => (
            <TarjetaComentario
              key={`${propios.length - i}-${c.nombre}-${c.texto}`}
              comentario={c}
              color={
                COLORES_AVATAR[(c.nombre.charCodeAt(0) || 0) % COLORES_AVATAR.length] ?? 'naranja'
              }
              nuevo={c === nuevo}
            />
          ))}
          {MUESTRAS.map(({ color, ...c }, i) => (
            <TarjetaComentario key={c.nombre} comentario={c} color={color} retraso={`${i / 10}s`} />
          ))}
        </div>

        <div className="opinar revelar">
          <div className="opinar__texto">
            <h3>Dejanos tu comentario</h3>
            <p>¿Probaste Ensambla o tenés una idea para un bloque nuevo? Contanos.</p>
          </div>
          <form className="formulario" onSubmit={publicar} noValidate>
            <div className="formulario__fila">
              <label className="campo">
                <span>Nombre</span>
                <input
                  type="text"
                  name="nombre"
                  autoComplete="given-name"
                  required
                  maxLength={40}
                  placeholder="Tu nombre"
                  aria-invalid={invalido.nombre}
                  onChange={() => limpiarError('nombre')}
                />
              </label>
              <label className="campo">
                <span>
                  Negocio <em>(opcional)</em>
                </span>
                <input
                  type="text"
                  name="negocio"
                  maxLength={50}
                  placeholder="Ej.: Ferretería del centro"
                />
              </label>
            </div>
            <fieldset className="campo campo--puntaje">
              <legend>Puntaje</legend>
              <div className="selector-puntaje">
                {[1, 2, 3, 4, 5].map((n) => (
                  <PuntajeOpcion key={n} n={n} />
                ))}
              </div>
            </fieldset>
            <label className="campo">
              <span>Comentario</span>
              <textarea
                name="comentario"
                rows={3}
                required
                maxLength={280}
                placeholder="¿Qué te pareció?"
                aria-invalid={invalido.comentario}
                onChange={() => limpiarError('comentario')}
              />
            </label>
            <div className="formulario__pie">
              <p className={cn('formulario__mensaje', mensaje?.tipo)} role="status">
                {mensaje?.texto}
              </p>
              <button type="submit" className="boton boton--primario">
                Publicar comentario
              </button>
            </div>
          </form>
        </div>
      </div>
    </section>
  );
}

function PuntajeOpcion({ n }: { n: number }) {
  return (
    <>
      <input
        type="radio"
        name="puntaje"
        id={`puntaje-${n}`}
        value={n}
        defaultChecked={n === 5}
        aria-label={n === 1 ? '1 bloque' : `${n} bloques`}
      />
      <label htmlFor={`puntaje-${n}`} aria-hidden="true" />
    </>
  );
}
