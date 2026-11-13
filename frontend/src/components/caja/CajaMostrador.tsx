import { useEffect, useRef, useState } from 'react';
import { useCaja } from '@/hooks/caja/useCaja';
import { useCamaraCodigo } from '@/hooks/escaneo/useCamaraCodigo';
import { LectorDeCodigos } from '@/components/escaneo/LectorDeCodigos';
import { AtribucionCatalogos } from '@/components/escaneo/AtribucionCatalogos';
import { Icon } from '@/components/sistema/Icon';
import { ArticuloCajaForm } from './ArticuloCajaForm';
import { CompraActual } from './CompraActual';
import { CobroCaja } from './CobroCaja';
import { DialogoCaja } from './DialogoCaja';
import { GestionCaja } from './GestionCaja';
import { dineroCaja, modoVenta } from '@/lib/caja';
import { normalize } from '@/lib/sistema/core';

export function CajaMostrador({
  sistemaId,
  inventarioId,
  ventasId,
}: {
  sistemaId: string;
  inventarioId: string;
  ventasId: string;
}) {
  const caja = useCaja(sistemaId, inventarioId, ventasId);
  const [consulta, setConsulta] = useState('');
  const [vista, setVista] = useState<'cobrar' | 'dia' | 'abierta' | 'cierres'>('cobrar');
  const [favoritos, setFavoritos] = useState(false);
  const [cobrando, setCobrando] = useState(!!caja.solicitud);
  const entrada = useRef<HTMLInputElement>(null);
  const camara = useCamaraCodigo((codigo) => {
    void caja.buscar(codigo);
  }, true);
  const bloqueada = !!caja.seleccion || cobrando || favoritos || !!caja.ticket || !!caja.solicitud;
  const cerrarCamara = camara.cerrar;
  useEffect(() => {
    if (bloqueada || vista !== 'cobrar') cerrarCamara();
  }, [bloqueada, cerrarCamara, vista]);
  useEffect(() => {
    if (!bloqueada) entrada.current?.focus();
  }, [bloqueada, caja.lineas.length]);
  const mostrados = caja.productos.filter((p) =>
    consulta
      ? normalize(`${p.name} ${p.barcode ?? ''}`).includes(normalize(consulta))
      : p.favorite === true,
  );
  return (
    <>
      <div className="caja-contexto">
        <span>
          <Icon name="box" />
          {caja.inventario?.name}
        </span>
        <span>{caja.pendiente ? 'Guardando cambios…' : 'Inventario conectado'}</span>
      </div>
      <nav className="caja-vistas" aria-label="Secciones de caja">
        {(
          [
            ['cobrar', 'Cobrar'],
            ['dia', 'Resumen del día'],
            ['abierta', 'Caja abierta'],
            ['cierres', 'Cierres guardados'],
          ] as const
        ).map(([id, nombre]) => (
          <button
            type="button"
            key={id}
            aria-current={vista === id ? 'page' : undefined}
            disabled={bloqueada}
            onClick={() => setVista(id)}
          >
            {nombre}
          </button>
        ))}
      </nav>
      {vista !== 'cobrar' ? (
        <GestionCaja
          sistemaId={sistemaId}
          inventarioId={inventarioId}
          vista={vista}
          compraPendiente={caja.lineas.length > 0}
        />
      ) : (
        <>
          {caja.error && !bloqueada && (
            <p className="caja-error" role="alert">
              {caja.error}
            </p>
          )}
          {caja.mensaje && (
            <p className="caja-mensaje" role="status">
              {caja.mensaje}
            </p>
          )}
          <div className="caja-distribucion">
            <div className="caja-entrada">
              <section className="caja-lector">
                <div className="caja-entre">
                  <div>
                    <span className="caja-antetitulo">AGREGAR A LA COMPRA</span>
                    <h2>Escaneá y seguí</h2>
                  </div>
                  <Icon name="camera" />
                </div>
                <div className="caja-proxima">
                  <label>
                    Próxima lectura
                    <input
                      aria-label="Cantidad para la próxima lectura"
                      inputMode="numeric"
                      value={caja.cantidad}
                      disabled={bloqueada}
                      onChange={(e) => caja.setCantidad(e.target.value)}
                    />
                  </label>
                  <div className="caja-opciones">
                    {[1, 2, 3].map((n) => (
                      <button
                        key={n}
                        type="button"
                        aria-pressed={caja.cantidad === String(n)}
                        disabled={bloqueada}
                        onClick={() => caja.setCantidad(String(n))}
                      >
                        ×{n}
                      </button>
                    ))}
                  </div>
                </div>
                <p className="caja-ayuda">
                  La cantidad vuelve a uno después de agregar. Podés corregirla en la compra.
                </p>
                <LectorDeCodigos
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
                />
              </section>
              <section className="caja-productos" aria-label="Productos y favoritos">
                <div className="caja-entre">
                  <h2>{consulta ? 'Resultados' : 'Tus favoritos'}</h2>
                  <button
                    type="button"
                    className="btn soft small"
                    disabled={bloqueada}
                    onClick={() => setFavoritos(true)}
                  >
                    <Icon name="plus" />
                    Favoritos
                  </button>
                </div>
                <label className="caja-buscador">
                  <Icon name="search" />
                  <input
                    aria-label="Buscar artículo en el inventario"
                    placeholder="Buscar un artículo por nombre…"
                    value={consulta}
                    onChange={(e) => setConsulta(e.target.value)}
                    disabled={bloqueada}
                  />
                </label>
                <div className="caja-favoritos">
                  {mostrados.map((producto) => (
                    <div key={producto.id} className="caja-tarjeta-producto">
                      <button
                        type="button"
                        className="caja-elegir-producto"
                        onClick={() => caja.elegir(producto)}
                        disabled={bloqueada}
                      >
                        <span
                          className={`caja-simbolo ${modoVenta(producto) === 'peso' ? 'peso' : ''}`}
                        >
                          <Icon name={modoVenta(producto) === 'peso' ? 'grid' : 'box'} />
                        </span>
                        <strong>{String(producto.name)}</strong>
                        <span>
                          {Number(producto.price) > 0
                            ? dineroCaja(Math.round(Number(producto.price) * 100))
                            : 'Completar precio'}{' '}
                          / {modoVenta(producto) === 'peso' ? 'kg' : 'unidad'}
                        </span>
                      </button>
                      <div className="caja-tarjeta-acciones">
                        <button
                          type="button"
                          className="icon-btn"
                          disabled={bloqueada}
                          aria-label={`${producto.favorite === true ? 'Quitar de' : 'Agregar a'} favoritos: ${String(producto.name)}`}
                          onClick={() => caja.favorito(producto)}
                        >
                          <Icon name={producto.favorite === true ? 'check' : 'plus'} />
                        </button>
                        <button
                          type="button"
                          className="icon-btn"
                          disabled={bloqueada}
                          aria-label={`Editar artículo ${String(producto.name)}`}
                          onClick={() => caja.editar(producto)}
                        >
                          <Icon name="edit" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
                {!mostrados.length && (
                  <div className="caja-vacio">
                    <Icon name="box" />
                    <h3>
                      {consulta
                        ? 'No encontramos ese artículo'
                        : 'Lo de todos los días, a un toque'}
                    </h3>
                    <p>
                      {consulta
                        ? 'Podés cargarlo y seguir con la compra.'
                        : 'Agregá tus productos más vendidos desde el inventario o creá uno nuevo.'}
                    </p>
                  </div>
                )}
                <button
                  type="button"
                  className="btn secondary"
                  disabled={bloqueada}
                  onClick={() => caja.nuevo()}
                >
                  <Icon name="plus" />
                  Agregar artículo sin código
                </button>
              </section>
              <AtribucionCatalogos />
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
            />
          </div>
        </>
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
          total={caja.total}
          ocupada={caja.ocupada}
          incierta={!!caja.solicitud && !caja.ocupada}
          error={caja.error}
          onConfirmar={(medio, recibido, pagos) => void caja.confirmar(medio, recibido, pagos)}
          onCerrar={() => setCobrando(false)}
        />
      )}
      {caja.ticket && (
        <DialogoCaja
          titulo="Venta registrada"
          onCerrar={() => {
            caja.setTicket(null);
            setCobrando(false);
          }}
        >
          <div className="caja-confirmada">
            <Icon name="check" />
            <strong>{dineroCaja(Math.round(caja.ticket.total * 100))}</strong>
            <p>La venta y el cobro quedaron guardados.</p>
          </div>
          <ul className="caja-resumen">
            {caja.ticket.lineas.map((linea, indice) => (
              <li key={indice}>
                <span>
                  {linea.nombre} ·{' '}
                  {linea.modo === 'PESO'
                    ? `${Math.round(linea.cantidad * 1000)} g`
                    : `${linea.cantidad} u.`}
                </span>
                <strong>{dineroCaja(Math.round(linea.importe * 100))}</strong>
              </li>
            ))}
          </ul>
          <p>Vuelto: {dineroCaja(Math.round(caja.ticket.vuelto * 100))}</p>
          <p className="caja-ayuda">Operación {caja.ticket.id} · Comprobante de registro</p>
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
          <p>Son accesos a los mismos artículos de tu inventario.</p>
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
    </>
  );
}
