import { useEffect, useLayoutEffect, useRef, useState, type FormEvent } from 'react';
import { GRUPOS, pesos, type GrupoDeAviso } from '@/lib/sistema/alertas';
import { MINIMO_DE_COMERCIOS } from '@/lib/sistema/zona';
import { useAccionesDeAvisos } from '@/hooks/avisos/useAccionesDeAvisos';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useZona } from '@/hooks/zona/queries';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { Sami } from '../Sami';
import { TarjetaDeAviso } from './TarjetaDeAviso';
import { InvitacionConfigurarComercio } from '../ubicacion/InvitacionConfigurarComercio';
import { useFaltaConfiguracionNegocio } from '@/hooks/negocio/queries';

const PREGUNTAS = [
  '¿Qué tengo que reponer?',
  '¿Qué se vende cerca que no tengo?',
  '¿Cuánto cobran la yerba en mi zona?',
  '¿Cómo vienen mis márgenes?',
];

const GRUPOS_EN_ORDEN = Object.entries(GRUPOS) as [GrupoDeAviso, string][];

// Un rato después de cerrar el panel sigue montado para que se vea la animación de salida.
const SALIDA_MS = 200;

function conClaseEnRaiz(clase: string, activa: boolean) {
  document.querySelector('.mk-root')?.classList.toggle(clase, activa);
}

// El panel lateral con los avisos de Sami: filtros, preguntas cortas, tarjetas y ajustes.
export function PanelDeAvisos() {
  const panel = useSistemaStore((estado) => estado.panel);
  const avisos = useSistemaStore((estado) => estado.avisos);
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const cerrarPanel = useSistemaStore((estado) => estado.cerrarPanel);
  const actualizarPanel = useSistemaStore((estado) => estado.actualizarPanel);
  const zona = useZona();
  const faltaConfiguracion = useFaltaConfiguracionNegocio();
  const navegacion = useNavegacion();
  const acciones = useAccionesDeAvisos();
  const [montado, setMontado] = useState(false);
  const [visible, setVisible] = useState(false);
  const [pregunta, setPregunta] = useState('');
  const ajustes = useRef<HTMLDetailsElement>(null);
  const abierto = panel.abierto;

  // Entrar y salir con animación; el foco va al botón de cerrar y vuelve a la campana.
  useEffect(() => {
    if (abierto) {
      setMontado(true);
      conClaseEnRaiz('alertas-abiertas', true);
      const cuadro = requestAnimationFrame(() => setVisible(true));
      return () => cancelAnimationFrame(cuadro);
    }
    setVisible(false);
    conClaseEnRaiz('alertas-abiertas', false);
    const salida = setTimeout(() => setMontado(false), SALIDA_MS);
    return () => clearTimeout(salida);
  }, [abierto]);

  useEffect(() => {
    if (!montado || !abierto) return;
    const objetivo = panel.foco
      ? document.querySelector(`[data-alerta="${CSS.escape(panel.foco)}"]`)
      : null;
    if (objetivo) objetivo.scrollIntoView({ block: 'center' });
    else document.querySelector<HTMLElement>('.alertas-panel .icon-btn')?.focus();
  }, [montado, abierto, panel.foco]);

  const habiaAbierto = useRef(false);
  useEffect(() => {
    if (habiaAbierto.current && !abierto) {
      document.querySelector<HTMLElement>('.alert-bell')?.focus({ preventScroll: true });
    }
    habiaAbierto.current = abierto;
  }, [abierto]);

  // Esc cierra el panel antes que cualquier otra cosa (captura: se adelanta al resto).
  useEffect(() => {
    if (!abierto) return;
    const alTeclear = (evento: KeyboardEvent) => {
      if (evento.key !== 'Escape') return;
      evento.stopPropagation();
      cerrarPanel();
    };
    document.addEventListener('keydown', alTeclear, true);
    return () => document.removeEventListener('keydown', alTeclear, true);
  }, [abierto, cerrarPanel]);

  // La cinta de filtros: una marca que se desliza hasta el filtro elegido y flechas para correrla.
  const cinta = useRef<HTMLDivElement>(null);
  const [marca, setMarca] = useState<{ left: number; width: number }>();
  const [bordes, setBordes] = useState({ izquierda: false, derecha: false });

  const medirCinta = () => {
    const nodo = cinta.current;
    if (!nodo) return;
    setBordes({
      izquierda: nodo.scrollLeft > 2,
      derecha: nodo.scrollLeft + nodo.clientWidth < nodo.scrollWidth - 2,
    });
  };

  useLayoutEffect(() => {
    const activo = cinta.current?.querySelector<HTMLElement>(`[data-filtro="${panel.filtro}"]`);
    if (activo) {
      setMarca({ left: activo.offsetLeft, width: activo.offsetWidth });
      activo.scrollIntoView?.({ block: 'nearest', inline: 'nearest', behavior: 'smooth' });
    }
    medirCinta();
  }, [panel.filtro, visible, montado]);

  const deslizar = (sentido: number) =>
    cinta.current?.scrollBy?.({ left: sentido * 160, behavior: 'smooth' });

  if (!montado) return null;

  const urgentes = avisos.filter((aviso) => aviso.prioridad >= 2).length;
  const filtros: [string, string, number][] = [
    ['todas', 'Todas', avisos.length],
    ['urgentes', 'Urgentes', urgentes],
    ...GRUPOS_EN_ORDEN.map(([clave, nombre]): [string, string, number] => [
      clave,
      nombre,
      avisos.filter((aviso) => aviso.grupo === clave).length,
    ]),
  ];
  const lista = avisos.filter(
    (aviso) =>
      panel.filtro === 'todas' ||
      (panel.filtro === 'urgentes' ? aviso.prioridad >= 2 : aviso.grupo === panel.filtro),
  );
  const hayDemo = sistemas.some((sistema) => sistema.demoAlmacen);
  const sinDatos = !sistemas.some((sistema) =>
    sistema.sections.some((seccion) => seccion.blocks.some((bloque) => bloque.rows.length)),
  );
  const zonaSuficiente = zona.comercios >= MINIMO_DE_COMERCIOS;

  const preguntar = (texto: string) => {
    setPregunta(texto);
    acciones.preguntar(texto);
  };

  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    acciones.preguntar(pregunta);
  };

  const tarjetas = (items: typeof avisos) =>
    items.map((aviso) => (
      <TarjetaDeAviso
        key={aviso.id}
        aviso={aviso}
        enfocado={panel.foco === aviso.id}
        onAccion={acciones.ejecutar}
        onDescartar={acciones.descartar}
      />
    ));

  const respuesta = panel.respuesta;

  return (
    <div id="alertas-capa" className={`alertas-capa ${visible ? 'visible' : ''}`.trim()}>
      <div className="alertas-fondo" onClick={cerrarPanel} />
      <div
        className="alertas-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="alertas-titulo"
      >
        <div className="alertas-barra">
          <button
            type="button"
            className="alertas-cerrar"
            aria-label="Cerrar avisos"
            title="Cerrar"
            onClick={cerrarPanel}
          >
            <Icon name="close" />
          </button>
        </div>
        <div className="alertas-head">
          <img className="alertas-sami" src="/mockup/sami-detective.webp" alt="" />
          <div>
            <span className="eyebrow">Sami está atento</span>
            <h2 id="alertas-titulo">
              {avisos.length ? 'Lo que encontré en tus datos' : 'Tus avisos'}
            </h2>
            <p>
              {avisos.length > 0 &&
                `${urgentes ? `${urgentes} para resolver pronto · ` : ''}${avisos.length} en total. `}
              Miré {sistemas.length === 1 ? '1 sistema' : `${sistemas.length} sistemas`}{' '}
              {zonaSuficiente
                ? `y los datos de tu zona (${zona.comercios} comercios, sin nombres).`
                : 'pero todavía no hay comercios suficientes en tu zona para usar sus datos.'}
            </p>
          </div>
        </div>
        <InvitacionConfigurarComercio visible={faltaConfiguracion} onIr={cerrarPanel} />
        <form className="alertas-pregunta" onSubmit={enviar}>
          <label className="sr-only" htmlFor="alertas-q">
            Preguntale a Sami
          </label>
          <Icon name="search" />
          <input
            id="alertas-q"
            className="input"
            placeholder="Preguntale a Sami: ¿cuánto cobran la yerba en mi zona?"
            autoComplete="off"
            value={pregunta}
            onChange={(evento) => setPregunta(evento.target.value)}
          />
          <button className="btn primary small" type="submit">
            Preguntar
          </button>
        </form>
        <div className="alertas-sugeridas">
          {PREGUNTAS.map((texto) => (
            <button key={texto} type="button" onClick={() => preguntar(texto)}>
              {texto}
            </button>
          ))}
        </div>
        {respuesta ? (
          <div className="alertas-respuesta" role="status">
            <div className="alertas-respuesta-txt">
              <Sami pose="think" />
              <p>{respuesta.texto}</p>
            </div>
            {respuesta.precios && respuesta.precios.length > 0 && (
              <ul className="alertas-ofertas">
                {respuesta.precios.map((producto) => (
                  <li key={producto.nombre}>
                    <button
                      type="button"
                      onClick={() => {
                        cerrarPanel();
                        void navegacion.irAZona();
                      }}
                    >
                      <span>
                        <strong>{producto.nombre}</strong>
                        <small>
                          {producto.comercios} comercios · de {pesos(producto.precio.minimo)} a{' '}
                          {pesos(producto.precio.maximo)}
                        </small>
                      </span>
                      <b>{pesos(producto.precio.promedio)}</b>
                    </button>
                  </li>
                ))}
              </ul>
            )}
            {respuesta.avisos.length > 0 && (
              <div className="alertas-lista mini">{tarjetas(respuesta.avisos)}</div>
            )}
            <button
              type="button"
              className="text-link"
              onClick={() => actualizarPanel({ respuesta: null })}
            >
              Volver a todos los avisos
            </button>
          </div>
        ) : (
          <>
            <div className="alertas-cinta">
              <button
                type="button"
                className="cinta-flecha"
                aria-label="Ver filtros anteriores"
                disabled={!bordes.izquierda}
                onClick={() => deslizar(-1)}
              >
                <Icon name="back" />
              </button>
              <div
                ref={cinta}
                className="alertas-filtros"
                role="group"
                aria-label="Filtrar avisos"
                onScroll={medirCinta}
              >
                <span className="alertas-filtro-marca" style={marca} aria-hidden="true" />
                {filtros.map(([clave, texto, cantidad]) => (
                  <button
                    key={clave}
                    type="button"
                    data-filtro={clave}
                    className={panel.filtro === clave ? 'active' : ''}
                    aria-pressed={panel.filtro === clave}
                    onClick={() => actualizarPanel({ filtro: clave })}
                  >
                    {texto}
                    {cantidad ? (
                      <>
                        {' '}
                        <span>{cantidad}</span>
                      </>
                    ) : null}
                  </button>
                ))}
              </div>
              <button
                type="button"
                className="cinta-flecha"
                aria-label="Ver más filtros"
                disabled={!bordes.derecha}
                onClick={() => deslizar(1)}
              >
                <Icon name="arrow" />
              </button>
            </div>
            <div className="alertas-lista">
              {lista.length ? (
                tarjetas(lista)
              ) : (
                <div className={sinDatos ? 'alertas-vacio' : 'alertas-vacio ok'}>
                  {sinDatos ? (
                    <Sami />
                  ) : (
                    <img className="alertas-vacio-img" src="/mockup/sami-guino.webp" alt="" />
                  )}
                  {sinDatos ? (
                    <>
                      <h3>Todavía no tengo datos para mirar.</h3>
                      <p>
                        Cuando cargues stock, ventas o precios, te aviso lo que necesita atención y
                        lo comparo con los datos de tu zona.
                      </p>
                      {!hayDemo && (
                        <Btn cls="primary" onClick={acciones.crearDemo}>
                          Probar con un almacén de ejemplo
                        </Btn>
                      )}
                    </>
                  ) : (
                    <>
                      <h3>
                        {panel.filtro === 'todas' ? 'Todo en orden.' : 'Nada en esta categoría.'}
                      </h3>
                      <p>
                        Sigo mirando tu stock, tus ventas y los precios de tu zona. Si algo cambia,
                        te aviso.
                      </p>
                    </>
                  )}
                </div>
              )}
            </div>
          </>
        )}
        <details className="alertas-ajustes" ref={ajustes}>
          <summary>
            <span className="ajustes-icono">
              <Icon name="settings" />
            </span>
            <span className="ajustes-titulo">
              <strong>Qué me avisa Sami</strong>
              <small>Elegí de qué temas querés recibir avisos</small>
            </span>
            <Icon name="down" />
          </summary>
          {GRUPOS_EN_ORDEN.map(([clave, nombre]) => (
            <label className="check-row ajuste-tema" key={clave}>
              <input
                type="checkbox"
                checked={acciones.grupos[clave] !== false}
                onChange={(evento) => acciones.alternarGrupo(clave, evento.target.checked)}
              />
              {nombre}
            </label>
          ))}
          {acciones.descartadas > 0 && (
            <button type="button" className="text-link" onClick={acciones.restaurar}>
              Volver a mostrar{' '}
              {acciones.descartadas === 1
                ? '1 aviso oculto'
                : `${acciones.descartadas} avisos ocultos`}
            </button>
          )}
          {!hayDemo && !sinDatos && (
            <button type="button" className="text-link" onClick={acciones.crearDemo}>
              Crear un almacén de ejemplo para probar
            </button>
          )}
          <p className="alertas-nota">
            <Icon name="help" />
            Sami arma los avisos con reglas sobre tus datos y sobre datos agregados de tu zona:
            nunca se muestra un comercio puntual y hace falta un mínimo de {
              MINIMO_DE_COMERCIOS
            }{' '}
            para que un dato cuente. Cada aviso muestra de dónde sale. No cambia nada sin que vos lo
            apliques.
          </p>
        </details>
      </div>
    </div>
  );
}
