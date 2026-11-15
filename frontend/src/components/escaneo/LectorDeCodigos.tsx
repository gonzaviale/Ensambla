import { useEffect, type RefObject } from 'react';
import type { CamaraDelLector } from '@/types/escaneo';
import { Icon } from '@/components/sistema/Icon';
import { DialogoLector } from './DialogoLector';
import { AtribucionCatalogos } from './AtribucionCatalogos';

type Props = {
  codigo: string;
  buscando: boolean;
  abierta: boolean;
  iniciando: boolean;
  sinLectura: boolean;
  sesion: number;
  camaras: CamaraDelLector[];
  camaraId: string;
  camaraActiva: string;
  camaraActualId: string;
  errorCamara: string;
  video: RefObject<HTMLVideoElement | null>;
  entradaCodigo: RefObject<HTMLInputElement | null>;
  onCodigo: (codigo: string) => void;
  onBuscar: () => void;
  onAbrir: () => void;
  onCerrar: () => void;
  onReiniciar: () => void;
  onCamara: (id: string) => void;
  panel: 'codigo' | 'ajustes' | null;
  onPanel: (panel: 'codigo' | 'ajustes' | null) => void;
  mensaje?: string;
};
export function LectorDeCodigos({
  codigo,
  buscando,
  abierta,
  iniciando,
  sinLectura,
  sesion,
  camaras,
  camaraId,
  camaraActiva,
  camaraActualId,
  errorCamara,
  video,
  entradaCodigo,
  onCodigo,
  onBuscar,
  onAbrir,
  onCerrar,
  onReiniciar,
  onCamara,
  panel,
  onPanel,
  mensaje,
}: Props) {
  useEffect(() => {
    if (panel === 'codigo') entradaCodigo.current?.focus({ preventScroll: true });
  }, [panel, entradaCodigo]);
  const abrirPanel = (nuevo: 'codigo' | 'ajustes') => {
    onCerrar();
    onPanel(nuevo);
  };
  return (
    <section
      className="escaneo-panel escaneo-lector escaneo-lector-inventario"
      aria-label="Lector de códigos"
    >
      <div className="escaneo-lector-titulo">
        <span className="escaneo-icono">
          <Icon name="camera" />
        </span>
        <div>
          <h2>Escaneá el producto</h2>
          <p>Después revisás los datos y confirmás la carga.</p>
        </div>
      </div>
      <div className={'escaneo-visor ' + (abierta ? 'escaneo-visor-activo' : '')}>
        {abierta && (
          <video
            key={sesion}
            ref={video}
            muted
            playsInline
            autoPlay
            aria-label="Vista previa del lector"
          />
        )}
        <div className="escaneo-visor-estado" role="status">
          <Icon name="camera" />
          <span>
            {iniciando
              ? 'Iniciando cámara…'
              : abierta
                ? 'Cámara lista para leer'
                : 'Cámara del celular'}
          </span>
        </div>
        <div className="escaneo-marco-codigo" aria-hidden="true">
          <span className="escaneo-barras-ejemplo" />
        </div>
      </div>
      <p className="escaneo-guia-codigo" role={errorCamara ? 'alert' : 'status'}>
        {errorCamara
          ? 'La cámara necesita atención. Abrí Ajustes o ingresá el código.'
          : sinLectura
            ? 'Alejá un poco el teléfono y evitá reflejos.'
            : 'Alineá todas las barras dentro del marco horizontal.'}
      </p>
      <div className="escaneo-lector-acciones">
        <button
          type="button"
          className={abierta ? 'btn secondary' : 'btn primary'}
          disabled={buscando || iniciando}
          aria-label={
            abierta ? 'Pausar cámara' : errorCamara ? 'Reintentar cámara' : 'Escanear producto'
          }
          onClick={abierta ? onCerrar : onAbrir}
        >
          <Icon name="camera" />
          {abierta
            ? iniciando
              ? 'Iniciando…'
              : 'Pausar cámara'
            : errorCamara
              ? 'Reintentar'
              : 'Escanear'}
        </button>
        <button
          type="button"
          className="escaneo-accion-secundaria"
          disabled={buscando}
          aria-label="Ingresar código · lector USB"
          title="Ingresar código o usar lector USB"
          onClick={() => abrirPanel('codigo')}
        >
          <span aria-hidden="true">123</span>
        </button>
        <button
          type="button"
          className="escaneo-accion-secundaria"
          aria-label="Ajustes del lector"
          onClick={() => abrirPanel('ajustes')}
        >
          <Icon name="settings" />
        </button>
      </div>
      <p className="escaneo-lector-nota">
        <Icon name="box" />
        Leer el código no cambia el stock.
      </p>
      {panel && (
        <DialogoLector
          titulo={panel === 'codigo' ? 'Ingresar código' : 'Ajustes del lector'}
          onCerrar={() => onPanel(null)}
        >
          {panel === 'codigo' ? (
            <form
              className="escaneo-codigo"
              onSubmit={(e) => {
                e.preventDefault();
                onBuscar();
              }}
            >
              <p>Escribí el código o usá un lector USB/Bluetooth y presioná Enter.</p>
              <label htmlFor="escaneo-codigo">Código de barras</label>
              <input
                ref={entradaCodigo}
                id="escaneo-codigo"
                inputMode="numeric"
                autoComplete="off"
                placeholder="EAN / UPC"
                value={codigo}
                onChange={(e) => onCodigo(e.target.value)}
                disabled={buscando}
              />
              {mensaje && <p role="status">{mensaje}</p>}
              <div className="escaneo-dialogo-acciones">
                <button type="button" className="btn secondary" onClick={() => onPanel(null)}>
                  Volver al lector
                </button>
                <button type="submit" className="btn primary" disabled={buscando || !codigo.trim()}>
                  <Icon name="search" />
                  {buscando ? 'Buscando…' : 'Buscar'}
                </button>
              </div>
            </form>
          ) : (
            <>
              {errorCamara && (
                <p className="escaneo-aviso-error" role="alert">
                  {errorCamara}
                </p>
              )}
              {camaras.length > 0 && (
                <label>
                  Cámara del lector
                  <select
                    value={camaraId || camaraActualId}
                    disabled={iniciando}
                    onChange={(e) => {
                      onCamara(e.target.value);
                      onPanel(null);
                    }}
                  >
                    <option value="">Elegir automáticamente</option>
                    {camaras.map((camara) => (
                      <option key={camara.id} value={camara.id}>
                        {camara.nombre}
                      </option>
                    ))}
                  </select>
                </label>
              )}
              {camaraActiva && <p>Cámara actual: {camaraActiva}</p>}
              <p>
                Mostrá un código por vez. Si la imagen no enfoca, probá otra cámara o la selección
                automática.
              </p>
              <button
                type="button"
                className="btn primary"
                disabled={buscando}
                onClick={() => {
                  onReiniciar();
                  onPanel(null);
                }}
              >
                <Icon name="camera" />
                Reiniciar lector
              </button>
              <AtribucionCatalogos />
            </>
          )}
        </DialogoLector>
      )}
    </section>
  );
}
