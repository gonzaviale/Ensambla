import { QRCodeSVG } from 'qrcode.react';
import { useRef } from 'react';
import { DownloadSimpleIcon } from '@phosphor-icons/react';
import { useVinculacionWhatsapp } from '@/hooks/whatsapp/queries';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { enlaceParaPedir } from '@/lib/pedidos';

const NOMBRE_DEL_ARCHIVO = 'qr-pedidos-sami.svg';

/**
 * El QR que el dueño pone en su local: el cliente lo escanea y se abre en WhatsApp el chat con Sami, listo para pedir.
 * Usa el número de Sami; sin tu WhatsApp vinculado el comercio no recibe pedidos, así que antes hay que vincularlo.
 */
export function QrDeSami() {
  const { data: vinculo, isPending, isError } = useVinculacionWhatsapp();
  const navegacion = useNavegacion();
  const contenedor = useRef<HTMLDivElement>(null);

  // El QR es un SVG: se baja tal cual, en vectores, para imprimirlo grande sin que pierda calidad.
  const descargar = () => {
    const svg = contenedor.current?.querySelector('svg');
    if (!svg) return;
    const url = URL.createObjectURL(
      new Blob([new XMLSerializer().serializeToString(svg)], { type: 'image/svg+xml' }),
    );
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = NOMBRE_DEL_ARCHIVO;
    enlace.click();
    URL.revokeObjectURL(url);
  };

  if (isPending || isError) return null;

  return (
    <section className="pedidos-qr" aria-labelledby="pedidos-qr-titulo">
      <div className="pedidos-qr-texto">
        <h2 id="pedidos-qr-titulo">Que tus clientes te pidan por WhatsApp</h2>
        {vinculo === null ? (
          <>
            <p>
              Para recibir pedidos primero vinculá tu WhatsApp: Sami necesita saber cuál es tu
              comercio. Después vas a tener acá un QR para ponerlo en tu local.
            </p>
            <button
              type="button"
              className="btn primary"
              onClick={() => void navegacion.irACuenta('whatsapp')}
            >
              Vincular mi WhatsApp
            </button>
          </>
        ) : vinculo.numeroBot === '' ? (
          <p>Todavía no hay un número de Sami configurado. Cuando esté, vas a ver acá el QR.</p>
        ) : (
          <>
            <p>
              Ponelo en el mostrador o en la vidriera. Tu cliente lo escanea, se abre el chat con
              Sami y te pide por nombre: tiene que decir cómo se llama tu comercio.
            </p>
            <div className="pedido-acciones">
              <a
                className="btn secondary"
                href={enlaceParaPedir(vinculo.numeroBot)}
                target="_blank"
                rel="noreferrer"
              >
                Abrir el chat con Sami
              </a>
              <button type="button" className="btn secondary" onClick={descargar}>
                <DownloadSimpleIcon aria-hidden size={18} /> Descargar el QR
              </button>
            </div>
          </>
        )}
      </div>
      {vinculo && vinculo.numeroBot !== '' && (
        <div ref={contenedor} className="pedidos-qr-codigo">
          <QRCodeSVG
            value={enlaceParaPedir(vinculo.numeroBot)}
            size={176}
            level="M"
            marginSize={2}
            title="Código QR que abre el chat de WhatsApp con Sami"
          />
        </div>
      )}
    </section>
  );
}
