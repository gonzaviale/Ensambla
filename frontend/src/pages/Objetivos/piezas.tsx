import type { ReactNode } from 'react';
import { Sami } from '@/components/sistema/Sami';

// Piezas visuales que comparten Objetivos y «Sami te enseña».

export function Encabezado({
  etiqueta,
  titulo,
  texto,
  sami = 'Te explica cada cálculo',
  samiTitulo = 'Sami está listo',
}: {
  etiqueta: string;
  titulo: string;
  texto: string;
  sami?: string;
  /** Qué está haciendo Sami en esta pantalla, en pocas palabras. */
  samiTitulo?: string;
}) {
  return (
    <header className="objetivos-encabezado">
      <div>
        <p className="objetivos-etiqueta">{etiqueta}</p>
        <h1>{titulo}</h1>
        <p>{texto}</p>
      </div>
      <div className="objetivos-sami-mini">
        <Sami pose="think" />
        <div>
          <strong>{samiTitulo}</strong>
          <span>{sami}</span>
        </div>
      </div>
    </header>
  );
}

export function Dato({
  etiqueta,
  valor,
  positivo,
}: {
  etiqueta: string;
  valor: string;
  positivo?: boolean;
}) {
  return (
    <div className="objetivos-dato">
      <span>{etiqueta}</span>
      <strong className={positivo ? 'positivo' : ''}>{valor}</strong>
    </div>
  );
}

export function Metrica({
  etiqueta,
  valor,
  alerta,
}: {
  etiqueta: string;
  valor: string;
  alerta?: boolean;
}) {
  return (
    <div className={`objetivos-metrica ${alerta ? 'alerta' : ''}`.trim()}>
      <span>{etiqueta}</span>
      <strong>{valor}</strong>
    </div>
  );
}

export function Explicacion({
  titulo = 'Sami te lo explica',
  children,
}: {
  titulo?: string;
  children: ReactNode;
}) {
  return (
    <aside className="objetivos-explicacion">
      <div className="objetivos-sami-cara">
        <Sami pose="think" />
      </div>
      <div>
        <strong>{titulo}</strong>
        <p>{children}</p>
      </div>
    </aside>
  );
}

/** Una barra de avance, de 0 a 1. */
export function Progreso({ valor, etiqueta }: { valor: number; etiqueta: string }) {
  const porcentaje = Math.round(valor * 100);
  return (
    <div
      className="objetivos-progreso"
      role="progressbar"
      aria-label={etiqueta}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={porcentaje}
    >
      <i style={{ width: `${porcentaje}%` }} />
    </div>
  );
}
