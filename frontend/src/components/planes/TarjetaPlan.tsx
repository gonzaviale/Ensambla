import { Btn } from '@/components/sistema/Btn';
import { GestionRenovacion } from './GestionRenovacion';
import { Icon } from '@/components/sistema/Icon';
import type { Plan } from '@/types/planes';

const FUNCIONES = [
  ['RECETAS_COSTOS', 'Recetas y cálculo de costos'],
  ['REPORTES_FILTRADOS', 'Reportes filtrados'],
  ['EXPORTAR_EXCEL', 'Descarga de reportes en Excel'],
] as const;

function limite(valor: number | null, configurado: boolean) {
  if (!configurado) return 'Por definir';
  return valor === null ? 'Sin límite comercial' : valor.toLocaleString('es-AR');
}

function precio(plan: Plan) {
  if (plan.codigo === 'GRATUITO') return 'Sin costo';
  if (!plan.precioConfigurado || plan.precioMensual === null || !plan.moneda)
    return 'Precio por definir';
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: plan.moneda,
  }).format(plan.precioMensual);
}

type TarjetaPlanProps = {
  plan: Plan;
  actual: boolean;
  pruebaActiva: boolean;
  puedeIniciar: boolean;
  activando: boolean;
  pruebaUtilizada: boolean;
  onIniciar: () => void;
};

export function TarjetaPlan({
  plan,
  actual,
  pruebaActiva,
  puedeIniciar,
  activando,
  pruebaUtilizada,
  onIniciar,
}: TarjetaPlanProps) {
  const premium = plan.codigo === 'PREMIUM';
  return (
    <article
      className={`plan-card ${premium ? 'plan-card-premium' : ''}`}
      aria-label={`Plan ${plan.nombre}`}
    >
      <div className="plan-card-top">
        <span className="plan-card-icon">
          <Icon name={premium ? 'spark' : 'grid'} />
        </span>
        {actual ? (
          <span className="plan-label">{pruebaActiva ? 'En prueba' : 'Tu plan'}</span>
        ) : null}
      </div>
      <h2>{plan.nombre}</h2>
      <p className="plan-card-description">
        {premium
          ? 'Más espacio para tus datos. Más herramientas para decidir.'
          : 'Lo esencial para empezar a organizar tu negocio.'}
      </p>
      <p className="plan-price">{precio(plan)}</p>
      <p className="plan-price-detail">
        {premium
          ? plan.precioConfigurado
            ? 'Cada 30 días · renovación automática'
            : 'La contratación estará disponible próximamente.'
          : 'Para tu negocio, sin vencimiento.'}
      </p>
      <dl className="plan-limits">
        <div>
          <dt>Cuadernos</dt>
          <dd>{limite(plan.limiteProyectos, plan.limitesConfigurados)}</dd>
        </div>
        <div>
          <dt>Registros en total</dt>
          <dd>{limite(plan.limiteRegistros, plan.limitesConfigurados)}</dd>
        </div>
        <div>
          <dt>Páginas de IA por mes</dt>
          <dd>{limite(plan.limitePaginasIaMensuales, plan.limitesConfigurados)}</dd>
        </div>
        <div>
          <dt>Avisos por mes</dt>
          <dd>{premium ? 'Todos los disponibles' : '3'}</dd>
        </div>
        <div>
          <dt>Recomendaciones de Mi zona y Otra zona por mes</dt>
          <dd>{premium ? 'Todas las disponibles' : '3 entre ambas funciones'}</dd>
        </div>
      </dl>
      <ul className="plan-features">
        <li>
          <Icon name="check" /> Carga manual, edición y consulta
        </li>
        <li>
          <Icon name="check" /> Stock básico
        </li>
        {FUNCIONES.map(([codigo, nombre]) => {
          const incluida = plan.funciones.includes(codigo);
          return (
            <li key={codigo} className={incluida ? '' : 'plan-feature-excluded'}>
              <Icon name={incluida ? 'check' : 'close'} />
              <span>
                {nombre}
                {!incluida ? <span className="sr-only">: no incluido</span> : null}
              </span>
            </li>
          );
        })}
      </ul>
      {premium ? (
        <div className="plan-card-bottom">
          <Btn cls="primary full" disabled={!puedeIniciar || activando} onClick={onIniciar}>
            {activando
              ? 'Activando tu prueba…'
              : puedeIniciar
                ? 'Probar Premium por 7 días'
                : pruebaActiva
                  ? 'Prueba activa'
                  : actual
                    ? 'Premium activo'
                    : pruebaUtilizada
                      ? 'Prueba ya utilizada'
                      : 'Prueba no disponible'}
          </Btn>
          <p>
            La prueba incluye <strong>10 páginas de IA en total</strong>, durante los 7 días.
          </p>
          <GestionRenovacion />
        </div>
      ) : (
        <p className="plan-card-bottom plan-free-note">
          Podés seguir usando tus datos al terminar la prueba.
        </p>
      )}
    </article>
  );
}
