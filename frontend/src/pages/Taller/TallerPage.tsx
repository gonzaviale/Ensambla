import { useEffect, useRef, useState, type DragEvent, type FormEvent } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { LayoutToolbar } from '@/components/sistema/LayoutToolbar';
import { PreviewBoard } from '@/components/sistema/PreviewBoard';
import { Sami } from '@/components/sistema/Sami';
import { varsDeColor } from '@/components/sistema/colores';
import { CATALOG, TEMPLATES } from '@/lib/sistema/catalogo';
import {
  allBlocks,
  applyLayout,
  clone,
  findBlock,
  makeBlock,
  moveBlock,
  wireSystem,
} from '@/lib/sistema/core';
import type { ClavePlantilla, Distribucion, TipoBloque } from '@/lib/sistema/tipos';
import { saludoDeSami, useNavegacion, useNombre } from '@/hooks/sistemas/useNavegacion';
import { useTaller } from '@/hooks/sistemas/useTaller';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { useSistemaStore } from '@/stores/sistemaStore';

const EJEMPLOS: { clave: ClavePlantilla; etiqueta: string; nombre: string; texto: string }[] = [
  {
    clave: 'shop',
    etiqueta: 'Almacén o kiosco',
    nombre: 'Mi almacén',
    texto:
      'Tengo un almacén de barrio. Vendo productos de almacén y kiosco, anoto lo que me fían y quiero saber qué stock me queda y a qué precio vender.',
  },
  {
    clave: 'warehouse',
    etiqueta: 'Un depósito',
    nombre: 'Mi depósito',
    texto:
      'Tengo un depósito de productos. Necesito cargar productos, registrar entradas y salidas y saber cuánto stock queda.',
  },
  {
    clave: 'business',
    etiqueta: 'Un local de ropa',
    nombre: 'Mi tienda de ropa',
    texto:
      'Tengo un local de indumentaria. Vendo ropa y accesorios, manejo productos con precios y stock y anoto pedidos de mis clientes.',
  },
];

const AJUSTES: { mensaje: string; etiqueta: string }[] = [
  { mensaje: 'Me falta controlar gastos', etiqueta: 'Sumar gastos' },
  { mensaje: 'Prefiero dos columnas y el stock de todo el ancho', etiqueta: 'Otra distribución' },
  { mensaje: 'Separar clientes en otra sección', etiqueta: 'Separar clientes' },
];

const PIEZAS_PARA_SUMAR: TipoBloque[] = ['clients', 'sales', 'inventory', 'orders', 'expenses'];

// El taller de Sami: una conversación a la izquierda y la propuesta, editable, a la derecha.
export function TallerPage() {
  useTituloPagina('La guía de Sami');
  const nombre = useNombre();
  const taller = useSistemaStore((estado) => estado.taller);
  const actualizarTaller = useSistemaStore((estado) => estado.actualizarTaller);
  const editarBorrador = useSistemaStore((estado) => estado.editarBorrador);
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const avisar = useSistemaStore((estado) => estado.avisar);
  const navegacion = useNavegacion();
  const { enviar } = useTaller();
  const mensajes = useRef<HTMLDivElement>(null);
  const campo = useRef<HTMLTextAreaElement>(null);
  const [arrastrando, setArrastrando] = useState<string | null>(null);
  const [sobre, setSobre] = useState<string | null>(null);

  // Si se entra por la dirección, sin pasar por un botón, la conversación empieza igual.
  useEffect(() => {
    if (!useSistemaStore.getState().taller) {
      useSistemaStore.getState().iniciarTaller(saludoDeSami(nombre));
    }
  }, [nombre]);

  const cantidadDeMensajes = taller?.mensajes.length ?? 0;
  const pensando = taller?.pensando ?? false;
  useEffect(() => {
    if (!mensajes.current) return;
    mensajes.current.scrollTop = mensajes.current.scrollHeight;
    if (!pensando && cantidadDeMensajes > 1) campo.current?.focus({ preventScroll: true });
  }, [cantidadDeMensajes, pensando]);

  if (!taller) return null;
  const { borrador } = taller;
  const seccionId = taller.seccionId ?? borrador?.sections[0]?.id ?? null;

  const alEnviar = (evento: FormEvent) => {
    evento.preventDefault();
    void enviar();
  };

  const usarEjemplo = (ejemplo: (typeof EJEMPLOS)[number]) => {
    actualizarTaller({ nombre: ejemplo.nombre });
    void enviar(ejemplo.texto);
  };

  const sumarPieza = (tipo: TipoBloque) =>
    editarBorrador((copia) => {
      const seccion = copia.sections.find((item) => item.id === seccionId) ?? copia.sections[0];
      seccion?.blocks.push(makeBlock(tipo, true));
      wireSystem(copia);
    });

  const quitarPieza = (bloqueId: string) =>
    editarBorrador((copia) => {
      for (const seccion of copia.sections) {
        seccion.blocks = seccion.blocks.filter((bloque) => bloque.id !== bloqueId);
      }
    });

  const empezarArrastre = (evento: DragEvent<HTMLButtonElement>, bloqueId: string) => {
    const carga = JSON.stringify({ kind: 'draft', value: bloqueId });
    evento.dataTransfer.setData('application/x-ensambla', carga);
    evento.dataTransfer.setData('text/plain', carga);
    evento.dataTransfer.effectAllowed = 'move';
    setArrastrando(bloqueId);
  };

  const terminarArrastre = () => {
    setArrastrando(null);
    setSobre(null);
  };

  const soltar = (antesDe: string | null) => {
    const movida = arrastrando;
    terminarArrastre();
    if (!movida || !seccionId || antesDe === movida) return;
    editarBorrador((copia) => {
      moveBlock(copia, movida, seccionId, antesDe);
    });
  };

  const crearSistema = () => {
    if (!borrador) return;
    const sistema = clone(borrador);
    sistema.name = sistema.name.trim() || 'Mi sistema';
    // La propuesta se imaginó con datos de ejemplo; el sistema nuevo arranca vacío.
    for (const bloque of allBlocks(sistema)) bloque.rows = [];
    void navegacion.crearYAbrir(sistema);
  };

  return (
    <>
      <CabeceraComun />
      <main id="main" tabIndex={-1} className="creation-studio reveal">
        <div className="studio-top">
          <Btn cls="ghost small" onClick={() => void navegacion.irAInicio()}>
            <Icon name="back" /> Inicio
          </Btn>
          <span>EL TALLER DE TU PRIMER SISTEMA</span>
          <span className="tag outline">
            {borrador ? 'Propuesta sin guardar' : 'Tu idea es el punto de partida'}
          </span>
        </div>
        <div className="studio-layout">
          <aside className="sami-conversation">
            <header className="conversation-head">
              <Sami pose={taller.pensando ? 'think' : 'hello'} />
              <div>
                <h1>Lo armamos juntos.</h1>
                <p>
                  <span className="live-dot" />
                  Sami · Tu guía
                </p>
              </div>
            </header>
            <div className="conversation-messages" aria-live="polite" ref={mensajes}>
              {taller.mensajes.map((mensaje, indice) => (
                <div key={indice} className={`conversation-bubble ${mensaje.quien}`}>
                  {mensaje.texto}
                </div>
              ))}
              {taller.pensando && (
                <div className="thinking-bubble">
                  <Sami pose="think" />
                  <div>
                    <strong>Estoy buscando qué piezas encajan…</strong>
                    <span>
                      <i />
                      <i />
                      <i />
                    </span>
                  </div>
                </div>
              )}
            </div>
            {borrador ? (
              <div className="feedback-chips">
                {AJUSTES.map(({ mensaje, etiqueta }) => (
                  <button
                    key={etiqueta}
                    type="button"
                    disabled={taller.pensando}
                    onClick={() => void enviar(mensaje)}
                  >
                    {etiqueta}
                  </button>
                ))}
              </div>
            ) : (
              <div className="conversation-examples">
                <span>O probá con un ejemplo:</span>
                {EJEMPLOS.map((ejemplo) => (
                  <button
                    key={ejemplo.clave}
                    type="button"
                    disabled={taller.pensando}
                    onClick={() => usarEjemplo(ejemplo)}
                  >
                    <Icon name={TEMPLATES[ejemplo.clave].icon} />
                    {ejemplo.etiqueta}
                    <Icon name="arrow" />
                  </button>
                ))}
              </div>
            )}
            <form className="guide-composer" onSubmit={alEnviar}>
              <label htmlFor="guide-message">
                {borrador ? '¿Qué le falta? ¿Qué cambiarías?' : 'Contame un poco de tu negocio'}
              </label>
              <textarea
                id="guide-message"
                ref={campo}
                placeholder={
                  borrador
                    ? 'Por ejemplo: quiero los clientes en otra sección…'
                    : 'Tengo un almacén, vendo productos y anoto lo que me fían…'
                }
                maxLength={2500}
                disabled={taller.pensando}
                value={taller.entrada}
                onChange={(evento) => actualizarTaller({ entrada: evento.target.value })}
              />
              <div className="row between">
                <Btn
                  cls="ghost small"
                  aria-label="Aportar una foto o captura"
                  onClick={() => avisar('La carga desde una foto llega con el editor.')}
                >
                  <Icon name="camera" />
                </Btn>
                <button className="btn primary" type="submit" disabled={taller.pensando}>
                  {borrador ? 'Ajustalo conmigo' : 'Pensalo conmigo'} <Icon name="arrow" />
                </button>
              </div>
            </form>
            <p className="conversation-foot">Vos decidís qué queda y cuándo está listo.</p>
          </aside>

          <section className="studio-canvas">
            {borrador ? (
              <>
                <div className="draft-heading">
                  <div>
                    <span className="eyebrow">TU PROPUESTA, A LA VISTA</span>
                    <input
                      className="draft-name-input"
                      id="guide-system-name"
                      aria-label="Nombre de tu sistema"
                      maxLength={100}
                      value={borrador.name}
                      onChange={(evento) =>
                        editarBorrador((copia) => {
                          copia.name = evento.target.value;
                        })
                      }
                    />
                  </div>
                  <span className="tag">{allBlocks(borrador).length} piezas conectadas</span>
                </div>
                <p className="draft-intro">
                  Esto ya tiene forma. Cambiá nombres, anchos y posiciones, o pedile un ajuste a
                  Sami.
                </p>
                <LayoutToolbar
                  distribucion={borrador.layout}
                  onElegir={(distribucion: Distribucion) =>
                    editarBorrador((copia) => {
                      applyLayout(copia, distribucion);
                    })
                  }
                />
                <div className="draft-palette">
                  <span>Sumar una pieza:</span>
                  {PIEZAS_PARA_SUMAR.map((tipo) => (
                    <button
                      key={tipo}
                      type="button"
                      style={varsDeColor(CATALOG[tipo].color)}
                      onClick={() => sumarPieza(tipo)}
                    >
                      <Icon name={CATALOG[tipo].icon} />
                      {CATALOG[tipo].name}
                      <Icon name="plus" />
                    </button>
                  ))}
                </div>
                <div className="draft-section-tabs">
                  {borrador.sections.map((seccion) => (
                    <button
                      key={seccion.id}
                      type="button"
                      className={seccionId === seccion.id ? 'active' : ''}
                      onClick={() => actualizarTaller({ seccionId: seccion.id })}
                    >
                      {seccion.name}
                      <span>{seccion.blocks.length}</span>
                    </button>
                  ))}
                  <span className="preview-example-label">Datos de ejemplo para imaginarlo</span>
                </div>
                <PreviewBoard
                  sistema={borrador}
                  seccionId={seccionId}
                  editable
                  arrastrando={arrastrando}
                  sobre={sobre}
                  onRenombrar={(bloqueId) => abrirModal({ tipo: 'renombrarPieza', bloqueId })}
                  onQuitar={quitarPieza}
                  onAncho={(bloqueId, ancho) =>
                    editarBorrador((copia) => {
                      const bloque = findBlock(copia, bloqueId);
                      if (bloque) bloque.width = ancho;
                    })
                  }
                  onArrastrar={empezarArrastre}
                  onSobre={setSobre}
                  onSoltar={soltar}
                  onFinArrastre={terminarArrastre}
                />
                <div className="draft-footer">
                  <div>
                    <Icon name="check-circle" />
                    <span>
                      <strong>¿Se parece a tu manera de trabajar?</strong>
                      <small>Podés seguir ajustándolo después de crearlo.</small>
                    </span>
                  </div>
                  <Btn cls="primary" onClick={crearSistema}>
                    Sí, crear mi sistema <Icon name="arrow" />
                  </Btn>
                </div>
              </>
            ) : (
              <div className="studio-empty">
                <div className="empty-studio-label">TU IDEA VA A APARECER ACÁ</div>
                <div className="ghost-composition">
                  <div className="ghost-piece ghost-clients">
                    <Icon name="users" />
                    <strong>Tus clientes</strong>
                    <i />
                    <i />
                  </div>
                  <div className="ghost-piece ghost-orders">
                    <Icon name="orders" />
                    <strong>Tus ventas</strong>
                    <i />
                    <i />
                  </div>
                  <div className="ghost-piece ghost-stock">
                    <Icon name="box" />
                    <strong>Lo que necesitás organizar</strong>
                    <div>
                      <i />
                      <i />
                      <i />
                    </div>
                  </div>
                  <span className="floating-piece yellow" />
                  <span className="floating-piece petrol" />
                </div>
                <h2>
                  Primero te escucho.
                  <br />
                  Después, lo vemos armado.
                </h2>
                <p>
                  Una propuesta de bloques que podés mover, combinar y cambiar. Sin tener que
                  imaginar el sistema a ciegas.
                </p>
                <span className="empty-studio-note">
                  <Icon name="spark" />
                  No hay una única forma de hacerlo.
                </span>
              </div>
            )}
          </section>
        </div>
      </main>
    </>
  );
}
