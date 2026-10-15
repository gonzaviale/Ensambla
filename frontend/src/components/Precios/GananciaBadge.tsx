import { CheckCircleIcon, WarningCircleIcon, WarningIcon } from '@phosphor-icons/react';
import { Badge, type BadgeTone } from '@/components/ui';
import { estadoDeGanancia, textoGanancia, type EstadoGanancia } from './ayudas';

const ESTADOS: Record<
  EstadoGanancia,
  { tono: BadgeTone; etiqueta: string; Icono: typeof WarningIcon }
> = {
  riesgo: { tono: 'bad', etiqueta: 'En riesgo', Icono: WarningIcon },
  atencion: { tono: 'warn', etiqueta: 'Justa', Icono: WarningCircleIcon },
  bien: { tono: 'ok', etiqueta: 'Bien', Icono: CheckCircleIcon },
};

type GananciaBadgeProps = { margen: number; minimo: number };

// La ganancia siempre lleva ícono y palabra: el color no es la única señal.
export function GananciaBadge({ margen, minimo }: GananciaBadgeProps) {
  const estado = ESTADOS[estadoDeGanancia(margen, minimo)];
  return (
    <Badge tone={estado.tono} icon={<estado.Icono size={18} weight="fill" />}>
      {textoGanancia(margen, minimo)} · {estado.etiqueta}
    </Badge>
  );
}
