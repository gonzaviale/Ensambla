import { CompassIcon } from '@phosphor-icons/react';
import { Button, type ButtonVariant } from '@/components/ui';
import { useRecorrido } from '@/hooks/recorrido/useRecorrido';
import { cn } from '@/lib/cn';

type BotonRecorridoProps = {
  /**
   * `barra`: sólo el ícono (con su nombre accesible) y con texto cuando la barra tiene lugar.
   * `completo`: ícono y texto siempre (hoja «Más», sección de ayuda).
   */
  presentacion?: 'barra' | 'completo';
  /** Texto del botón; por defecto «Recorrido guiado». */
  etiqueta?: string;
  variant?: ButtonVariant;
  fullWidth?: boolean;
  className?: string;
  /** Se llama justo antes de empezar (por ejemplo, para cerrar una hoja). */
  alIniciar?: () => void;
};

// Abre el recorrido guiado desde el principio, esté o no «visto».
export function BotonRecorrido({
  presentacion = 'completo',
  etiqueta = 'Recorrido guiado',
  variant = 'ghost',
  fullWidth,
  className,
  alIniciar,
}: BotonRecorridoProps) {
  const { iniciar } = useRecorrido();
  const enBarra = presentacion === 'barra';

  return (
    <Button
      variant={variant}
      fullWidth={fullWidth}
      aria-label={enBarra ? etiqueta : undefined}
      title={enBarra ? etiqueta : undefined}
      className={cn('whitespace-nowrap', enBarra ? 'px-3' : 'justify-start', className)}
      onClick={() => {
        alIniciar?.();
        iniciar();
      }}
    >
      <CompassIcon aria-hidden size={22} />
      {enBarra ? <span className="hidden min-[1400px]:inline">{etiqueta}</span> : etiqueta}
    </Button>
  );
}
