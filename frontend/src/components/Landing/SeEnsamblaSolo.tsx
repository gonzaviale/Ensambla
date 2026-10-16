import { useId } from 'react';
import { Link } from '@tanstack/react-router';
import { EJEMPLOS_RELATO, useEnsamblarRelato } from '@/hooks/landing/useEnsamblarRelato';
import { NOMBRE_RUBRO } from '@/lib/catalogo/piezas';
import { cn } from '@/lib/cn';
import { plural } from '@/lib/formato';
import type { PiezaId } from '@/types/negocio';
import { variables } from './estilos';

const ESTILO_PIEZA: Record<PiezaId, { color: string; icono: string }> = {
  productos: { color: 'naranja', icono: '▤' },
  ventas: { color: 'ambar', icono: '$' },
  gastos: { color: 'pizarra', icono: '−' },
  fiados: { color: 'coral', icono: '◎' },
  pedidos: { color: 'arena', icono: '✓' },
  insumos: { color: 'celeste', icono: '◔' },
};

// Demo en vivo: lo que la persona cuenta se convierte en bloques y en las respuestas que desbloquean.
export function SeEnsamblaSolo() {
  const { texto, setTexto, ensamblado, desbloqueadas } = useEnsamblarRelato();
  const idCampo = useId();
  const idAyuda = `${idCampo}-ayuda`;
  const cantidad = ensamblado?.piezas.length ?? 0;

  return (
    <section className="seccion seccion--tono" id="se-arma" aria-labelledby="se-arma-titulo">
      <div className="contenedor armador demo-relato">
        <div className="armador__texto revelar">
          <p className="sobretitulo">Probalo acá</p>
          <h2 id="se-arma-titulo">Se ensambla solo.</h2>
          <p>
            Contá en una frase cómo es tu negocio y mirá cómo se acomodan los bloques. Nada de armar
            tablas.
          </p>

          <div className="relato">
            <label htmlFor={idCampo}>Contá tu negocio con tus palabras</label>
            <textarea
              id={idCampo}
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
              aria-describedby={idAyuda}
              placeholder="Por ejemplo: vendo viandas por encargo y cobro una seña"
              rows={3}
            />
            <p id={idAyuda} className="relato__ayuda">
              Es una demostración: no guardamos lo que escribís.
            </p>
          </div>

          <div className="piezas" role="group" aria-label="Ejemplos para probar">
            {EJEMPLOS_RELATO.map((ejemplo) => (
              <button
                key={ejemplo}
                type="button"
                className="pieza"
                data-color="naranja"
                aria-pressed={texto === ejemplo}
                onClick={() => setTexto(ejemplo)}
              >
                {ejemplo}
              </button>
            ))}
          </div>

          <div className="armador__acciones">
            <Link to="/probar" className="boton boton--primario">
              Armar el mío
            </Link>
          </div>
        </div>

        <div className="tablero revelar" style={variables({ '--d': '.1s' })}>
          <div className="tablero__barra">
            <span className="tablero__puntos" aria-hidden="true">
              <i />
              <i />
              <i />
            </span>
            <span className="tablero__titulo">Tu sistema</span>
            <span className="tablero__contador" role="status">
              {plural(cantidad, 'bloque', 'bloques')}
            </span>
          </div>
          {ensamblado && (
            <p className="tablero__rubro">Lo leí como: {NOMBRE_RUBRO[ensamblado.rubro]}</p>
          )}
          <div className={cn('tablero__grilla', ensamblado && 'con-bloques')}>
            {!ensamblado && (
              <p className="tablero__vacio">
                <span className="tablero__fantasma" aria-hidden="true" />
                Escribí tu negocio para empezar
              </p>
            )}
            {ensamblado?.piezas.map(({ id, nombre, motivo }) => {
              const { color, icono } = ESTILO_PIEZA[id];
              return (
                <div key={id} className="bloque" data-color={color}>
                  <div className="bloque__cabeza">
                    <span>{nombre}</span>
                    <span className="bloque__icono" aria-hidden="true">
                      {icono}
                    </span>
                  </div>
                  <p className="bloque__motivo">{motivo}</p>
                </div>
              );
            })}
          </div>
          <div className="desbloqueos">
            <h3>Y ya podés saber:</h3>
            {desbloqueadas.length === 0 ? (
              <p className="desbloqueos__vacio">
                Cuando se acomoden los bloques, acá aparecen las respuestas.
              </p>
            ) : (
              <ul>
                {desbloqueadas.map(({ id, pregunta, bloque }) => (
                  <li key={id}>
                    {pregunta}
                    <small>con {bloque}</small>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
