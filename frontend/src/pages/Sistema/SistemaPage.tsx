import { useEffect, useState } from 'react';
import { getRouteApi, useNavigate } from '@tanstack/react-router';
import { Btn } from '@/components/sistema/Btn';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { FranjaDeSami } from '@/components/sistema/avisos/FranjaDeSami';
import { BibliotecaDeBloques } from '@/components/sistema/editor/BibliotecaDeBloques';
import { BloqueEnLienzo } from '@/components/sistema/editor/BloqueEnLienzo';
import { CierreDeCreacion } from '@/components/sistema/CierreDeCreacion';
import { EncabezadoDelEditor } from '@/components/sistema/editor/EncabezadoDelEditor';
import { Inspector } from '@/components/sistema/editor/Inspector';
import { TablaCompleta } from '@/components/sistema/editor/TablaCompleta';
import { Icon } from '@/components/sistema/Icon';
import { IconBtn } from '@/components/sistema/IconBtn';
import { IconTile } from '@/components/sistema/IconTile';
import { LayoutToolbar } from '@/components/sistema/LayoutToolbar';
import { CATALOG } from '@/lib/sistema/catalogo';
import { varsDeColor } from '@/components/sistema/colores';
import { allBlocks, findBlock, totals } from '@/lib/sistema/core';
import type { Color, Distribucion, TipoBloque } from '@/lib/sistema/tipos';
import {
  agregarBloque,
  aplicarDistribucion,
  deshacerCambio,
  pedirQuitarEjemplos,
} from '@/stores/accionesDelEditor';
import { MODO_EN_LA_URL, useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useArrastreDelEditor } from '@/hooks/sistemas/useArrastreDelEditor';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import {
  seleccionarBloqueElegido,
  seleccionarSeccionActiva,
  seleccionarSistemaActivo,
  useSistemaStore,
  type ModoDelEditor,
} from '@/stores/sistemaStore';

const ruta = getRouteApi('/_app/sistemas/$sistemaId');

// Cada sección lleva un color de la paleta, en orden, para distinguirlas de un vistazo.
const COLORES_DE_SECCION: Color[] = ['orange', 'blue', 'coral', 'petrol', 'yellow'];

const MODO_DE_LA_URL: Record<'construir' | 'usar', ModoDelEditor> = {
  construir: 'build',
  usar: 'use',
};

// Si «Tus piezas» quedó fijada al costado se recuerda en este navegador; si no se puede leer, flota.
const CLAVE_PIEZAS_FIJAS = 'ensambla.piezasFijas';
function leerPiezasFijas() {
  try {
    return localStorage.getItem(CLAVE_PIEZAS_FIJAS) === 'si';
  } catch {
    return false;
  }
}
function guardarPiezasFijas(fijada: boolean) {
  try {
    localStorage.setItem(CLAVE_PIEZAS_FIJAS, fijada ? 'si' : 'no');
  } catch {
    // Sin almacenamiento el panel igual se fija mientras la página siga abierta.
  }
}

// Construir y Usar un sistema: biblioteca de piezas, lienzo con bloques, secciones e inspector.
export function SistemaPage() {
  const { sistemaId } = ruta.useParams();
  const { modo: modoDeLaUrl } = ruta.useSearch();
  const navigate = useNavigate();
  const navegacion = useNavegacion();

  const existe = useSistemaStore((estado) => estado.sistemas.some((item) => item.id === sistemaId));
  const activoId = useSistemaStore((estado) => estado.activeId);
  const sistema = useSistemaStore(seleccionarSistemaActivo);
  const seccion = useSistemaStore(seleccionarSeccionActiva);
  const bloqueElegido = useSistemaStore(seleccionarBloqueElegido);
  const modo = useSistemaStore((estado) => estado.modo);
  const listo = useSistemaStore((estado) => estado.listo);
  const tablaCompletaId = useSistemaStore((estado) => estado.tablaCompletaId);
  const seleccionadoId = useSistemaStore((estado) => estado.selectedId);
  const hayHistorial = useSistemaStore((estado) => estado.historial.length > 0);
  const guardando = useSistemaStore((estado) => estado.pendientes.length > 0);
  const elegirSeccion = useSistemaStore((estado) => estado.elegirSeccion);
  const seleccionar = useSistemaStore((estado) => estado.seleccionar);
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const [bibliotecaAbierta, setBibliotecaAbierta] = useState(false);
  const [bibliotecaFijada, setBibliotecaFijada] = useState(leerPiezasFijas);
  const arrastre = useArrastreDelEditor(seccion?.id);

  useTituloPagina(sistema?.name ?? 'Mi cuaderno');

  // La dirección manda: abrir el sistema de la URL y seguir el modo que ella diga.
  useEffect(() => {
    if (!existe) return;
    const estado = useSistemaStore.getState();
    if (estado.activeId !== sistemaId) estado.abrirSistema(sistemaId, MODO_DE_LA_URL[modoDeLaUrl]);
    else if (estado.modo !== MODO_DE_LA_URL[modoDeLaUrl]) {
      useSistemaStore.setState({ listo: false });
      estado.cambiarModo(MODO_DE_LA_URL[modoDeLaUrl]);
    }
  }, [existe, sistemaId, modoDeLaUrl]);

  // Un sistema que ya no existe lleva al inicio, como el mockup.
  useEffect(() => {
    if (!existe) void navigate({ to: '/inicio', replace: true });
  }, [existe, navigate]);

  // Atajos: Esc deselecciona o cierra el catálogo del celular; Ctrl/Cmd+Z deshace.
  useEffect(() => {
    const alTeclear = (evento: KeyboardEvent) => {
      if (useSistemaStore.getState().modal) return;
      if (evento.key === 'Escape') {
        if (useSistemaStore.getState().selectedId) seleccionar(null);
        else setBibliotecaAbierta(false);
      }
      if (
        (evento.ctrlKey || evento.metaKey) &&
        evento.key === 'z' &&
        !(evento.target as HTMLElement).closest('input,textarea,select')
      ) {
        evento.preventDefault();
        deshacerCambio();
      }
    };
    document.addEventListener('keydown', alTeclear);
    return () => document.removeEventListener('keydown', alTeclear);
  }, [seleccionar]);

  if (!existe || !sistema || activoId !== sistemaId || !seccion) return null;

  const cambiarModo = (siguiente: ModoDelEditor) => {
    useSistemaStore.setState({ listo: false });
    useSistemaStore.getState().cerrarModal();
    useSistemaStore.getState().cambiarModo(siguiente);
    void navigate({
      to: '/sistemas/$sistemaId',
      params: { sistemaId },
      search: { modo: MODO_EN_LA_URL[siguiente] },
      replace: true,
    });
  };

  const hayEjemplos = allBlocks(sistema).some((bloque) =>
    bloque.rows.some((fila) => fila.__example),
  );
  const tablaCompleta = findBlock(sistema, tablaCompletaId ?? undefined);
  const bloques = seccion.blocks;
  const construyendo = modo === 'build';

  return (
    <>
      <CabeceraComun active="sistemas" />
      <main
        id="main"
        tabIndex={-1}
        className={`editor-layout ${modo === 'use' ? 'using' : ''} ${construyendo && bibliotecaFijada ? 'con-piezas-fijas' : ''}`
          .replace(/\s+/g, ' ')
          .trim()}
      >
        {construyendo && (
          <BibliotecaDeBloques
            abierta={bibliotecaAbierta}
            fijada={bibliotecaFijada}
            onCerrar={() => setBibliotecaAbierta(false)}
            onFijar={(fijada) => {
              setBibliotecaFijada(fijada);
              guardarPiezasFijas(fijada);
              setBibliotecaAbierta(false);
            }}
            onArrastrar={arrastre.alArrastrar}
            onFinArrastre={arrastre.limpiar}
          />
        )}
        <div className="canvas-area" onDragOver={arrastre.alPasarPor} onDrop={arrastre.alSoltar}>
          <EncabezadoDelEditor
            nombre={sistema.name}
            modo={modo}
            guardando={guardando}
            puedeDeshacer={hayHistorial}
            onCuadernos={() => void navegacion.irAMisSistemas()}
            onModo={cambiarModo}
            onDeshacer={deshacerCambio}
          />
          {listo && modo === 'use' && (
            <CierreDeCreacion
              sistema={sistema}
              onRegistrar={(bloqueId) => abrirModal({ tipo: 'registro', bloqueId })}
              onPersonalizar={() => cambiarModo('build')}
              onAjustarConSami={() => void navegacion.ajustarConSami(sistema.id)}
              onCerrar={() => useSistemaStore.getState().descartarCartelListo()}
            />
          )}
          <div className="canvas-heading">
            <div className="canvas-titulo">
              <span className={`canvas-titulo-icono ${construyendo ? '' : 'usando'}`.trim()}>
                <Icon name={construyendo ? 'tools' : 'grid'} />
              </span>
              <div>
                <span className="canvas-etiqueta">
                  {construyendo ? 'Gestión de tablas · Armado' : 'Gestión de tablas · Uso diario'}
                </span>
                <h1>
                  <button
                    type="button"
                    title="Configurar cuaderno"
                    onClick={() => abrirModal({ tipo: 'renombrar' })}
                  >
                    {sistema.name} <Icon name="edit" />
                  </button>
                </h1>
                <p>
                  {construyendo
                    ? 'Sumá tablas, movelas entre secciones y elegí qué datos guarda cada una.'
                    : 'Cargá registros, buscá lo que necesitás y revisá cómo viene tu negocio.'}
                </p>
                <div className="canvas-cifras">
                  <span data-color="orange">
                    <strong>{allBlocks(sistema).length}</strong> tablas
                  </span>
                  <span data-color="blue">
                    <strong>{sistema.sections.length}</strong> secciones
                  </span>
                  <span data-color="coral">
                    <strong>{totals(sistema).records}</strong> registros
                  </span>
                </div>
              </div>
            </div>
            <div className="canvas-heading-actions">
              {construyendo && (
                <Btn
                  cls="secondary mobile-blocks"
                  data-accion="mobile-library"
                  data-tour="agregar-pieza-movil"
                  onClick={() => setBibliotecaAbierta((abierta) => !abierta)}
                >
                  <Icon name="plus" /> Bloques
                </Btn>
              )}
              {construyendo && (
                <Btn cls="secondary" onClick={() => abrirModal({ tipo: 'secciones' })}>
                  <Icon name="folder" /> Secciones
                </Btn>
              )}
              <button
                type="button"
                className="foto-redonda"
                aria-label="Traer datos de una foto"
                title="Traer datos de una foto"
                onClick={() =>
                  abrirModal({
                    tipo: 'importar',
                    sistemaId: sistema.id,
                    bloqueId: null,
                    contexto: 'registros',
                  })
                }
              >
                <Icon name="camera" />
              </button>
              <Btn
                cls="secondary"
                data-accion="ajustar-con-sami"
                onClick={() => void navegacion.ajustarConSami(sistema.id)}
              >
                <Icon name="spark" /> Ajustar con Sami
              </Btn>
            </div>
          </div>
          <div className="tabs-y-distribucion">
            <nav className="section-tabs" aria-label="Secciones del cuaderno">
              {/* <span className="section-tabs-label">Secciones</span> */}
              {sistema.sections.map((otra, indice) => (
                <div
                  key={otra.id}
                  style={varsDeColor(
                    COLORES_DE_SECCION[indice % COLORES_DE_SECCION.length] ?? 'orange',
                  )}
                  data-color={COLORES_DE_SECCION[indice % COLORES_DE_SECCION.length]}
                  className={`section-tab ${otra.id === seccion.id ? 'active' : ''} ${arrastre.sobre?.destino === otra.id ? 'drop-active' : ''}`
                    .replace(/\s+/g, ' ')
                    .trim()}
                  data-drop-section={otra.id}
                >
                  <button
                    type="button"
                    aria-current={otra.id === seccion.id ? 'page' : 'false'}
                    onClick={() => elegirSeccion(otra.id)}
                  >
                    {otra.name}
                    <span className="section-tab-cuenta" aria-hidden="true">
                      {otra.blocks.length}
                    </span>
                  </button>
                </div>
              ))}
              {construyendo && (
                <IconBtn
                  name="plus"
                  label="Agregar una sección"
                  onClick={() => abrirModal({ tipo: 'agregarSeccion' })}
                />
              )}
            </nav>
            {construyendo && (
              <LayoutToolbar
                compacta
                distribucion={sistema.layout}
                onElegir={(distribucion: Distribucion) => aplicarDistribucion(distribucion)}
              />
            )}
          </div>
          {hayEjemplos && (
            <div className="sample-data-banner">
              <Icon name="help" />
              Estás viendo datos de ejemplo. Podés editarlos o cargar los tuyos.
              <button type="button" onClick={pedirQuitarEjemplos}>
                Quitar ejemplos
              </button>
            </div>
          )}
          {!construyendo && <FranjaDeSami sistemaId={sistema.id} />}
          <div
            className={`canvas-grid ${arrastre.sobre?.destino === '#canvas-drop' ? 'drop-active' : ''}`.trim()}
            id="canvas-drop"
            data-tour="lienzo"
            aria-label={`Bloques de ${seccion.name}`}
          >
            {bloques.length ? (
              bloques.map((bloque, indice) => (
                <BloqueEnLienzo
                  key={bloque.id}
                  bloque={bloque}
                  sistema={sistema}
                  modo={modo}
                  seleccionado={seleccionadoId === bloque.id}
                  primero={indice === 0}
                  ultimo={indice === bloques.length - 1}
                  arrastrando={
                    arrastre.carga?.tipo === 'bloque' && arrastre.carga.valor === bloque.id
                  }
                  sobre={arrastre.sobre?.bloque === bloque.id}
                  ladoDelCambio={
                    arrastre.sobre?.bloque === bloque.id ? arrastre.sobre.lado : undefined
                  }
                  anchoProvisorio={
                    arrastre.ajuste?.id === bloque.id ? arrastre.ajuste.span : undefined
                  }
                  onArrastrar={arrastre.alArrastrar}
                  onFinArrastre={arrastre.limpiar}
                  onTirador={arrastre.alTirador}
                />
              ))
            ) : (
              <div className="canvas-empty">
                <div className="empty-blocks">
                  <IconTile tipo="inventory" />
                  <IconTile tipo="orders" />
                  <IconTile tipo="clients" />
                </div>
                <h2>Este espacio lo armás vos.</h2>
                <p>
                  {construyendo
                    ? 'Traé un bloque desde la izquierda o elegí uno acá. Los campos ya vienen preparados.'
                    : 'Esta sección todavía no tiene bloques. Agregá uno para empezar a registrar.'}
                </p>
                <div className="row">
                  {construyendo ? (
                    (['inventory', 'orders', 'clients'] as TipoBloque[]).map((tipo) => (
                      <Btn
                        key={tipo}
                        cls="secondary"
                        data-accion="add-block"
                        data-tour="agregar-pieza-vacio"
                        onClick={() => agregarBloque(tipo)}
                      >
                        <Icon name={CATALOG[tipo].icon} /> {CATALOG[tipo].name}
                      </Btn>
                    ))
                  ) : (
                    <Btn cls="primary" data-accion="mode" onClick={() => cambiarModo('build')}>
                      Agregar mi primer bloque
                    </Btn>
                  )}
                </div>
              </div>
            )}
          </div>
          {construyendo && bloques.length > 0 && (
            <>
              <button
                type="button"
                className="add-block-end"
                onClick={() => abrirModal({ tipo: 'elegirBloque' })}
              >
                <Icon name="plus" />
                Agregar otro bloque
              </button>
              <p className="builder-note">
                <Icon name="grip" />
                Arrastrá una tabla sobre otra para cambiarlas de lugar. También podés usar las
                flechas.
              </p>
            </>
          )}
        </div>
        {construyendo && bloqueElegido && <Inspector bloque={bloqueElegido} sistema={sistema} />}
      </main>
      <button
        type="button"
        className="sami-float"
        aria-label="¿Una mano con esto?"
        title="¿Una mano con esto?"
        onClick={() => abrirModal({ tipo: 'ayuda' })}
      >
        <img className="sami-float-img" src="/mockup/sami-boton.webp" alt="" />
      </button>
      {tablaCompleta && (
        <TablaCompleta key={tablaCompleta.id} bloque={tablaCompleta} sistema={sistema} />
      )}
    </>
  );
}
