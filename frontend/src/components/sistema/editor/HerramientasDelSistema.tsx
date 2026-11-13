import { useEffect, useRef, useState, type ReactNode } from 'react';
import type { Distribucion } from '@/lib/sistema/tipos';
import { Icon } from '../Icon';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import { LayoutToolbar } from '../LayoutToolbar';

type Panel = 'agregar' | 'distribucion';

type Props = {
  distribucion: Distribucion | undefined;
  onDistribucion: (distribucion: Distribucion) => void;
  onBloques: () => void;
  onSecciones: () => void;
  onNuevaSeccion: () => void;
  /** Sin esta acción (o sin `onResumen`) no se dibuja: el taller de Sami no las tiene. */
  onFoto?: () => void;
  onResumen?: () => void;
};

function Accion({
  icono,
  children,
  onClick,
  ...atributos
}: {
  icono: NombreDeIcono;
  children: ReactNode;
  onClick: () => void;
  'data-accion'?: string;
}) {
  return (
    <button type="button" onClick={onClick} {...atributos}>
      <Icon name={icono} />
      {children}
    </button>
  );
}

// Dos íconos en lugar de una fila de botones: «Agregar» (bloques, secciones, foto, resumen) y «Distribución»
// (los mosaicos). Cada uno despliega lo suyo al tocarlo, así el cuaderno queda limpio hasta que se necesita.
export function HerramientasDelCuaderno({
  distribucion,
  onDistribucion,
  onBloques,
  onSecciones,
  onNuevaSeccion,
  onFoto,
  onResumen,
}: Props) {
  const [abierto, setAbierto] = useState<Panel | null>(null);
  const contenedor = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!abierto) return;
    const alTocarAfuera = (evento: MouseEvent) => {
      if (!contenedor.current?.contains(evento.target as Node)) setAbierto(null);
    };
    const alTeclear = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') setAbierto(null);
    };
    document.addEventListener('mousedown', alTocarAfuera);
    document.addEventListener('keydown', alTeclear);
    return () => {
      document.removeEventListener('mousedown', alTocarAfuera);
      document.removeEventListener('keydown', alTeclear);
    };
  }, [abierto]);

  const alternar = (panel: Panel) => setAbierto((actual) => (actual === panel ? null : panel));
  // Elegir algo cierra el panel y hace lo que dice.
  const y = (accion: () => void) => () => {
    setAbierto(null);
    accion();
  };

  return (
    <div className="herramientas" ref={contenedor}>
      {/* La guía señala todo el contenedor: así se puede tocar el ícono y también lo que despliega. */}
      <div
        className={`herramienta ${abierto === 'agregar' ? 'abierta' : ''}`.trim()}
        data-tour="agregar-pieza-movil"
      >
        <button
          type="button"
          className="herramienta-icono"
          aria-label="Agregar y organizar"
          title="Agregar y organizar"
          aria-expanded={abierto === 'agregar'}
          data-accion="herramientas"
          onClick={() => alternar('agregar')}
        >
          <Icon name="plus" />
        </button>
        <div className="herramienta-panel" role="group" aria-label="Agregar y organizar">
          <Accion icono="plus" data-accion="mobile-library" onClick={y(onBloques)}>
            Bloques
          </Accion>
          <Accion icono="folder" onClick={y(onSecciones)}>
            Secciones
          </Accion>
          <Accion icono="plus" onClick={y(onNuevaSeccion)}>
            Agregar una sección
          </Accion>
          {onFoto && (
            <Accion icono="camera" onClick={y(onFoto)}>
              Traer datos de una foto
            </Accion>
          )}
          {onResumen && (
            <Accion icono="download" data-accion="resumen-pdf" onClick={y(onResumen)}>
              Resumen en PDF
            </Accion>
          )}
        </div>
      </div>
      <div className={`herramienta ${abierto === 'distribucion' ? 'abierta' : ''}`.trim()}>
        <button
          type="button"
          className="herramienta-icono"
          aria-label="Distribución de los bloques"
          title="Distribución de los bloques"
          aria-expanded={abierto === 'distribucion'}
          onClick={() => alternar('distribucion')}
        >
          <Icon name="grid" />
        </button>
        <div className="herramienta-panel distribucion">
          <LayoutToolbar
            compacta
            distribucion={distribucion}
            onElegir={(elegida) => {
              setAbierto(null);
              onDistribucion(elegida);
            }}
          />
        </div>
      </div>
    </div>
  );
}
