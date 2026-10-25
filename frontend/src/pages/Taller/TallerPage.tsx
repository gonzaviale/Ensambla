import {
  useEffect,
  useRef,
  useState,
  type ChangeEvent,
  type DragEvent,
  type FormEvent,
} from 'react';
import { getRouteApi } from '@tanstack/react-router';
import { Btn } from '@/components/sistema/Btn';
import { BotoneraDeDudas } from '@/components/sistema/BotoneraDeDudas';
import { BurbujasDeConversacion } from '@/components/sistema/BurbujasDeConversacion';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { LayoutToolbar } from '@/components/sistema/LayoutToolbar';
import { PreviewBoard } from '@/components/sistema/PreviewBoard';
import { TablaCompleta } from '@/components/sistema/editor/TablaCompleta';
import { Sami } from '@/components/sistema/Sami';
import { VisorDeFoto } from '@/components/sistema/VisorDeFoto';
import { varsDeColor } from '@/components/sistema/colores';
import { ajusteParaGuardar, mismoSistema, sistemaParaCrear } from '@/lib/sistema/acciones';
import { debeMostrarConsejosDeFoto } from '@/lib/sistema/consejosDeFoto';
import { CATALOG } from '@/lib/sistema/catalogo';
import {
  allBlocks,
  applyLayout,
  findBlock,
  makeBlock,
  moveBlock,
  wireSystem,
} from '@/lib/sistema/core';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type { Distribucion, TipoBloque } from '@/lib/sistema/tipos';
import {
  saludoDeSami,
  saludoParaAjustar,
  useNavegacion,
  useNombre,
} from '@/hooks/sistemas/useNavegacion';
import { useDictado } from '@/hooks/sistemas/useDictado';
import { useTaller, vistaDeFoto } from '@/hooks/sistemas/useTaller';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { useSistemaStore } from '@/stores/sistemaStore';

// Para arrancar sin escribir: el texto va a Sami igual que si la persona lo hubiera escrito.
const EJEMPLOS: {
  clave: string;
  icono: NombreDeIcono;
  etiqueta: string;
  nombre: string;
  texto: string;
}[] = [
  {
    clave: 'almacen',
    icono: 'check-circle',
    etiqueta: 'Almacén o kiosco',
    nombre: 'Mi almacén',
    texto:
      'Tengo un almacén de barrio. Vendo productos de almacén y kiosco, anoto lo que me fían y quiero saber qué stock me queda y a qué precio vender.',
  },
  {
    clave: 'velas',
    icono: 'spark',
    etiqueta: 'Velas artesanales',
    nombre: 'Mis velas',
    texto:
      'Hago velas artesanales y las vendo por Instagram y en ferias. Compro cera, mechas y esencias, y anoto los pedidos de mis clientas.',
  },
  {
    clave: 'peluqueria',
    icono: 'clock',
    etiqueta: 'Una peluquería',
    nombre: 'Mi peluquería',
    texto:
      'Tengo una peluquería. Doy turnos para corte y color, quiero tener a mis clientes con su teléfono y saber qué productos me quedan.',
  },
];

const PIEZAS_PARA_SUMAR: TipoBloque[] = ['clients', 'sales', 'inventory', 'orders', 'expenses'];

const ruta = getRouteApi('/_app/taller');

// El taller de Sami: una conversación a la izquierda y la propuesta, editable, a la derecha. Con `?sistema=<id>`,
// la propuesta arranca como una copia de ese sistema y «Guardar cambios» lo reemplaza.
export function TallerPage() {
  useTituloPagina('La guía de Sami');
  const nombre = useNombre();
  const { sistema: sistemaDeLaUrl } = ruta.useSearch();
  const taller = useSistemaStore((estado) => estado.taller);
  const original = useSistemaStore((estado) =>
    estado.taller?.sistemaId
      ? estado.sistemas.find((item) => item.id === estado.taller?.sistemaId)
      : undefined,
  );
  const actualizarTaller = useSistemaStore((estado) => estado.actualizarTaller);
  const editarBorrador = useSistemaStore((estado) => estado.editarBorrador);
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const tablaCompletaId = useSistemaStore((estado) => estado.tablaCompletaId);
  const verTablaCompleta = useSistemaStore((estado) => estado.verTablaCompleta);
  const navegacion = useNavegacion();
  const { enviar, responderDuda, deshacer, adjuntarFoto, quitarFoto } = useTaller();
  const avisar = useSistemaStore((estado) => estado.avisar);
  // La voz se transcribe en el navegador y queda en el campo para revisarla antes de enviar.
  const dictado = useDictado({
    obtenerBase: () => useSistemaStore.getState().taller?.entrada ?? '',
    alTexto: (entrada) => actualizarTaller({ entrada }),
    alAvisar: avisar,
  });
  const mensajes = useRef<HTMLDivElement>(null);
  const campo = useRef<HTMLTextAreaElement>(null);
  const selectorDeFoto = useRef<HTMLInputElement>(null);
  const [preparandoFoto, setPreparandoFoto] = useState(false);
  const [arrastrando, setArrastrando] = useState<string | null>(null);
  const [sobre, setSobre] = useState<string | null>(null);
  const [fotoAmpliada, setFotoAmpliada] = useState(false);
  // El chat flota sobre la propuesta; se minimiza a un botón redondo para ver todo el lienzo.
  const [chatAbierto, setChatAbierto] = useState(true);
  const botonDelChat = useRef<HTMLButtonElement>(null);
  const pasoDelTutorial = useSistemaStore((estado) =>
    estado.tutorial.activo ? estado.tutorial.paso : null,
  );

  // En el celular el chat tapa la propuesta: cuando la guía pide crear el cuaderno, se minimiza para
  // que se vea el botón «Sí, crear mi cuaderno».
  useEffect(() => {
    if (pasoDelTutorial === 'create' && window.matchMedia?.('(max-width: 760px)').matches) {
      setChatAbierto(false);
    }
  }, [pasoDelTutorial]);

  // Si se entra por la dirección, sin pasar por un botón, la conversación empieza igual: sobre el sistema que diga
  // la dirección o, si no hay (o ya no existe), una nueva.
  useEffect(() => {
    const estado = useSistemaStore.getState();
    const sistema = sistemaDeLaUrl
      ? estado.sistemas.find((item) => item.id === sistemaDeLaUrl)
      : undefined;
    if (sistema) {
      if (estado.taller?.sistemaId !== sistema.id) {
        estado.iniciarTaller(saludoParaAjustar(sistema.name), sistema);
      }
    } else if (!estado.taller || estado.taller.sistemaId) {
      estado.iniciarTaller(saludoDeSami(nombre));
    }
  }, [nombre, sistemaDeLaUrl]);

  // La tabla a pantalla completa no queda abierta al salir del taller.
  useEffect(() => () => useSistemaStore.getState().verTablaCompleta(null), []);

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
  const tablaCompleta = borrador ? findBlock(borrador, tablaCompletaId ?? undefined) : undefined;
  const ajustando = Boolean(original);
  const hayCambios =
    !original || !borrador || !mismoSistema(ajusteParaGuardar(original, borrador), original);

  const alEnviar = (evento: FormEvent) => {
    evento.preventDefault();
    dictado.detener();
    void enviar();
  };

  // Como en Claude: siempre se puede dejar las opciones y escribir lo que uno quiera.
  const escribirOtraCosa = () => {
    actualizarTaller({ dudas: [], respuestasDeDudas: [] });
    campo.current?.focus();
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

  // La foto va con el próximo mensaje: se achica al elegirla y se ve en miniatura hasta mandarla.
  const elegirFoto = async (evento: ChangeEvent<HTMLInputElement>) => {
    const archivo = evento.target.files?.[0];
    evento.target.value = '';
    if (!archivo) return;
    setPreparandoFoto(true);
    try {
      await adjuntarFoto(archivo);
    } finally {
      setPreparandoFoto(false);
    }
    campo.current?.focus();
  };

  // La cámara primero explica cómo sacar una buena foto; «Tomar foto» abre el selector.
  const elegirDondeSacarLaFoto = () => {
    const abrirSelector = () => selectorDeFoto.current?.click();
    if (debeMostrarConsejosDeFoto())
      abrirModal({ tipo: 'consejosDeFoto', onTomarFoto: abrirSelector });
    else abrirSelector();
  };

  const sacarFoto = () => {
    setFotoAmpliada(false);
    quitarFoto();
    campo.current?.focus();
  };

  // La propuesta se imaginó con datos de ejemplo, que no pasan al sistema; los registros reales que cargó Sami, sí.
  const crearSistema = () => {
    if (!borrador) return;
    void navegacion.crearYAbrir(sistemaParaCrear(borrador));
  };

  // Reemplaza el sistema con la propuesta (así se guarda en el servidor) y vuelve a él, en el modo en que estaba.
  const guardarCambios = () => {
    if (!borrador || !original) return;
    const estado = useSistemaStore.getState();
    const modo = estado.activeId === original.id ? estado.modo : 'use';
    if (hayCambios) {
      const ajustado = ajusteParaGuardar(original, borrador);
      estado.actualizarSistema(original.id, (sistema) => {
        for (const clave of Object.keys(sistema)) {
          if (!(clave in ajustado)) Reflect.deleteProperty(sistema, clave);
        }
        Object.assign(sistema, ajustado);
      });
      avisar(`Guardé los cambios en «${ajustado.name}».`);
    } else {
      avisar('No había cambios para guardar.');
    }
    void navegacion.abrirSistema(original.id, modo);
  };

  const abrirChat = () => {
    setChatAbierto(true);
    requestAnimationFrame(() => campo.current?.focus());
  };

  const minimizarChat = () => {
    setChatAbierto(false);
    requestAnimationFrame(() => botonDelChat.current?.focus());
  };

  const volver = () => {
    if (original) void navegacion.abrirSistema(original.id);
    else void navegacion.irAInicio();
  };

  return (
    <>
      <CabeceraComun />
      <main id="main" tabIndex={-1} className="creation-studio reveal">
        <div className="studio-top">
          <Btn cls="ghost small" onClick={volver}>
            <Icon name="back" /> {original ? `Volver a ${original.name}` : 'Inicio'}
          </Btn>
          <span>
            {ajustando ? 'AJUSTANDO TU SISTEMA CON SAMI' : 'EL TALLER DE TU PRIMER SISTEMA'}
          </span>
          <span className="tag outline">
            {ajustando
              ? hayCambios
                ? 'Cambios sin guardar'
                : 'Sin cambios todavía'
              : borrador
                ? 'Propuesta sin guardar'
                : 'Tu idea es el punto de partida'}
          </span>
        </div>
        <div className="studio-layout chat-flotante">
          <section className="studio-canvas">
            {borrador ? (
              <>
                <div className="draft-heading">
                  <div>
                    <span className="eyebrow">
                      {ajustando ? 'TU SISTEMA, CON LOS CAMBIOS' : 'TU PROPUESTA, A LA VISTA'}
                    </span>
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
                  {ajustando
                    ? 'Pedile a Sami lo que quieras cambiar, o cambiá nombres, anchos y posiciones acá. Tus registros se conservan.'
                    : 'Esto ya tiene forma. Cambiá nombres, anchos y posiciones, o pedile un ajuste a Sami.'}
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
                  <span className="preview-example-label">
                    {ajustando
                      ? 'Las filas de ejemplo no se guardan ni son reales'
                      : 'Las filas marcadas «Ejemplo» no son reales: están para imaginarlo'}
                  </span>
                </div>
                <PreviewBoard
                  sistema={borrador}
                  seccionId={seccionId}
                  editable
                  arrastrando={arrastrando}
                  sobre={sobre}
                  onRenombrar={(bloqueId) => abrirModal({ tipo: 'renombrarPieza', bloqueId })}
                  onRegistro={(bloqueId, filaId) =>
                    abrirModal({ tipo: 'registroDelBorrador', bloqueId, filaId })
                  }
                  onQuitar={quitarPieza}
                  onTablaCompleta={verTablaCompleta}
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
                <div className="draft-footer" data-tour="create">
                  <div>
                    <Icon name="check-circle" />
                    {ajustando ? (
                      <span>
                        <strong>¿Te sirve así?</strong>
                        <small>Se guarda sobre tu sistema, con tus registros.</small>
                      </span>
                    ) : (
                      <span>
                        <strong>¿Se parece a tu manera de trabajar?</strong>
                        <small>Podés seguir ajustándolo después de crearlo.</small>
                      </span>
                    )}
                  </div>
                  {ajustando ? (
                    <Btn cls="primary" data-accion="guide-save" onClick={guardarCambios}>
                      Guardar cambios <Icon name="arrow" />
                    </Btn>
                  ) : (
                    <Btn cls="primary" data-accion="guide-create" onClick={crearSistema}>
                      Sí, crear mi cuaderno <Icon name="arrow" />
                    </Btn>
                  )}
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
                {/* <span className="empty-studio-note">
                  <Icon name="spark" />
                  No hay una única forma de hacerlo.
                </span> */}
              </div>
            )}
          </section>
        </div>
        {fotoAmpliada && taller.foto && (
          <VisorDeFoto
            src={vistaDeFoto(taller.foto)}
            alt="La foto que vas a mandar a Sami, en grande"
            alCerrar={() => setFotoAmpliada(false)}
          />
        )}
      </main>
      {/* Fuera del <main>: su animación de entrada haría que «fixed» se mueva con la página. */}
      <aside
        id="chat-de-sami"
        className="sami-conversation flotante"
        aria-label="Chat con Sami"
        hidden={!chatAbierto}
      >
        <header className="conversation-head">
          <img className="conversation-sami" src="/mockup/sami-constructora.webp" alt="" />
          <div>
            <h1>{ajustando ? 'Lo ajustamos juntos.' : 'Lo armamos juntos.'}</h1>
            <p>
              <span className="live-dot" />
              Sami · Tu guía
            </p>
          </div>
          <button
            type="button"
            className="chat-minimizar"
            aria-label="Minimizar el chat"
            onClick={minimizarChat}
          >
            <Icon name="close" />
          </button>
        </header>
        <div className="conversation-scroll" ref={mensajes}>
          <div className="conversation-messages" aria-live="polite">
            {taller.mensajes.map((mensaje, indice) => (
              <BurbujasDeConversacion
                key={indice}
                quien={mensaje.quien}
                texto={mensaje.texto}
                foto={mensaje.foto}
              />
            ))}
            {taller.pensando && (
              <div className="thinking-bubble" data-tour="describe-pensando">
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
          {taller.dudas[taller.respuestasDeDudas.length] ? (
            <BotoneraDeDudas
              dudas={taller.dudas}
              respondidas={taller.respuestasDeDudas.length}
              pensando={taller.pensando}
              onResponder={responderDuda}
              onEscribirOtraCosa={escribirOtraCosa}
            />
          ) : borrador ? null : (
            <div className="conversation-examples">
              <span>O probá con un ejemplo:</span>
              {EJEMPLOS.map((ejemplo) => (
                <button
                  key={ejemplo.clave}
                  type="button"
                  disabled={taller.pensando}
                  onClick={() => usarEjemplo(ejemplo)}
                >
                  <Icon name={ejemplo.icono} />
                  {ejemplo.etiqueta}
                  <Icon name="arrow" />
                </button>
              ))}
            </div>
          )}
          {taller.anterior && (
            <div className="feedback-chips">
              <button type="button" disabled={taller.pensando} onClick={deshacer}>
                <Icon name="undo" /> Deshacer lo último
              </button>
            </div>
          )}
        </div>
        <form className="guide-composer" data-tour="describe" onSubmit={alEnviar}>
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
          {taller.foto && (
            <div className="guide-photo-preview">
              <button
                type="button"
                className="guide-photo-thumb"
                aria-label="Ver la foto en grande"
                onClick={() => setFotoAmpliada(true)}
              >
                <img src={vistaDeFoto(taller.foto)} alt="La foto que vas a mandar a Sami" />
              </button>
              <button
                type="button"
                className="guide-photo-remove"
                data-accion="guide-photo-remove"
                aria-label="Quitar foto"
                disabled={taller.pensando}
                onClick={sacarFoto}
              >
                <Icon name="close" />
              </button>
            </div>
          )}
          {/* Con `image/*` el celular ofrece sacar la foto en el momento o elegir una de la galería. */}
          <input
            ref={selectorDeFoto}
            id="guide-photo-file"
            type="file"
            accept="image/*"
            hidden
            onChange={(evento) => void elegirFoto(evento)}
          />
          <div className="row between">
            <div className="row">
              <Btn
                cls="ghost small"
                data-accion="guide-photo"
                aria-label={taller.foto ? 'Cambiar la foto para Sami' : 'Mandarle una foto a Sami'}
                disabled={taller.pensando || preparandoFoto}
                onClick={elegirDondeSacarLaFoto}
              >
                <Icon name="camera" />
              </Btn>
              <Btn
                cls={dictado.escuchando ? 'soft small' : 'ghost small'}
                data-accion="guide-voice"
                aria-label={dictado.escuchando ? 'Dejar de dictar' : 'Dictar con la voz'}
                aria-pressed={dictado.escuchando}
                disabled={taller.pensando}
                onClick={dictado.alternar}
              >
                <Icon name="mic" />
              </Btn>
            </div>
            <button
              className="btn primary"
              type="submit"
              disabled={taller.pensando || preparandoFoto}
            >
              {borrador ? 'Ajustalo conmigo' : 'Pensalo conmigo'} <Icon name="arrow" />
            </button>
          </div>
        </form>
        <p className="conversation-foot">
          {ajustando
            ? 'Nada cambia en tu sistema hasta que guardes.'
            : 'Vos decidís qué queda y cuándo está listo.'}
        </p>
      </aside>
      {!chatAbierto && (
        <button
          ref={botonDelChat}
          type="button"
          className="chat-abrir"
          aria-label="Abrir el chat con Sami"
          aria-controls="chat-de-sami"
          aria-expanded={false}
          onClick={abrirChat}
        >
          <img src="/mockup/sami-boton.webp" alt="" />
        </button>
      )}
      {borrador && tablaCompleta && (
        <TablaCompleta
          key={tablaCompleta.id}
          bloque={tablaCompleta}
          sistema={borrador}
          textoVolver="Volver a la propuesta"
          onRegistro={(filaId) =>
            abrirModal({ tipo: 'registroDelBorrador', bloqueId: tablaCompleta.id, filaId })
          }
        />
      )}
    </>
  );
}
