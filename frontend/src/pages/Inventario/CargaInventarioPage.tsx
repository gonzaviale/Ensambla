import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { LectorDeCodigos } from '@/components/escaneo/LectorDeCodigos';
import { ProductoDetectado } from '@/components/escaneo/ProductoDetectado';
import { ProductoCargado } from '@/components/escaneo/ProductoCargado';
import { FuentesDelProducto } from '@/components/escaneo/FuentesDelProducto';
import { FormularioProductoInventario } from '@/components/escaneo/FormularioProductoInventario';
import { ReposicionProductoInventario } from '@/components/escaneo/ReposicionProductoInventario';
import { AtribucionCatalogos } from '@/components/escaneo/AtribucionCatalogos';
import { useCargaInventario } from '@/hooks/escaneo/useCargaInventario';
import { useCamaraCodigo } from '@/hooks/escaneo/useCamaraCodigo';
import { useDetalleCatalogos } from '@/hooks/escaneo/useDetalleCatalogos';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';

export function CargaInventarioPage({
  sistemaId,
  inventarioId,
}: {
  sistemaId: string;
  inventarioId: string;
}) {
  const carga = useCargaInventario(sistemaId, inventarioId);
  const camara = useCamaraCodigo((codigo) => {
    void carga.buscar(codigo, 'camara');
  });
  const detalle = useDetalleCatalogos(carga.resultado);
  const navegacion = useNavegacion();
  useTituloPagina('Cargar productos al inventario');
  return (
    <>
      <CabeceraComun active="sistemas" />
      <main id="main" tabIndex={-1} className="escaneo-pagina">
        <nav className="escaneo-migas" aria-label="Ubicación en el inventario">
          <button type="button" onClick={() => void navegacion.irAMisSistemas()}>
            Mis cuadernos
          </button>
          <span aria-hidden="true">/</span>
          <button type="button" onClick={() => void navegacion.abrirSistema(sistemaId)}>
            {carga.sistema?.name}
          </button>
          <span aria-hidden="true">/</span>
          <span>{carga.inventario?.name ?? 'Inventario'}</span>
        </nav>
        <header className="escaneo-cabecera">
          <div>
            <span className="escaneo-etiqueta">
              CARGA DE PRODUCTOS · {carga.inventario?.name ?? 'INVENTARIO'}
            </span>
            <h1>Escaneá, revisá y agregá.</h1>
            <p>
              Los productos se guardan en el inventario de este negocio cuando confirmás la carga.
            </p>
          </div>
          <Btn
            cls="secondary"
            onClick={() => void navegacion.volverAlInventario(sistemaId, inventarioId)}
          >
            <Icon name="back" /> Volver al inventario
          </Btn>
        </header>
        {!carga.sistema || carga.inventario?.type !== 'inventory' ? (
          <p className="escaneo-error" role="alert">
            Este inventario ya no está disponible. Volvé al negocio para elegir una tabla de
            Inventario.
          </p>
        ) : (
          <>
            <p className="escaneo-sincronizacion" role="status">
              <Icon name={carga.pendiente ? 'clock' : 'check-circle'} />
              {carga.pendiente ? 'Guardando los cambios del negocio…' : 'Negocio actualizado'}
            </p>
            {carga.ultimo && !carga.errores._form && (
              <div ref={carga.confirmacionCarga} className="escaneo-confirmacion-carga">
                <ProductoCargado
                  producto={carga.ultimo}
                  pendiente={carga.pendiente}
                  inventario={carga.inventario.name}
                  agregadas={carga.unidadesAgregadas}
                />
              </div>
            )}
            <div
              className={`escaneo-distribucion ${carga.borrador && !camara.abierta && !camara.error ? 'escaneo-con-producto' : ''}`}
            >
              {!carga.borrador && (
                <div className="escaneo-entrada">
                  <LectorDeCodigos
                    codigo={carga.codigo}
                    buscando={carga.buscando}
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
                    entradaCodigo={carga.entradaCodigo}
                    onCodigo={carga.setCodigo}
                    onBuscar={() => {
                      void carga.buscar(carga.codigo);
                    }}
                    onAbrir={camara.abrir}
                    onCerrar={camara.cerrar}
                    onReiniciar={camara.reiniciar}
                    onCamara={camara.seleccionarCamara}
                  />
                </div>
              )}
              <div className="escaneo-revision" ref={carga.revisionProducto}>
                {carga.borrador && (
                  <div className="escaneo-acciones-revision">
                    <span>Producto leído · Revisá y confirmá</span>
                    <button type="button" className="btn secondary" onClick={carga.otraLectura}>
                      <Icon name="camera" /> Escanear otro producto
                    </button>
                  </div>
                )}
                {carga.borrador && (
                  <ProductoDetectado
                    resultado={carga.resultado}
                    sugerencia={carga.sugerencia}
                    nombre={String(carga.borrador.valores.name || '')}
                    existente={!!carga.borrador.productoId}
                    buscando={carga.buscando}
                    consultandoReferencia={carga.consultandoReferencia}
                    onFuentes={detalle.abrir}
                  />
                )}
                {carga.mensaje && !carga.ultimo && (
                  <div
                    className={`escaneo-aviso ${carga.borrador?.productoId ? 'escaneo-aviso-existente' : ''}`}
                    role="status"
                  >
                    <Icon name={carga.borrador?.productoId ? 'box' : 'undo'} />
                    <div>
                      <strong>
                        {carga.borrador?.productoId
                          ? 'Este producto ya está cargado'
                          : 'Carga descartada'}
                      </strong>
                      <p>{carga.mensaje}</p>
                    </div>
                  </div>
                )}
                {carga.errores._form && (
                  <div className="escaneo-aviso escaneo-aviso-error" role="alert">
                    <Icon name="help" />
                    <div>
                      <strong>Revisá los datos para continuar</strong>
                      <p>{carga.errores._form}</p>
                    </div>
                  </div>
                )}
                {carga.borrador ? (
                  carga.esReposicion ? (
                    <ReposicionProductoInventario
                      stockActual={Number(carga.productoActual?.quantity ?? 0)}
                      cantidad={carga.cantidadAgregar}
                      error={carga.errores.quantity}
                      formulario={carga.formulario}
                      onCantidad={carga.cambiarCantidadAgregar}
                      onAgregar={carga.guardar}
                      onDescartar={carga.cancelar}
                    />
                  ) : (
                    <FormularioProductoInventario
                      borrador={carga.borrador}
                      sistema={carga.sistema}
                      inventario={carga.inventario}
                      grupos={carga.grupos}
                      productosVinculables={carga.productosVinculables}
                      formulario={carga.formulario}
                      errores={carga.errores}
                      bloqueado={carga.buscando || camara.abierta}
                      onCambiar={carga.cambiar}
                      onElegir={carga.elegirExistente}
                      onGuardar={carga.guardar}
                      onCancelar={() => {
                        camara.cerrar();
                        carga.cancelar();
                      }}
                    />
                  )
                ) : (
                  <section className="escaneo-panel escaneo-vacio">
                    <Icon name="box" />
                    <h2>Un producto por vez, con tus datos</h2>
                    <p>
                      Leé el código con la cámara, un lector o ingresalo a mano. Completamos la
                      descripción y vos revisás la cantidad, el costo y el precio.
                    </p>
                    <p>
                      Si ya existe, lo reconocemos para evitar duplicados. Si no está en los
                      catálogos, podés completarlo a mano.
                    </p>
                  </section>
                )}
              </div>
            </div>
            <footer className="escaneo-atribucion">
              <AtribucionCatalogos />
            </footer>
          </>
        )}
      </main>
      {carga.resultado && carga.sugerencia && (
        <FuentesDelProducto
          resultado={carga.resultado}
          sugerencia={carga.sugerencia}
          dialogo={detalle.dialogo}
          onCerrar={detalle.cerrar}
        />
      )}
    </>
  );
}
