import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { LectorDeCodigos } from '@/components/escaneo/LectorDeCodigos';
import { ProductoDetectado } from '@/components/escaneo/ProductoDetectado';
import { FuentesDelProducto } from '@/components/escaneo/FuentesDelProducto';
import { FormularioProductoInventario } from '@/components/escaneo/FormularioProductoInventario';
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
            <div className="escaneo-distribucion">
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
                {carga.resultado && carga.sugerencia && (
                  <ProductoDetectado
                    resultado={carga.resultado}
                    sugerencia={carga.sugerencia}
                    onFuentes={detalle.abrir}
                  />
                )}
                {carga.ultimo && (
                  <section className="escaneo-ultimo" aria-label="Último producto guardado">
                    <Icon name="check-circle" />
                    <div>
                      <strong>{String(carga.ultimo.name)}</strong>
                      <p>
                        Último producto guardado · {String(carga.ultimo.quantity ?? '—')} unidades
                        en inventario.
                      </p>
                    </div>
                  </section>
                )}
                <AtribucionCatalogos />
              </div>
              <div className="escaneo-revision">
                {carga.mensaje && (
                  <p className="escaneo-mensaje" role="status">
                    {carga.mensaje}
                  </p>
                )}
                {carga.errores._form && (
                  <p className="escaneo-error" role="alert">
                    {carga.errores._form}
                  </p>
                )}
                {carga.borrador ? (
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
