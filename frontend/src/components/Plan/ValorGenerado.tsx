import { LockSimpleIcon, SealCheckIcon, SquaresFourIcon } from '@phosphor-icons/react';
import type { ReactNode } from 'react';
import { Panel } from '@/components/ui';
import { usePlanActual } from '@/hooks/negocio/usePlan';
import { useRespuestas } from '@/hooks/negocio/useRespuestas';
import { PLANES, planMinimoPara } from '@/lib/planes/planes';

// «Lo que desbloqueás»: cuántas respuestas ya tenés y cuántas más trae el plan completo. Sólo cuenta
// lo que es verdad hoy (respuestas listas, bloqueadas por el plan o a la espera de un bloque); no
// inventa ahorros.
export function ValorGenerado() {
  const { vistas, cargando, hayNegocio } = useRespuestas();
  const plan = usePlanActual();

  const listas = vistas.filter((vista) => vista.resultado.estado === 'lista').length;
  const delPlan = vistas.filter((vista) => vista.resultado.estado === 'plan').length;
  const faltanBloques = vistas.filter((vista) => vista.resultado.estado === 'bloqueada').length;
  const minimo = PLANES[planMinimoPara('respuestas-completas')];
  const incluida = plan.incluye('respuestas-completas');

  return (
    <Panel
      aria-labelledby="plan-valor"
      className="flex flex-col gap-4 border-accent-300 bg-accent-100"
    >
      <h2 id="plan-valor" className="m-0 text-h3">
        Lo que desbloqueás
      </h2>

      {cargando ? (
        <p className="m-0 text-body text-soft" aria-live="polite">
          Estamos contando tus respuestas…
        </p>
      ) : !hayNegocio ? (
        <p className="m-0 text-body text-soft">
          Todavía no armaste tu negocio. Cuando lo cuentes, acá vas a ver cuántas respuestas ya
          tenés y cuántas suma cada plan.
        </p>
      ) : (
        <>
          <p className="m-0 text-lead font-bold">
            Hoy tenés {listas} {listas === 1 ? 'respuesta' : 'respuestas'}
            {delPlan > 0 ? `; con el plan ${minimo.nombre} ves ${delPlan} más.` : '.'}
          </p>
          <ul className="m-0 grid list-none gap-3 p-0 sm:grid-cols-3">
            <Dato
              icono={<SealCheckIcon aria-hidden size={24} />}
              numero={listas}
              texto={listas === 1 ? 'respuesta lista' : 'respuestas listas'}
            />
            <Dato
              icono={<LockSimpleIcon aria-hidden size={24} />}
              numero={delPlan}
              texto={incluida ? 'del plan, ya incluidas' : `con el plan ${minimo.nombre}`}
            />
            <Dato
              icono={<SquaresFourIcon aria-hidden size={24} />}
              numero={faltanBloques}
              texto={faltanBloques === 1 ? 'espera un bloque' : 'esperan un bloque'}
            />
          </ul>
        </>
      )}

      <p className="m-0 text-hint text-muted">
        Es un conteo de lo que hay hoy en tu negocio: no estimamos ahorros ni prometemos resultados.
      </p>
    </Panel>
  );
}

function Dato({ icono, numero, texto }: { icono: ReactNode; numero: number; texto: string }) {
  return (
    <li className="flex items-center gap-3 rounded-lg bg-surface px-4 py-3">
      <span className="text-accent-800">{icono}</span>
      <p className="m-0 flex flex-col leading-tight">
        <span className="text-h3 font-extrabold">{numero}</span>
        <span className="text-hint text-soft">{texto}</span>
      </p>
    </li>
  );
}
