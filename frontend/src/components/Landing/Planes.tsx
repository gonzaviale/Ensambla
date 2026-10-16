import { Link } from '@tanstack/react-router';
import { cn } from '@/lib/cn';
import { pesos, plural } from '@/lib/formato';
import {
  DIAS_DE_PRUEBA,
  NOMBRE_FUNCION,
  ORDEN_PLANES,
  PLANES,
  type DefinicionPlan,
} from '@/lib/planes/planes';
import { variables } from './estilos';

// Lo que incluye cada plan; en los pagos, sólo lo que suman sobre el anterior.
function incluye(plan: DefinicionPlan, anterior: DefinicionPlan | null): string[] {
  if (!anterior) {
    return [
      plan.bloques === null
        ? NOMBRE_FUNCION['bloques-ilimitados']
        : `Hasta ${plural(plan.bloques, 'bloque', 'bloques')}`,
      'Las respuestas básicas',
      plan.cargasHistoriaMes === null
        ? 'Cargas de historia sin límite'
        : `${plural(plan.cargasHistoriaMes, 'carga', 'cargas')} de historia por mes`,
    ];
  }
  return plan.funciones
    .filter((funcion) => !anterior.funciones.includes(funcion))
    .map((funcion) => NOMBRE_FUNCION[funcion]);
}

export function Planes() {
  return (
    <section className="seccion seccion--tono" id="planes" aria-labelledby="planes-titulo">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">Planes y precios</p>
          <h2 id="planes-titulo">Empezá gratis. Crecé cuando lo necesites.</h2>
          <p>Armás tu sistema sin pagar y, cuando te sirva más, sumás lo que falte.</p>
        </header>

        <ul className="planes">
          {ORDEN_PLANES.map((id, i) => {
            const plan = PLANES[id];
            const previo = i > 0 ? PLANES[ORDEN_PLANES[i - 1] ?? 'gratis'] : null;
            return (
              <li
                key={id}
                className={cn('plan revelar', plan.destacado && 'plan--destacado')}
                style={variables({ '--d': `${i / 10}s` })}
              >
                {plan.destacado && <p className="plan__sello">El más elegido</p>}
                <h3>{plan.nombre}</h3>
                <p className="plan__bajada">{plan.bajada}</p>
                <p className="plan__precio">
                  <strong>{pesos(plan.precioMensual)}</strong>
                  <span> por mes</span>
                </p>
                {previo && <p className="plan__previo">Todo lo de {previo.nombre}, y además:</p>}
                <ul className="plan__lista">
                  {incluye(plan, previo).map((texto) => (
                    <li key={texto}>{texto}</li>
                  ))}
                </ul>
              </li>
            );
          })}
        </ul>

        <div className="planes__pie revelar">
          <p className="planes__prueba">Probá {DIAS_DE_PRUEBA} días gratis</p>
          <p className="planes__nota">
            Precios ilustrativos, en pesos por mes. En la prueba tenés todo; si no seguís, volvés al
            plan Gratis y no perdés nada de lo que cargaste.
          </p>
          <Link to="/registro" className="boton boton--primario boton--grande">
            Empezar mi prueba de {DIAS_DE_PRUEBA} días
          </Link>
        </div>
      </div>
    </section>
  );
}
