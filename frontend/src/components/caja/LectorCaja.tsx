import { useState } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { ControlSonidosEscaneo } from '@/components/escaneo/ControlSonidosEscaneo';
import { AtribucionCatalogos } from '@/components/escaneo/AtribucionCatalogos';
import type { PropsLectorCodigos } from '@/components/escaneo/LectorDeCodigos';
import { useSonidosEscaneo } from '@/hooks/escaneo/useSonidosEscaneo';
import { useLectorAutomatico } from '@/hooks/escaneo/useLectorAutomatico';
import {
  useConfirmacionLectura,
  type ConfirmacionLectura,
} from '@/hooks/escaneo/useConfirmacionLectura';
import { DialogoCaja } from './DialogoCaja';

export function LectorCaja({
  codigo,
  buscando,
  abierta,
  iniciando,
  sinLectura,
  sesion,
  camaras,
  camaraId,
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
  revisandoProducto,
  errorCodigo,
  mensaje,
  habilitado,
  lectura,
}: PropsLectorCodigos & {
  revisandoProducto: boolean;
  errorCodigo: string;
  mensaje: string;
  habilitado: boolean;
  lectura: ConfirmacionLectura | null;
}) {
  const [panel, setPanel] = useState<'codigo' | 'ajustes' | null>(null);
  const sonido = useSonidosEscaneo();
  const confirmacion = useConfirmacionLectura(lectura);
  useLectorAutomatico(habilitado && panel === null, {
    abrir: onAbrir,
    cerrar: onCerrar,
    abierta,
    iniciando,
  });
  const abrirPanel = (nuevo: 'codigo' | 'ajustes') => {
    onCerrar();
    setPanel(nuevo);
  };
  return (
    <section
      className="caja-escaner"
      aria-label="Lector de códigos"
      onPointerDownCapture={sonido.preparar}
      onKeyDownCapture={sonido.preparar}
    >
      <div className={`caja-visor ${abierta ? 'caja-visor-activo' : ''}`}>
        {abierta && (
          <video
            key={`camara-${sesion}`}
            ref={video}
            muted
            playsInline
            autoPlay
            aria-label="Vista previa del lector"
          />
        )}
        <div className="caja-visor-estado">
          <Icon name="camera" />
          <span>
            {abierta
              ? iniciando
                ? 'Iniciando cámara…'
                : 'Lectura continua'
              : 'Cámara del celular'}
          </span>
        </div>
        <div
          key={`lectura-${confirmacion?.id ?? 'esperando'}`}
          className={`caja-marco-codigo ${confirmacion ? `caja-lectura-${confirmacion.tipo}` : ''}`}
          aria-hidden="true"
        >
          <span className="caja-barras-ejemplo" />
          {confirmacion && (
            <span className="caja-lectura-confirmada">
              <Icon name={confirmacion.tipo === 'reconocido' ? 'check-circle' : 'help'} />
              {confirmacion.tipo === 'reconocido' ? 'Leído' : 'Revisar artículo'}
            </span>
          )}
        </div>
      </div>
      <p className="caja-guia-codigo" role={errorCamara || errorCodigo ? 'alert' : 'status'}>
        {errorCamara
          ? 'La cámara necesita atención. Abrí los ajustes.'
          : errorCodigo
            ? errorCodigo
            : sinLectura
              ? 'Alejá un poco el teléfono y evitá reflejos.'
              : mensaje || 'Alineá las barras dentro del marco horizontal.'}
      </p>
      <div className="caja-escaner-acciones">
        {errorCamara ? (
          <button
            type="button"
            className="btn primary"
            disabled={buscando}
            onClick={onAbrir}
            aria-label="Reintentar cámara"
          >
            <Icon name="camera" />
            Reintentar
          </button>
        ) : sonido.requiereActivacion ? (
          <button type="button" className="btn secondary" onClick={sonido.preparar}>
            <span aria-hidden="true">♪</span>Activar sonido
          </button>
        ) : (
          <div className="caja-lector-listo">
            <Icon name="camera" />
            <span>
              {iniciando ? 'Iniciando lector…' : abierta ? 'Lector activo' : 'Lector en espera'}
            </span>
          </div>
        )}
        <button
          type="button"
          className="caja-ingresar-codigo"
          aria-label="Ingresar código · lector USB"
          title="Ingresar código o usar lector USB"
          disabled={buscando}
          onClick={() => abrirPanel('codigo')}
        >
          <span aria-hidden="true">123</span>
        </button>
        <button
          type="button"
          className="caja-ajustes-lector"
          aria-label="Ajustes del lector"
          onClick={() => abrirPanel('ajustes')}
        >
          <Icon name="settings" />
        </button>
      </div>
      {panel && !revisandoProducto && (
        <DialogoCaja
          titulo={panel === 'codigo' ? 'Ingresar código' : 'Ajustes del lector'}
          onCerrar={() => setPanel(null)}
        >
          {panel === 'codigo' ? (
            <>
              <p className="caja-ayuda">
                Ingresá el código o usá tu lector USB/Bluetooth. Podés leer varios seguidos.
              </p>
              <form
                className="caja-formulario"
                onSubmit={(evento) => {
                  evento.preventDefault();
                  onBuscar();
                }}
              >
                <label>
                  Código de barras
                  <input
                    ref={entradaCodigo}
                    inputMode="numeric"
                    autoComplete="off"
                    value={codigo}
                    onChange={(evento) => onCodigo(evento.target.value)}
                    disabled={buscando}
                  />
                </label>
                {errorCodigo && (
                  <p role="alert" className="caja-error">
                    {errorCodigo}
                  </p>
                )}
                {mensaje && (
                  <p role="status" className="caja-mensaje">
                    <Icon name="check-circle" />
                    {mensaje}
                  </p>
                )}
                <div className="caja-acciones">
                  <button type="button" className="btn secondary" onClick={() => setPanel(null)}>
                    Listo
                  </button>
                  <button
                    type="submit"
                    className="btn primary"
                    disabled={buscando || !codigo.trim()}
                  >
                    {buscando ? 'Buscando…' : 'Buscar'}
                  </button>
                </div>
              </form>
            </>
          ) : (
            <>
              {errorCamara && (
                <p role="alert" className="caja-error">
                  {errorCamara}
                </p>
              )}
              <h3>Sonido de lectura</h3>
              <ControlSonidosEscaneo />
              {camaras.length > 0 && (
                <fieldset className="caja-elegir-camara">
                  <legend>Cámara del lector</legend>
                  <button
                    type="button"
                    aria-pressed={!camaraId}
                    onClick={() => {
                      onCamara('');
                      setPanel(null);
                    }}
                  >
                    Elegir automáticamente
                  </button>
                  {camaras.map((camara) => (
                    <button
                      key={camara.id}
                      type="button"
                      aria-pressed={(camaraId || camaraActualId) === camara.id}
                      onClick={() => {
                        onCamara(camara.id);
                        setPanel(null);
                      }}
                    >
                      {camara.nombre}
                    </button>
                  ))}
                </fieldset>
              )}
              <p className="caja-ayuda">
                Mostrá un código por vez. Retirá el producto antes de volver a leerlo.
              </p>
              <button
                type="button"
                className="btn secondary"
                onClick={() => {
                  onReiniciar();
                  setPanel(null);
                }}
              >
                <Icon name="camera" />
                Reiniciar lector
              </button>
              <AtribucionCatalogos />
            </>
          )}
        </DialogoCaja>
      )}
    </section>
  );
}
