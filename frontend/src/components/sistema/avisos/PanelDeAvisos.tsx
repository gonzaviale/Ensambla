import { ContenidoPremium } from '@/components/planes/ContenidoPremium';
import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { GRUPOS, type GrupoDeAviso } from '@/lib/sistema/alertas';
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

const GRUPOS_EN_ORDEN = Object.entries(GRUPOS) as [GrupoDeAviso, string][];

// Los avisos de Sami en su propia pantalla: el botón al taller de Sami, los filtros y las tarjetas.
export function PanelDeAvisos() {
  const panel = useSistemaStore((estado) => estado.panel);
  const avisos = useSistemaStore((estado) => estado.avisos);
  const acceso = useSistemaStore((estado) => estado.accesoAvisos);
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const actualizarPanel = useSistemaStore((estado) => estado.actualizarPanel);
  const navegacion = useNavegacion();
  const zona = useZona();
  const faltaConfiguracion = useFaltaConfiguracionNegocio();
  const acciones = useAccionesDeAvisos();

  // Mientras la pantalla está a la vista no salta «Sami vio un cambio».
  useEffect(() => {
    actualizarPanel({ abierto: true });
    return () => actualizarPanel({ abierto: false });
  }, [actualizarPanel]);

  // Si se llegó desde un aviso puntual, la pantalla baja hasta él.
  useEffect(() => {
    if (!panel.foco) return;
    document
      .querySelector(`[data-alerta="${CSS.escape(panel.foco)}"]`)
      ?.scrollIntoView?.({ block: 'center' });
  }, [panel.foco]);

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
  }, [panel.filtro]);

  const deslizar = (sentido: number) =>
    cinta.current?.scrollBy?.({ left: sentido * 160, behavior: 'smooth' });

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

  return (
    <section className="alertas-panel" aria-labelledby="alertas-titulo">
      <div className="alertas-head">
        <img className="alertas-sami" src="/mockup/sami-detective.webp" alt="" />
        <div>
          <span className="eyebrow">Sami está atento</span>
          <h1 id="alertas-titulo">
            {avisos.length ? 'Lo que encontré en tus datos' : 'Tus avisos'}
          </h1>
          <p>
            {avisos.length > 0 &&
              `${urgentes ? `${urgentes} para resolver pronto · ` : ''}${avisos.length} ${acceso?.bloqueadas ? 'visibles' : 'en total'}. `}
            {acceso?.bloqueadas ? `${acceso.bloqueadas} más con Premium. ` : ''}
            Miré {sistemas.length === 1 ? '1 cuaderno' : `${sistemas.length} cuadernos`}{' '}
            {zonaSuficiente
              ? `y los datos de tu zona (${zona.comercios} comercios, sin nombres).`
              : 'pero todavía no hay comercios suficientes en tu zona para usar sus datos.'}
          </p>
        </div>
      </div>
      <InvitacionConfigurarComercio visible={faltaConfiguracion} />
      <button
        type="button"
        className="alertas-preguntar"
        onClick={() => void navegacion.irATaller()}
      >
        <img src="/mockup/sami-boton.webp" alt="" />
        <span>
          <strong>Preguntale a Sami</strong>
          <small>Armá o ajustá tus tablas charlando con Sami.</small>
        </span>
        <Icon name="arrow" />
      </button>
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
          lista.map((aviso) => (
            <TarjetaDeAviso
              key={aviso.id}
              aviso={aviso}
              enfocado={panel.foco === aviso.id}
              onAccion={acciones.ejecutar}
              onDescartar={acciones.descartar}
            />
          ))
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
                  Cuando cargues stock, ventas o precios, te aviso lo que necesita atención y lo
                  comparo con los datos de tu zona.
                </p>
                {!hayDemo && (
                  <Btn cls="primary" onClick={acciones.crearDemo}>
                    Probar con un almacén de ejemplo
                  </Btn>
                )}
              </>
            ) : (
              <>
                <h3>{panel.filtro === 'todas' ? 'Todo en orden.' : 'Nada en esta categoría.'}</h3>
                <p>
                  Sigo mirando tu stock, tus ventas y los precios de tu zona. Si algo cambia, te
                  aviso.
                </p>
              </>
            )}
          </div>
        )}
      </div>
      <div className="alertas-pie">
        <button
          type="button"
          className="text-link"
          onClick={() => void navegacion.irACuenta('notificaciones')}
        >
          <Icon name="settings" /> Elegir qué avisos recibo
        </button>
        {!hayDemo && !sinDatos && (
          <button type="button" className="text-link" onClick={acciones.crearDemo}>
            Crear un almacén de ejemplo para probar
          </button>
        )}
      </div>
      <ContenidoPremium acceso={acceso} recurso="avisos" compacto />
    </section>
  );
}
