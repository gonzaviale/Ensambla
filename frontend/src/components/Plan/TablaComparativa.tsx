import { CheckIcon, LockSimpleIcon } from '@phosphor-icons/react';
import { pesos } from '@/lib/formato';
import {
  NOMBRE_FUNCION,
  ORDEN_PLANES,
  PLANES,
  planIncluye,
  precioPorMes,
} from '@/lib/planes/planes';
import type { FuncionPlan, PeriodoPlan } from '@/types/negocio';

// Las funciones que en realidad son límites se muestran en las filas de arriba.
const COMO_LIMITE: FuncionPlan[] = ['bloques-ilimitados', 'varias-personas'];
const FUNCIONES = PLANES['negocio-plus'].funciones.filter(
  (funcion) => !COMO_LIMITE.includes(funcion),
);

const th = 'border-b border-line px-3 py-3 text-left align-top font-semibold';
const td = 'border-b border-line px-3 py-3 text-center align-middle';

// Comparación función por función: sólo en escritorio (en el celular cada tarjeta ya la trae).
export function TablaComparativa({ periodo }: { periodo: PeriodoPlan }) {
  return (
    <div className="hidden overflow-x-auto rounded-lg border border-line/60 bg-surface shadow-sm lg:block">
      <table className="w-full border-collapse text-body">
        <caption className="px-4 py-3 text-left text-h3 font-bold">Qué incluye cada plan</caption>
        <thead>
          <tr>
            <th scope="col" className={th}>
              <span className="sr-only">Función</span>
            </th>
            {ORDEN_PLANES.map((id) => (
              <th key={id} scope="col" className={`${td} font-bold`}>
                {PLANES[id].nombre}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          <tr>
            <th scope="row" className={th}>
              Precio por mes
            </th>
            {ORDEN_PLANES.map((id) => (
              <td key={id} className={`${td} font-semibold`}>
                {pesos(precioPorMes(id, periodo))}
              </td>
            ))}
          </tr>
          <tr>
            <th scope="row" className={th}>
              Bloques
            </th>
            {ORDEN_PLANES.map((id) => (
              <td key={id} className={td}>
                {PLANES[id].bloques ?? 'Sin límite'}
              </td>
            ))}
          </tr>
          <tr>
            <th scope="row" className={th}>
              Cargas de historia por mes
            </th>
            {ORDEN_PLANES.map((id) => (
              <td key={id} className={td}>
                {PLANES[id].cargasHistoriaMes ?? 'Sin límite'}
              </td>
            ))}
          </tr>
          <tr>
            <th scope="row" className={th}>
              Personas con acceso
            </th>
            {ORDEN_PLANES.map((id) => (
              <td key={id} className={td}>
                {PLANES[id].personas}
              </td>
            ))}
          </tr>
          {FUNCIONES.map((funcion) => (
            <tr key={funcion}>
              <th scope="row" className={th}>
                {NOMBRE_FUNCION[funcion]}
              </th>
              {ORDEN_PLANES.map((id) => {
                const incluida = planIncluye(id, funcion);
                return (
                  <td key={id} className={td}>
                    {incluida ? (
                      <CheckIcon aria-hidden size={24} weight="bold" className="inline text-ok" />
                    ) : (
                      <LockSimpleIcon aria-hidden size={22} className="inline text-muted" />
                    )}
                    <span className="sr-only">{incluida ? 'Incluido' : 'No incluido'}</span>
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
