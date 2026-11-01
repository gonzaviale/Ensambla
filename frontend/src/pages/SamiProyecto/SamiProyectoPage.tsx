import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { BurbujasDeConversacion } from '@/components/sistema/BurbujasDeConversacion';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { Sami } from '@/components/sistema/Sami';
import { useDictado } from '@/hooks/sistemas/useDictado';
import { saludoDeSami, useNavegacion, useNombre } from '@/hooks/sistemas/useNavegacion';
import { useSamiProyecto } from '@/hooks/sistemas/useSamiProyecto';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { ColumnaSugerida } from '@/types/samiProyecto';

const NOMBRE_DE_TIPO: Record<string, string> = {
  TEXTO: 'texto',
  NUMERO: 'número',
  FECHA: 'fecha',
  OPCION: 'opción',
  SI_NO: 'sí/no',
};

const tipoEnPalabras = (tipo: string) => NOMBRE_DE_TIPO[tipo] ?? tipo.toLowerCase();

const detalleDeColumna = (columna: ColumnaSugerida) =>
  columna.tipo === 'OPCION' && columna.opciones.length
    ? `${tipoEnPalabras(columna.tipo)}: ${columna.opciones.join(', ')}`
    : tipoEnPalabras(columna.tipo);

// «Armalo con Sami» (T-118): la conversación que arma un proyecto real a partir de cómo la persona cuenta su negocio.
export function SamiProyectoPage() {
  useTituloPagina('Armalo con Sami');
  const nombrePersona = useNombre();
  const navegacion = useNavegacion();
  const avisar = useSistemaStore((estado) => estado.avisar);
  const sami = useSamiProyecto(saludoDeSami(nombrePersona));
  const [entrada, setEntrada] = useState('');
  const mensajes = useRef<HTMLDivElement>(null);
  const dictado = useDictado({ obtenerBase: () => entrada, alTexto: setEntrada, alAvisar: avisar });

  const cantidadDeMensajes = sami.mensajes.length;
  useEffect(() => {
    if (mensajes.current) mensajes.current.scrollTop = mensajes.current.scrollHeight;
  }, [cantidadDeMensajes, sami.pensando]);

  const alEnviar = (evento: FormEvent) => {
    evento.preventDefault();
    dictado.detener();
    void sami.describir(entrada).then((enviado) => {
      if (enviado) setEntrada('');
    });
  };

  const { propuesta, plantilla, creado } = sami;
  const conversando = !propuesta && !creado;

  return (
    <>
      <CabeceraComun />
      <main id="main" tabIndex={-1} className="creation-studio reveal">
        <div className="studio-top">
          <Btn cls="ghost small" onClick={() => void navegacion.irAInicio()}>
            <Icon name="back" /> Inicio
          </Btn>
          <span>ARMALO CON SAMI</span>
          <span className="tag outline">
            {creado
              ? 'Proyecto creado'
              : propuesta
                ? 'Propuesta sin guardar'
                : 'Contame tu negocio'}
          </span>
        </div>
        <div className="studio-layout">
          <aside className="sami-conversation">
            <header className="conversation-head">
              <Sami pose={sami.pensando ? 'think' : 'hello'} />
              <div>
                <h1>Armemos tu proyecto.</h1>
                <p>
                  <span className="live-dot" />
                  Sami · Tu guía
                </p>
              </div>
            </header>
            <div className="conversation-scroll" ref={mensajes}>
              <div className="conversation-messages" aria-live="polite">
                {sami.mensajes.map((mensaje, indice) => (
                  <BurbujasDeConversacion
                    key={indice}
                    quien={mensaje.quien}
                    texto={mensaje.texto}
                  />
                ))}
                {sami.pensando && (
                  <div className="thinking-bubble">
                    <Sami pose="think" />
                    <div>
                      <strong>Estoy pensando qué te conviene…</strong>
                      <span>
                        <i />
                        <i />
                        <i />
                      </span>
                    </div>
                  </div>
                )}
              </div>
            </div>
            {sami.pregunta && (
              <div className="feedback-chips" role="group" aria-label="Respuestas rápidas">
                {sami.pregunta.opciones.map((opcion) => (
                  <button
                    key={opcion.id}
                    type="button"
                    disabled={sami.pensando}
                    onClick={() => void sami.elegir(opcion)}
                  >
                    {opcion.texto}
                  </button>
                ))}
              </div>
            )}
            {conversando && !sami.pregunta && (
              <form className="guide-composer" onSubmit={alEnviar}>
                <label htmlFor="sami-descripcion">Contame un poco de tu negocio</label>
                <textarea
                  id="sami-descripcion"
                  placeholder="Vendo velas y anoto quién me debe…"
                  maxLength={2500}
                  disabled={sami.pensando}
                  value={entrada}
                  onChange={(evento) => setEntrada(evento.target.value)}
                />
                <div className="row between">
                  <Btn
                    cls={dictado.escuchando ? 'soft small' : 'ghost small'}
                    aria-label={dictado.escuchando ? 'Dejar de dictar' : 'Dictar con la voz'}
                    aria-pressed={dictado.escuchando}
                    disabled={sami.pensando}
                    onClick={dictado.alternar}
                  >
                    <Icon name="mic" />
                  </Btn>
                  <button className="btn primary" type="submit" disabled={sami.pensando}>
                    Pensalo conmigo <Icon name="arrow" />
                  </button>
                </div>
              </form>
            )}
            {!conversando && (
              <div className="feedback-chips">
                <button type="button" disabled={sami.creando} onClick={sami.reiniciar}>
                  Empezar de nuevo
                </button>
              </div>
            )}
            <p className="conversation-foot">Vos decidís qué queda y cuándo está listo.</p>
          </aside>

          <section className="studio-canvas" aria-label="Tu proyecto">
            {creado ? (
              <div className="sp-hecho">
                <span className="eyebrow">LISTO</span>
                <h2>Tu proyecto «{creado.nombre}» ya está creado.</h2>
                <ul className="sp-columnas">
                  {creado.bloques.map((bloque) => (
                    <li key={bloque.nombre} className="sp-columna">
                      <span>{bloque.nombre}</span>
                      <small>{bloque.campos.length} columnas</small>
                    </li>
                  ))}
                </ul>
                <div className="sp-acciones">
                  <Btn cls="primary" onClick={() => void navegacion.irAInicio()}>
                    Volver al inicio <Icon name="arrow" />
                  </Btn>
                  <Btn cls="ghost" onClick={sami.reiniciar}>
                    Armar otro proyecto
                  </Btn>
                </div>
              </div>
            ) : propuesta ? (
              <>
                <div className="draft-heading">
                  <div>
                    <span className="eyebrow">TU PROPUESTA, A LA VISTA</span>
                    <input
                      className="draft-name-input"
                      aria-label="Nombre de tu proyecto"
                      maxLength={100}
                      value={sami.nombre}
                      onChange={(evento) => sami.setNombre(evento.target.value)}
                    />
                  </div>
                  <span className="tag">
                    {propuesta.origen === 'IA' ? 'Armada con ayuda de IA' : 'Armada por Sami'}
                  </span>
                </div>
                <p className="draft-intro">
                  Partimos de la plantilla «{propuesta.plantilla}». Sacá las columnas que no
                  necesites.
                </p>
                <div className="sp-bloques">
                  {(plantilla?.estructura.bloques ?? []).map((bloque, indice) => (
                    <section key={bloque.nombre} className="sp-bloque">
                      <h3>{bloque.nombre}</h3>
                      <ul className="sp-columnas">
                        {bloque.campos.map((campo) => (
                          <li key={campo.nombre} className="sp-columna">
                            <span>{campo.nombre}</span>
                            <small>{tipoEnPalabras(campo.tipo)}</small>
                          </li>
                        ))}
                        {indice === 0 &&
                          propuesta.columnas.map((columna) => {
                            const quitada = sami.quitadas.includes(columna.nombre);
                            return (
                              <li
                                key={columna.nombre}
                                className={`sp-columna extra${quitada ? 'quitada' : ''}`}
                              >
                                <span>{columna.nombre}</span>
                                <small>{quitada ? 'sin esta' : detalleDeColumna(columna)}</small>
                                <button
                                  type="button"
                                  onClick={() => sami.alternarColumna(columna.nombre)}
                                >
                                  {quitada ? 'Volver a sumar' : 'Quitar'}
                                  <span className="sr-only"> la columna {columna.nombre}</span>
                                </button>
                              </li>
                            );
                          })}
                      </ul>
                    </section>
                  ))}
                  {!plantilla && (
                    <section className="sp-bloque">
                      <h3>{propuesta.plantilla}</h3>
                      <ul className="sp-columnas">
                        {propuesta.columnas.map((columna) => (
                          <li key={columna.nombre} className="sp-columna extra">
                            <span>{columna.nombre}</span>
                            <small>{detalleDeColumna(columna)}</small>
                          </li>
                        ))}
                      </ul>
                    </section>
                  )}
                </div>
                <div className="draft-footer">
                  <div>
                    <Icon name="check-circle" />
                    <span>
                      <strong>¿Se parece a tu manera de trabajar?</strong>
                      <small>Podés seguir ajustándolo después de crearlo.</small>
                    </span>
                  </div>
                  <Btn
                    cls="primary"
                    disabled={sami.creando || !sami.nombre.trim()}
                    onClick={() => void sami.crearProyecto()}
                  >
                    Sí, crear mi proyecto <Icon name="arrow" />
                  </Btn>
                </div>
              </>
            ) : (
              <div className="studio-empty">
                <div className="empty-studio-label">TU PROPUESTA VA A APARECER ACÁ</div>
                <h2>Primero te escucho.</h2>
                <p>
                  Si hace falta te hago alguna pregunta. Después ves la estructura armada y decidís
                  si la creamos.
                </p>
                <span className="empty-studio-note">
                  <Icon name="spark" />
                  Podés escribir o dictar lo que hacés.
                </span>
              </div>
            )}
          </section>
        </div>
      </main>
    </>
  );
}
