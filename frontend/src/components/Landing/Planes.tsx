import { Link } from '@tanstack/react-router';
import {
  DIAS_DE_PRUEBA,
  MESES_PAGOS_ANUAL,
  NOMBRE_FUNCION,
  ORDEN_PLANES,
  PLANES,
} from '@/lib/red/planes';
import { pesos } from '@/lib/red/formato';
import { cn } from '@/lib/cn';
import { variables } from './estilos';

// Lo que suma cada plan sobre el anterior, para no repetir la misma lista tres veces.
function incluye(indice: number) {
  const plan = PLANES[ORDEN_PLANES[indice] ?? 'gratis'];
  const anterior = indice > 0 ? PLANES[ORDEN_PLANES[indice - 1] ?? 'gratis'] : null;
  const propias = plan.funciones.filter((f) => !anterior?.funciones.includes(f));

  const limites = [
    plan.listasPorMes === null ? null : `${plan.listasPorMes} listas de proveedores por mes`,
    plan.productos === null
      ? 'Productos sin límite'
      : `Hasta ${plan.productos.toLocaleString('es-AR')} productos`,
  ].filter((texto): texto is string => texto !== null);

  return {
    plan,
    anterior,
    items: [...limites, ...propias.map((f) => NOMBRE_FUNCION[f])],
  };
}

export function Planes() {
  return (
    <section className="seccion" id="planes" aria-labelledby="planes-titulo">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">Planes y precios</p>
          <h2 id="planes-titulo">Se paga solo con lo que te hace ganar.</h2>
          <p>Empezá gratis. Cuando veas la plata que te cuidó, elegís el plan que te sirva.</p>
        </header>

        <ul className="planes">
          {ORDEN_PLANES.map((id, i) => {
            const { plan, anterior, items } = incluye(i);
            return (
              <li
                key={id}
                className={cn('plan revelar', plan.destacado && 'plan--destacado')}
                style={variables({ '--d': `${i / 10}s` })}
              >
                {plan.destacado && <p className="plan__sello">El más completo</p>}
                <h3>{plan.nombre}</h3>
                <p className="plan__bajada">{plan.bajada}</p>
                <p className="plan__precio">
                  <strong>{pesos(plan.precioMensual)}</strong>
                  <span> por mes</span>
                </p>
                {anterior && (
                  <p className="plan__previo">Todo lo de {anterior.nombre}, y además:</p>
                )}
                <ul className="plan__lista">
                  {items.map((texto) => (
                    <li key={texto}>{texto}</li>
                  ))}
                </ul>
              </li>
            );
          })}
        </ul>

        <div className="planes__pie revelar">
          <p className="planes__prueba">Probá {DIAS_DE_PRUEBA} días gratis, sin tarjeta</p>
          <p className="planes__nota">
            Precios ilustrativos, en pesos por mes; los vamos a validar con comercios reales.
            Pagando por año, se pagan {MESES_PAGOS_ANUAL} meses de 12. Si cancelás, volvés al plan
            Gratis y no perdés tus datos.
          </p>
          <Link to="/registro" className="boton boton--primario boton--grande">
            Empezar mi prueba de {DIAS_DE_PRUEBA} días
          </Link>
        </div>
      </div>
    </section>
  );
}
