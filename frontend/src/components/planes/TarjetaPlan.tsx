import { Btn } from '@/components/sistema/Btn';
import { GestionRenovacion } from './GestionRenovacion';
import { Icon } from '@/components/sistema/Icon';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import { textoDeNegocios } from '@/lib/planes/negocios';
import type { Plan } from '@/types/planes';

const FUNCIONES = [['REPORTES_FILTRADOS', 'Resúmenes en PDF']] as const;

const AVISOS_Y_RECOMENDACIONES_GRATUITOS = '6';

function limite(valor: number | null, configurado: boolean) {
  if (!configurado) return 'Por definir';
  return valor === null ? 'Sin límite' : valor.toLocaleString('es-AR');
}

function precio(plan: Plan) {
  if (plan.codigo === 'GRATUITO') return 'Sin costo';
  if (!plan.precioConfigurado || plan.precioMensual === null || !plan.moneda)
    return 'Precio por definir';
  const importe = plan.precioMensual.toLocaleString('es-AR', { maximumFractionDigits: 0 });
  return plan.moneda === 'ARS' ? `$${importe}` : `${importe} ${plan.moneda}`;
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
  const avisos = premium ? 'Sin límite' : AVISOS_Y_RECOMENDACIONES_GRATUITOS;
  const color = premium ? 'petrol' : 'blue';
  const limites: { icono: NombreDeIcono; titulo: string; valor: string }[] = [
    {
      icono: 'folder',
      titulo: 'Negocios',
      valor: plan.limitesConfigurados ? textoDeNegocios(plan.limiteProyectos) : 'Por definir',
    },
    {
      icono: 'note',
      titulo: 'Registros en total',
      valor: limite(plan.limiteRegistros, plan.limitesConfigurados),
    },
    {
      icono: 'camera',
      titulo: 'Análisis de foto por mes',
      valor: limite(plan.limitePaginasIaMensuales, plan.limitesConfigurados),
    },
    { icono: 'bell', titulo: 'Avisos por mes', valor: avisos },
    { icono: 'ubicacion', titulo: 'Recomendaciones de zona por mes', valor: avisos },
  ];
  return (
    <article
      className={`plan-card ${premium ? 'plan-card-premium' : ''}`}
      data-color={color}
      data-actual={actual ? 'true' : undefined}
      aria-label={`Plan ${plan.nombre}`}
    >
      <div className="plan-card-top">
        <span className="plan-card-icon">
          <Icon name={premium ? 'spark' : 'grid'} />
        </span>
        {actual ? (
          <span className="plan-label">{pruebaActiva ? 'En prueba' : 'Tu plan'}</span>
        ) : premium ? (
          <span className="plan-destacado">El más completo</span>
        ) : null}
      </div>
      <h2>{plan.nombre}</h2>
      <p className="plan-card-description">
        {premium
          ? 'Más espacio para tus datos. Más herramientas para decidir.'
          : 'Lo esencial para empezar a organizar tu negocio.'}
      </p>
      {premium ? (
        <>
          <p className="plan-price">{precio(plan)}</p>
          <p className="plan-price-detail">
            {plan.precioConfigurado
              ? 'Cada 30 días · renovación automática'
              : 'La contratación estará disponible próximamente.'}
          </p>
        </>
      ) : null}
      <dl className="plan-limits">
        {limites.map((fila) => (
          <div key={fila.titulo}>
            <dt>
              <span className="plan-limit-icono">
                <Icon name={fila.icono} />
              </span>
              {fila.titulo}
            </dt>
            <dd>{fila.valor}</dd>
          </div>
        ))}
      </dl>
      <ul className="plan-features">
        <li>
          <Icon name="check" /> Carga manual, edición y consulta
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
            La prueba incluye <strong>10 análisis de foto en total</strong>, durante los 7 días.
          </p>
          <GestionRenovacion />
        </div>
      ) : null}
    </article>
  );
}
