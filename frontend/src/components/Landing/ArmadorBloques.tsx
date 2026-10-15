import { useEffect, useRef } from 'react';
import { Link } from '@tanstack/react-router';
import { useArmador } from '@/hooks/landing/useArmador';
import { cn } from '@/lib/cn';
import { variables } from './estilos';

const PIEZAS = [
  { nombre: 'Inventario', color: 'naranja', icono: '▤' },
  { nombre: 'Pedidos', color: 'coral', icono: '✓' },
  { nombre: 'Clientes', color: 'arena', icono: '◎' },
  { nombre: 'Proveedores', color: 'pizarra', icono: '⇄' },
  { nombre: 'Ventas', color: 'ambar', icono: '$' },
  { nombre: 'Turnos', color: 'coral', icono: '◷' },
] as const;

export function ArmadorBloques() {
  const { bloques, cantidad, estaActivo, alternar, reiniciar, mostrarDemo } = useArmador();
  const seccion = useRef<HTMLElement>(null);

  // Arranca con un par de bloques cuando la sección entra en pantalla, para mostrar la idea.
  useEffect(() => {
    if (!seccion.current || typeof IntersectionObserver === 'undefined') return;
    const observador = new IntersectionObserver(
      ([entrada]) => {
        if (!entrada?.isIntersecting) return;
        observador.disconnect();
        mostrarDemo();
      },
      { threshold: 0.4 },
    );
    observador.observe(seccion.current);
    return () => observador.disconnect();
  }, [mostrarDemo]);

  return (
    <section className="seccion seccion--tono" id="bloques" ref={seccion}>
      <div className="contenedor armador">
        <div className="armador__texto revelar">
          <p className="sobretitulo">Probalo acá</p>
          <h2>Armá tu sistema en segundos.</h2>
          <p>
            Tocá los bloques que necesita tu comercio y mirá cómo se ensamblan. Los precios y los
            avisos de arriba se apoyan en estos mismos bloques.
          </p>

          <div className="piezas" role="group" aria-label="Bloques disponibles">
            {PIEZAS.map(({ nombre, color }) => (
              <button
                key={nombre}
                type="button"
                className="pieza"
                data-color={color}
                aria-pressed={estaActivo(nombre)}
                onClick={() => alternar(nombre)}
              >
                {nombre}
              </button>
            ))}
          </div>

          <div className="armador__acciones">
            <Link to="/registro" className="boton boton--primario">
              Crear este sistema
            </Link>
            <button type="button" className="boton boton--fantasma" onClick={reiniciar}>
              Empezar de nuevo
            </button>
          </div>
        </div>

        <div className="tablero revelar" style={variables({ '--d': '.1s' })}>
          <div className="tablero__barra">
            <span className="tablero__puntos" aria-hidden="true">
              <i />
              <i />
              <i />
            </span>
            <span className="tablero__titulo">Mi negocio</span>
            <span className="tablero__contador" aria-live="polite">
              {cantidad === 1 ? '1 bloque' : `${cantidad} bloques`}
            </span>
          </div>
          <div className={cn('tablero__grilla', cantidad > 0 && 'con-bloques')}>
            <p className="tablero__vacio">
              <span className="tablero__fantasma" aria-hidden="true" />
              Elegí un bloque para empezar
            </p>
            {bloques.map(({ id, nombre, saliendo }) => {
              const pieza = PIEZAS.find((p) => p.nombre === nombre);
              return (
                <div
                  key={id}
                  className={cn('bloque', saliendo && 'saliendo')}
                  data-color={pieza?.color}
                >
                  <div className="bloque__cabeza">
                    <span>{nombre}</span>
                    <span className="bloque__icono" aria-hidden="true">
                      {pieza?.icono}
                    </span>
                  </div>
                  <div className="bloque__campos" aria-hidden="true">
                    <i />
                    <i />
                    <i />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </section>
  );
}
