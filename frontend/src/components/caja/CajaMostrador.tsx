import { ImporteLibreCaja } from './ImporteLibreCaja';
import { detalleLineaVendida } from '@/lib/caja/importeLibre';
import { useEffect, useRef, useState } from 'react';
import { useCaja } from '@/hooks/caja/useCaja';
import { useCamaraCodigo } from '@/hooks/escaneo/useCamaraCodigo';
import { useSonidosEscaneo } from '@/hooks/escaneo/useSonidosEscaneo';
import { LectorCaja } from './LectorCaja';
import { Icon } from '@/components/sistema/Icon';
import { ArticuloCajaForm } from './ArticuloCajaForm';
import { CompraActual } from './CompraActual';
import { CobroCaja } from './CobroCaja';
import { DialogoCaja } from './DialogoCaja';
import { GestionCaja } from './GestionCaja';
import { ClientesCaja } from './ClientesCaja';
import { ProductosCaja } from './ProductosCaja';
import { BarraCompraMovil } from './BarraCompraMovil';
import { dineroCaja } from '@/lib/caja';
import { normalize } from '@/lib/sistema/core';
import { claseDeSeleccion, tonoDe } from '@/lib/sistema/tonos';

const MODOS_DE_ENTRADA = [
  ['escanear', 'Escanear', 'camera', 'venderEscanear'],
  ['buscar', 'Buscar', 'search', 'venderBuscar'],
  ['favoritos', 'Favoritos', 'grid', 'venderFavoritos'],
] as const;

const VISTAS_DE_ACTIVIDAD = [
  ['dia', 'Resumen del día', 'chart', 'pestanaResumenDelDia'],
  ['cobros', 'Cobros', 'wallet', 'pestanaCobros'],
  ['abierta', 'Caja abierta', 'wallet', 'pestanaCajaAbierta'],
  ['cierres', 'Cierres guardados', 'folder', 'pestanaCierresGuardados'],
] as const;

export function CajaMostrador({
  sistemaId,
  inventarioId,
  ventasId,
  bloqueoExterno = false,
}: {
  sistemaId: string;
  inventarioId: string;
  ventasId: string;
  bloqueoExterno?: boolean;
}) {
  const caja = useCaja(sistemaId, inventarioId, ventasId);
  const { preparar: prepararAudio } = useSonidosEscaneo();
  const [consulta, setConsulta] = useState('');
  const [importeLibre, setImporteLibre] = useState(false);
  const [vista, setVista] = useState<
    'cobrar' | 'dia' | 'cobros' | 'abierta' | 'cierres' | 'clientes'
  >('cobrar');
  const [favoritos, setFavoritos] = useState(false);
  const [cobrando, setCobrando] = useState(!!caja.solicitud);
  const [entradaElegida, setEntradaElegida] = useState<'escanear' | 'buscar' | 'favoritos'>(
    'escanear',
  );
  const [paso, setPaso] = useState<'agregar' | 'compra'>('agregar');
  const entrada = useRef<HTMLInputElement>(null);
  const camara = useCamaraCodigo((codigo) => {
    void caja.buscar(codigo);
  }, true);
  const bloqueada =
    bloqueoExterno ||
    importeLibre ||
    !!caja.seleccion ||
    cobrando ||
    favoritos ||
    !!caja.ticket ||
    !!caja.solicitud;
  const cerrarCamara = camara.cerrar;
  useEffect(() => {
    if (bloqueada || vista !== 'cobrar') cerrarCamara();
  }, [bloqueada, cerrarCamara, vista]);
  const cambiarPaso = (nuevo: 'agregar' | 'compra') => {
    setPaso(nuevo);
    if (nuevo === 'compra') cerrarCamara();
  };
  const cambiarEntrada = (nueva: typeof entradaElegida) => {
    setEntradaElegida(nueva);
    if (nueva !== 'escanear') cerrarCamara();
    setConsulta('');
  };
  const volverAEscanear = () => {
    prepararAudio();
    cambiarPaso('agregar');
    cambiarEntrada('escanear');
  };
  return (
    <div
      className="caja-mostrador"
      data-paso={paso}
      data-vista={vista}
      data-entrada={entradaElegida}
    >
      <div className="caja-contexto">
        <span>
          <Icon name="box" />
          {caja.inventario?.name}
        </span>
        <span className="caja-conexion">
          <span aria-hidden="true" />
          {caja.pendiente ? 'Guardando cambios…' : 'Stock conectado'}
        </span>
      </div>
      <nav className="caja-vistas" aria-label="Secciones de caja">
        <button
          type="button"
          aria-current={vista === 'cobrar' ? 'page' : undefined}
          disabled={bloqueada}
          onClick={() => setVista('cobrar')}
        >
          <Icon name="orders" />
          Vender
        </button>
        <button
          type="button"
          aria-current={vista !== 'cobrar' && vista !== 'clientes' ? 'page' : undefined}
          disabled={bloqueada}
          onClick={() => setVista(vista === 'cobrar' || vista === 'clientes' ? 'dia' : vista)}
        >
          <Icon name="chart" />
          Actividad
        </button>
        <button
          type="button"
          aria-current={vista === 'clientes' ? 'page' : undefined}
          disabled={bloqueada}
          onClick={() => setVista('clientes')}
        >
          <Icon name="users" />
          Clientes
        </button>
      </nav>
      {vista === 'clientes' ? (
        <ClientesCaja sistemaId={sistemaId} inventarioId={inventarioId} />
      ) : vista !== 'cobrar' ? (
        <>
          <nav className="caja-actividad-vistas" aria-label="Consultar actividad">
            {VISTAS_DE_ACTIVIDAD.map(([id, nombre, icono, concepto]) => (
              <button
                key={id}
                type="button"
                className={claseDeSeleccion(vista === id)}
                data-tono={tonoDe(concepto)}
                aria-current={vista === id ? 'page' : undefined}
                disabled={bloqueada}
                onClick={() => setVista(id)}
              >
                <Icon name={icono} />
                {nombre}
              </button>
            ))}
          </nav>
          <GestionCaja
            sistemaId={sistemaId}
            inventarioId={inventarioId}
            vista={vista}
            compraPendiente={caja.lineas.length > 0}
          />
        </>
      ) : (
        <>
          <div className="caja-venta-panel">
            <nav className="caja-pasos" aria-label="Pasos de la venta">
              <button
                type="button"
                aria-current={paso === 'agregar' ? 'step' : undefined}
                disabled={bloqueada}
                onClick={() => cambiarPaso('agregar')}
              >
                <span>1</span>Agregar
              </button>
              <button
                type="button"
                aria-current={paso === 'compra' ? 'step' : undefined}
                disabled={bloqueada}
                onClick={() => cambiarPaso('compra')}
              >
                <span>2</span>Compra <small>{caja.lineas.length}</small>
              </button>
              <button
                type="button"
                disabled={bloqueada || !caja.lineas.length || caja.pendiente}
                onClick={() => setCobrando(true)}
              >
                <span>3</span>Cobrar
              </button>
            </nav>
            {caja.error && !bloqueada && (
              <p className="caja-error" role="alert">
                {caja.error}
              </p>
            )}
            {caja.mensaje && (
              <div className="caja-mensaje" role="status">
                <Icon name="check-circle" />
                <span>{caja.mensaje}</span>
              </div>
            )}
            <div className="caja-distribucion">
              <div className="caja-entrada">
                <section className="caja-lector">
                  <div className="caja-entre">
                    <div>
                      <span className="caja-antetitulo">01 · NUEVA VENTA</span>
                      <h2>Agregar productos</h2>
                    </div>
                    <Icon name="camera" />
                  </div>
                  <div
                    className="caja-modos"
                    role="tablist"
                    aria-label="Cómo agregar productos"
                    onKeyDown={(evento) => {
                      const opciones = ['escanear', 'buscar', 'favoritos'] as const;
                      const indice = opciones.indexOf(entradaElegida);
                      const siguiente =
                        evento.key === 'ArrowRight'
                          ? (indice + 1) % 3
                          : evento.key === 'ArrowLeft'
                            ? (indice + 2) % 3
                            : evento.key === 'Home'
                              ? 0
                              : evento.key === 'End'
                                ? 2
                                : -1;
                      const opcion = opciones[siguiente];
                      if (!opcion || bloqueada) return;
                      evento.preventDefault();
                      cambiarEntrada(opcion);
                      evento.currentTarget
                        .querySelector<HTMLButtonElement>(`#caja-modo-${opcion}`)
                        ?.focus();
                    }}
                  >
                    {MODOS_DE_ENTRADA.map(([id, nombre, icono, concepto]) => (
                      <button
                        key={id}
                        id={`caja-modo-${id}`}
                        type="button"
                        className={claseDeSeleccion(entradaElegida === id)}
                        data-tono={tonoDe(concepto)}
                        role="tab"
                        tabIndex={entradaElegida === id ? 0 : -1}
                        aria-selected={entradaElegida === id}
                        aria-controls={`caja-panel-${id}`}
                        disabled={bloqueada}
                        onClick={() => cambiarEntrada(id)}
                      >
                        <Icon name={icono} />
                        {nombre}
                      </button>
                    ))}
                  </div>
                  <div
                    id="caja-panel-escanear"
                    role="tabpanel"
                    aria-labelledby="caja-modo-escanear"
                    hidden={entradaElegida !== 'escanear'}
                  >
                    <div className="caja-proxima">
                      <div className="caja-proxima-resumen" aria-live="polite">
                        <span>Próxima lectura</span>
                        <strong>
                          {caja.cantidad} {caja.cantidad === '1' ? 'unidad' : 'unidades'}
                        </strong>
                      </div>
                      <div className="caja-opciones">
                        {[2, 3, 5].map((n) => (
                          <button
                            key={n}
                            type="button"
                            aria-pressed={caja.cantidad === String(n)}
                            disabled={bloqueada}
                            onClick={() => {
                              prepararAudio();
                              caja.setCantidad(caja.cantidad === String(n) ? '1' : String(n));
                            }}
                          >
                            ×{n}
                          </button>
                        ))}
                      </div>
                    </div>
                    <LectorCaja
                      habilitado={
                        vista === 'cobrar' &&
                        paso === 'agregar' &&
                        entradaElegida === 'escanear' &&
                        !bloqueada
                      }
                      lectura={caja.lectura}
                      codigo={caja.codigo}
                      buscando={caja.buscando || bloqueada}
                      abierta={camara.abierta}
                      iniciando={camara.iniciando}
                      sinLectura={camara.sinLectura}
                      sesion={camara.sesion}
                      camaras={camara.camaras}
                      camaraId={camara.camaraId}
                      camaraActiva={camara.camaraActiva}
                      camaraActualId={camara.camaraActualId}
                      errorCamara={camara.error}
                      video={camara.video}
                      entradaCodigo={entrada}
                      onCodigo={caja.setCodigo}
                      onBuscar={() => void caja.buscar(caja.codigo)}
                      onAbrir={camara.abrir}
                      onCerrar={camara.cerrar}
                      onReiniciar={camara.reiniciar}
                      onCamara={camara.seleccionarCamara}
                      continua
                      compacto
                      revisandoProducto={!!caja.seleccion}
                      errorCodigo={caja.error}
                      mensaje={caja.mensaje}
                    />
                  </div>
                  {entradaElegida !== 'escanear' && (
                    <div
                      id={`caja-panel-${entradaElegida}`}
                      role="tabpanel"
                      aria-labelledby={`caja-modo-${entradaElegida}`}
                    >
                      <ProductosCaja
                        productos={caja.productos}
                        consulta={consulta}
                        favoritos={entradaElegida === 'favoritos'}
                        bloqueada={bloqueada}
                        onConsulta={setConsulta}
                        onElegir={caja.elegir}
                        onFavorito={caja.favorito}
                        onEditar={caja.editar}
                        onGestionar={() => setFavoritos(true)}
                      />
                    </div>
                  )}
                  <button
                    type="button"
                    className="btn secondary caja-articulo-manual"
                    disabled={bloqueada}
                    onClick={() => caja.nuevo()}
                  >
                    <Icon name="plus" />
                    Agregar artículo sin código
                  </button>
                  <button
                    type="button"
                    className="btn secondary caja-articulo-manual"
                    disabled={bloqueada}
                    onClick={() => setImporteLibre(true)}
                  >
                    Agregar importe
                  </button>
                </section>
              </div>
              <CompraActual
                lineas={caja.lineas}
                total={caja.total}
                bloqueada={bloqueada}
                pendiente={caja.pendiente}
                onAjustar={caja.ajustar}
                onQuitar={caja.quitar}
                onCobrar={() => setCobrando(true)}
                onDeshacer={caja.deshacer}
                puedeDeshacer={!!caja.anterior}
                onAgregar={() => cambiarPaso('agregar')}
              />
            </div>
            <BarraCompraMovil
              total={caja.total}
              articulos={caja.lineas.length}
              revisando={paso === 'compra'}
              bloqueada={bloqueada}
              pendiente={caja.pendiente}
              onRevisar={() => cambiarPaso('compra')}
              onCobrar={() => setCobrando(true)}
              onEscanear={volverAEscanear}
            />
          </div>
        </>
      )}
      {importeLibre && (
        <ImporteLibreCaja onAgregar={caja.agregarImporte} onCerrar={() => setImporteLibre(false)} />
      )}
      {caja.seleccion && caja.inventario && (
        <ArticuloCajaForm
          seleccion={caja.seleccion}
          inventario={caja.inventario}
          error={caja.error}
          buscando={caja.buscando}
          onGuardar={caja.registrar}
          onCerrar={caja.cerrar}
        />
      )}
      {(cobrando || caja.solicitud) && !caja.ticket && (
        <CobroCaja
          sistemaId={sistemaId}
          ventasId={ventasId}
          total={caja.total}
          ocupada={caja.ocupada}
          incierta={!!caja.solicitud && !caja.ocupada}
          error={caja.error}
          onConfirmar={(medio, recibido, pagos, clienteId, cobroQrId) =>
            void caja.confirmar(medio, recibido, pagos, clienteId, cobroQrId)
          }
          onCerrar={() => setCobrando(false)}
        />
      )}
      {caja.ticket && (
        <DialogoCaja
          titulo="Venta guardada"
          onCerrar={() => {
            caja.setTicket(null);
            setCobrando(false);
          }}
        >
          <div className="caja-confirmada">
            <Icon name="check" />
            <strong>{dineroCaja(Math.round(caja.ticket.total * 100))}</strong>
            <p>
              {(caja.ticket.saldo ?? 0) > 0
                ? 'La venta y el saldo pendiente quedaron guardados.'
                : 'La venta y el cobro quedaron guardados.'}
            </p>
            {caja.ticket.clienteNombre && <p>Cliente: {caja.ticket.clienteNombre}</p>}
            {(caja.ticket.saldo ?? 0) > 0 && (
              <p>
                Pendiente: <strong>{dineroCaja(Math.round((caja.ticket.saldo ?? 0) * 100))}</strong>
              </p>
            )}
          </div>
          <ul className="caja-resumen">
            {caja.ticket.lineas.map((linea, indice) => (
              <li key={indice}>
                <span>
                  {linea.nombre} · {detalleLineaVendida(linea)}
                </span>
                <strong>{dineroCaja(Math.round(linea.importe * 100))}</strong>
              </li>
            ))}
          </ul>
          <p>Vuelto: {dineroCaja(Math.round(caja.ticket.vuelto * 100))}</p>
          <p className="caja-ayuda">Operación {caja.ticket.id} · Comprobante de la venta</p>
          <div className="caja-pagos-detalle">
            {caja.ticket.pagos.map((p) => (
              <p key={p.medio}>
                <span>
                  {p.medio === 'EFECTIVO'
                    ? 'Efectivo'
                    : p.medio === 'TARJETA'
                      ? 'Tarjeta'
                      : 'Transferencia'}
                </span>
                <strong>{dineroCaja(Math.round(p.importe * 100))}</strong>
              </p>
            ))}
          </div>
          <button
            type="button"
            className="btn primary full"
            onClick={() => {
              caja.setTicket(null);
              setCobrando(false);
            }}
          >
            Nueva venta
          </button>
        </DialogoCaja>
      )}
      {favoritos && (
        <DialogoCaja titulo="Elegí tus favoritos" onCerrar={() => setFavoritos(false)}>
          <p>Son accesos a los mismos productos de tu stock.</p>
          <label>
            Buscar artículo
            <input
              value={consulta}
              onChange={(e) => setConsulta(e.target.value)}
              placeholder="Nombre del artículo"
            />
          </label>
          <div className="caja-elegir-favoritos">
            {caja.productos
              .filter((p) => normalize(p.name).includes(normalize(consulta)))
              .map((p) => (
                <button
                  type="button"
                  key={p.id}
                  aria-pressed={p.favorite === true}
                  onClick={() => caja.favorito(p)}
                >
                  <span>{String(p.name)}</span>
                  <Icon name={p.favorite === true ? 'check' : 'plus'} />
                </button>
              ))}
          </div>
          <div className="caja-acciones">
            <button
              type="button"
              className="btn secondary"
              onClick={() => {
                setFavoritos(false);
                caja.nuevo(true);
              }}
            >
              Crear artículo favorito
            </button>
            <button
              type="button"
              className="btn primary"
              onClick={() => {
                setFavoritos(false);
                setConsulta('');
              }}
            >
              Listo
            </button>
          </div>
        </DialogoCaja>
      )}
    </div>
  );
}
