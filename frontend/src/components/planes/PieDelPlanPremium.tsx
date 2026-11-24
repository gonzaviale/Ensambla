import { Btn } from '@/components/sistema/Btn';
import { textoDelBotonDePrueba, type EstadoDeLaPrueba } from '@/lib/planes/textoDelBotonDePrueba';
import { GestionRenovacion } from './GestionRenovacion';

type PieDelPlanPremiumProps = EstadoDeLaPrueba & { onIniciar: () => void };

export function PieDelPlanPremium({ onIniciar, ...estado }: PieDelPlanPremiumProps) {
  return (
    <div className="plan-card-bottom">
      <Btn cls="primary full" disabled={!estado.puedeIniciar || estado.activando} onClick={onIniciar}>
        {textoDelBotonDePrueba(estado)}
      </Btn>
      <p>
        La prueba incluye <strong>10 análisis de foto en total</strong>, durante los 7 días.
      </p>
      <GestionRenovacion />
    </div>
  );
}
