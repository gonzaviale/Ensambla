import { useEffect, useRef, useState } from 'react';
import { useAccionesDeAvisos } from '@/hooks/avisos/useAccionesDeAvisos';
import { useSistemaStore } from '@/stores/sistemaStore';
import { IconBtn } from '../IconBtn';
import { Sami } from '../Sami';
import { textoDeAccion } from './presentacion';

const VISIBLE_MS = 9000;
const TRAS_SALIR_MS = 5000;

// «Sami vio un cambio»: el aviso nuevo asoma un rato arriba a la derecha. Si se lo mira, no se va.
export function AvisoDeSami() {
  const nuevo = useSistemaStore((estado) => estado.avisoNuevo);
  const ocultar = useSistemaStore((estado) => estado.ocultarAvisoNuevo);
  const abrirPanel = useSistemaStore((estado) => estado.abrirPanel);
  const acciones = useAccionesDeAvisos();
  const [visible, setVisible] = useState(false);
  const temporizador = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  useEffect(() => {
    if (!nuevo) {
      setVisible(false);
      return;
    }
    const cuadro = requestAnimationFrame(() => setVisible(true));
    clearTimeout(temporizador.current);
    temporizador.current = setTimeout(ocultar, VISIBLE_MS);
    return () => {
      cancelAnimationFrame(cuadro);
      clearTimeout(temporizador.current);
    };
  }, [nuevo, ocultar]);

  if (!nuevo) return null;
  const { aviso, cantidad } = nuevo;
  const primera = aviso.acciones[0];

  return (
    <div
      id="sami-aviso"
      className={`sami-aviso p${aviso.prioridad} ${visible ? 'visible' : ''}`.trim()}
      role="status"
      aria-live="polite"
      onMouseEnter={() => clearTimeout(temporizador.current)}
      onMouseLeave={() => {
        temporizador.current = setTimeout(ocultar, TRAS_SALIR_MS);
      }}
    >
      <Sami pose="think" />
      <div className="sami-aviso-txt">
        <span className="eyebrow">Sami vio un cambio</span>
        <strong>{aviso.titulo}</strong>
        <p>{aviso.porque[0] ?? aviso.detalle}</p>
        <div className="row">
          {primera && (
            <button
              type="button"
              className="btn primary small"
              onClick={() => acciones.ejecutar(primera)}
            >
              {textoDeAccion(primera)}
            </button>
          )}
          <button
            type="button"
            className="btn secondary small"
            onClick={() => abrirPanel(aviso.id)}
          >
            {cantidad > 1 ? `Ver los ${cantidad} avisos nuevos` : 'Ver el aviso'}
          </button>
        </div>
      </div>
      <IconBtn name="close" label="Cerrar aviso" onClick={ocultar} />
    </div>
  );
}
