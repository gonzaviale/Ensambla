import { useEffect, useRef } from 'react';
import { Link } from '@tanstack/react-router';
import { EJEMPLO, REGLAS_DEL_DUENO } from '@/hooks/landing/ejemploLista';
import { useDemoLista } from '@/hooks/landing/useDemoLista';
import { pesos, porcentaje } from '@/lib/red/formato';

type Destino = 'confirmar' | 'revisar' | 'resultado';

// El corazón de la landing: una línea de lista tal como llega, y lo que Ensambla hace con ella.
export function ListaLlega() {
  const { paso, regla, resultado, revisar, confirmar, elegirRegla, reiniciar } = useDemoLista();

  const botonRevisar = useRef<HTMLButtonElement>(null);
  const botonConfirmar = useRef<HTMLButtonElement>(null);
  const panelListo = useRef<HTMLDivElement>(null);
  // Al cambiar de paso el botón que se tocó desaparece: el foco pasa al próximo control, no se pierde.
  const foco = useRef<Destino | null>(null);

  const irA = (destino: Destino, accion: () => void) => {
    foco.current = destino;
    accion();
  };

  useEffect(() => {
    const destino = foco.current;
    foco.current = null;
    if (destino === 'confirmar') botonConfirmar.current?.focus();
    if (destino === 'revisar') botonRevisar.current?.focus();
    if (destino === 'resultado') panelListo.current?.focus();
  }, [paso]);

  const linea = `${EJEMPLO.textoDeLista} — ${pesos(EJEMPLO.precioDeLaCaja)}`;

  return (
    <section className="seccion seccion--tono" id="lista-llega" aria-labelledby="lista-titulo">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">La lista llega</p>
          <h2 id="lista-titulo">Llegó la lista de tu proveedor.</h2>
          <p>
            Ensambla te muestra qué precios revisar y te ayuda a actualizarlos en tu negocio.
            Probalo con una línea de verdad.
          </p>
        </header>

        <div className="demo revelar">
          <div className="demo__lado">
            <p className="demo__etiqueta">Así te llega</p>
            <div className="demo__mensaje">
              <p className="demo__origen">{EJEMPLO.proveedor} · lista de precios</p>
              <p className="demo__linea">{linea}</p>
              <p className="demo__detalle">
                Una caja de {EJEMPLO.unidadesPorCaja}. ¿Cuánto te sale cada una? ¿Subió? ¿A cuánto
                lo tenés que vender?
              </p>
            </div>
            {paso === 'llego' && (
              <button
                ref={botonRevisar}
                type="button"
                className="boton boton--primario boton--grande"
                onClick={() => irA('confirmar', revisar)}
              >
                Ensambla, revisá
              </button>
            )}
            {paso !== 'llego' && (
              <button
                type="button"
                className="boton boton--fantasma"
                onClick={() => irA('revisar', reiniciar)}
              >
                Ver de nuevo
              </button>
            )}
          </div>

          <div className="demo__lado demo__lado--resultado" aria-live="polite">
            <p className="demo__etiqueta">Lo que ve el comercio</p>

            {paso === 'llego' && (
              <p className="demo__vacio">
                Tocá «Ensambla, revisá» y mirá cuánto subió, a qué precio tendrías que venderlo y
                qué margen te queda.
              </p>
            )}

            {paso === 'revisado' && (
              <div className="demo__tarjeta">
                <h3>{EJEMPLO.producto}</h3>
                <dl className="demo__datos">
                  <div>
                    <dt>Te sale cada una</dt>
                    <dd>
                      {pesos(resultado.costoNuevo)}
                      <small>
                        {pesos(EJEMPLO.precioDeLaCaja)} ÷ {EJEMPLO.unidadesPorCaja}
                      </small>
                    </dd>
                  </div>
                  <div>
                    <dt>Cuánto subió</dt>
                    <dd>
                      {porcentaje(resultado.subioPorcentaje, { signo: true })}
                      <small>
                        de {pesos(EJEMPLO.costoAnterior)} a {pesos(resultado.costoNuevo)}
                      </small>
                    </dd>
                  </div>
                  <div>
                    <dt>Hoy lo vendés a</dt>
                    <dd>
                      {pesos(EJEMPLO.precioActual)}
                      <small>
                        te queda {porcentaje(resultado.margenHoy)}
                        {resultado.enRiesgo && (
                          <>
                            {' '}
                            · <b className="demo__riesgo">Precio en riesgo</b>
                          </>
                        )}
                      </small>
                    </dd>
                  </div>
                  <div className="demo__datos--destacado">
                    <dt>Precio sugerido</dt>
                    <dd>
                      {pesos(resultado.precioSugerido)}
                      <small>te queda {porcentaje(resultado.margenNuevo)}</small>
                    </dd>
                  </div>
                </dl>

                <div
                  className="demo__regla"
                  role="group"
                  aria-label="Tu regla: ganar sobre el costo"
                >
                  <span className="demo__regla-texto">Tu regla: ganar sobre el costo</span>
                  <div className="demo__reglas">
                    {REGLAS_DEL_DUENO.map((valor) => (
                      <button
                        key={valor}
                        type="button"
                        className="demo__chip"
                        aria-pressed={regla === valor}
                        onClick={() => elegirRegla(valor)}
                      >
                        {porcentaje(valor)}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="demo__acciones">
                  <button
                    ref={botonConfirmar}
                    type="button"
                    className="boton boton--primario"
                    onClick={() => irA('resultado', confirmar)}
                  >
                    Confirmar
                  </button>
                  <p className="demo__garantia">Nada cambia sin tu confirmación.</p>
                </div>
              </div>
            )}

            {paso === 'confirmado' && (
              <div className="demo__tarjeta demo__tarjeta--listo" ref={panelListo} tabIndex={-1}>
                <p className="demo__listo">
                  <span className="demo__tilde" aria-hidden="true">
                    ✓
                  </span>
                  Listo: cambiá la etiqueta
                </p>
                <p className="demo__etiqueta-nueva">
                  {EJEMPLO.producto}: <strong>{pesos(resultado.precioSugerido)}</strong>
                </p>
                <p className="demo__detalle">
                  Antes estaba a {pesos(EJEMPLO.precioActual)}. Ahora te queda{' '}
                  {porcentaje(resultado.margenNuevo)} y el aviso de precio en riesgo se apaga solo.
                </p>
                <Link to="/registro" className="enlace">
                  Probalo con tus listas <span aria-hidden="true">→</span>
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
