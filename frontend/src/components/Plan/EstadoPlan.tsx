import {
  CheckCircleIcon,
  CreditCardIcon,
  ProhibitIcon,
  SparkleIcon,
  UserIcon,
  WarningCircleIcon,
} from '@phosphor-icons/react';
import { Badge, Button, InfoTip, Panel, type BadgeTone } from '@/components/ui';
import { usePlanActual, useProductos } from '@/hooks/red/useRed';
import { fechaLarga, pesos, plural } from '@/lib/red/formato';
import { PLANES, importeACobrar } from '@/lib/red/planes';
import { cn } from '@/lib/cn';
import type { ReactNode } from 'react';

type EstadoPlanProps = {
  onCancelar: () => void;
  onReactivar: () => void;
};

type Descripcion = { titular: string; detalle: string; tono: BadgeTone; etiqueta: string };

// «Tu plan hoy»: el estado en palabras claras, el medio de pago y lo que se usó del mes.
export function EstadoPlan({ onCancelar, onReactivar }: EstadoPlanProps) {
  const plan = usePlanActual();
  const productos = useProductos();
  const { suscripcion, definicion } = plan;
  const medio = suscripcion.medioPago;
  const descripcion = describir(plan);

  return (
    <Panel aria-labelledby="plan-hoy" data-recorrido="plan-estado" className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-1">
          <h2 id="plan-hoy" className="m-0 text-h3">
            Tu plan hoy
          </h2>
          <InfoTip tema="prueba de 7 días">
            Durante 7 días usás todas las funciones del plan más completo, sin cargar tarjeta. Se
            puede usar una sola vez; al terminar, seguís con el plan Gratis si no elegís otro.
          </InfoTip>
        </div>
        <Badge tone={descripcion.tono} icon={iconoDe(descripcion.tono)}>
          {descripcion.etiqueta}
        </Badge>
      </div>

      <div className="flex flex-col gap-1.5" aria-live="polite">
        <p className="m-0 text-lead font-bold">{descripcion.titular}</p>
        <p className="m-0 text-body text-soft">{descripcion.detalle}</p>
      </div>

      <dl className="m-0 flex flex-col gap-4 sm:grid sm:grid-cols-2">
        <div className="flex min-w-0 flex-col gap-1">
          <dt className="flex items-center gap-2 text-hint text-muted">
            <CreditCardIcon aria-hidden size={20} />
            Medio de pago
          </dt>
          <dd className="m-0 text-body font-semibold">
            {medio ? medio.detalle : 'Todavía no cargaste uno: no hace falta para probar.'}
          </dd>
        </div>
        <div className="flex min-w-0 flex-col gap-1">
          <dt className="flex items-center gap-2 text-hint text-muted">
            <UserIcon aria-hidden size={20} />
            Personas con acceso
          </dt>
          <dd className="m-0 text-body font-semibold">{definicion.usuarios}</dd>
        </div>
      </dl>

      <div className="flex flex-col gap-4 border-t border-line pt-4">
        <h3 className="m-0 text-base font-bold">Lo que usaste este mes</h3>
        <Medidor
          etiqueta="Listas de proveedores"
          texto={
            plan.limiteListas === null
              ? `${plural(plan.listasUsadasMes, 'lista', 'listas')} este mes (sin límite)`
              : `${plan.listasUsadasMes} de ${plan.limiteListas} listas este mes`
          }
          valor={plan.listasUsadasMes}
          tope={plan.limiteListas}
          alerta={plan.sinListasEsteMes ? 'Llegaste al tope de listas de este mes.' : undefined}
        />
        <Medidor
          etiqueta="Productos cargados"
          texto={
            definicion.productos === null
              ? `${plural(productos.length, 'producto', 'productos')} (sin límite)`
              : `${productos.length} de ${definicion.productos.toLocaleString('es-AR')} productos`
          }
          valor={productos.length}
          tope={definicion.productos}
          alerta={
            definicion.productos !== null && productos.length > definicion.productos
              ? 'Pasaste el tope de productos de este plan.'
              : undefined
          }
        />
      </div>

      {(suscripcion.estado === 'activa' || suscripcion.estado === 'cancelada') &&
        !(plan.estado === 'vencida') && (
          <div className="flex flex-wrap gap-3 border-t border-line pt-4">
            {suscripcion.estado === 'activa' ? (
              <Button variant="danger" onClick={onCancelar}>
                Cancelar suscripción
              </Button>
            ) : (
              <Button variant="solid" onClick={onReactivar}>
                Volver a activar
              </Button>
            )}
          </div>
        )}
    </Panel>
  );
}

function describir(plan: ReturnType<typeof usePlanActual>): Descripcion {
  const { suscripcion, definicion } = plan;
  const nombre = definicion.nombre;

  if (plan.enPrueba) {
    const quedan = plan.diasDePrueba;
    return {
      etiqueta: 'En prueba',
      tono: 'accent',
      titular: `Estás probando ${PLANES.red.nombre}: te ${quedan === 1 ? 'queda' : 'quedan'} ${quedan} ${quedan === 1 ? 'día' : 'días'}.`,
      detalle: `${suscripcion.pruebaHasta ? `La prueba termina el ${fechaLarga(suscripcion.pruebaHasta)}. ` : ''}Después seguís con el plan Gratis, sin perder tus datos y sin que te cobremos nada.`,
    };
  }
  if (suscripcion.estado === 'activa') {
    return {
      etiqueta: 'Activo',
      tono: 'ok',
      titular: `Plan ${nombre} activo.`,
      detalle: suscripcion.proximoCobro
        ? `Próximo cobro el ${fechaLarga(suscripcion.proximoCobro)}: ${pesos(importeACobrar(suscripcion.plan, suscripcion.periodo))} (${suscripcion.periodo === 'anual' ? 'plan anual' : 'plan mensual'}).`
        : 'Se renueva solo y lo cancelás cuando quieras.',
    };
  }
  if (plan.estado === 'cancelada') {
    return {
      etiqueta: 'Cancelado',
      tono: 'warn',
      titular: `Cancelaste: seguís con el plan ${nombre}${suscripcion.activoHasta ? ` hasta el ${fechaLarga(suscripcion.activoHasta)}` : ''}.`,
      detalle:
        'Después pasás al plan Gratis y no borramos tus datos. Si cambiás de idea, podés volver a activarlo.',
    };
  }
  if (plan.estado === 'vencida') {
    const eraPrueba = suscripcion.estado === 'prueba';
    return {
      etiqueta: eraPrueba ? 'Prueba terminada' : 'Plan terminado',
      tono: 'bad',
      titular: eraPrueba
        ? 'Terminó tu prueba: estás en el plan Gratis.'
        : `Terminó tu plan ${PLANES[suscripcion.plan].nombre}: estás en el plan Gratis.`,
      detalle:
        'Tus datos están intactos. Algunas funciones quedaron con candado hasta que elijas un plan.',
    };
  }
  return {
    etiqueta: 'Gratis',
    tono: 'neutral',
    titular: 'Plan Gratis.',
    detalle: suscripcion.pruebaUsada
      ? 'Tenés lo básico para empezar. Cuando quieras, elegí un plan abajo.'
      : 'Tenés lo básico para empezar. Todavía podés probar 7 días gratis con todas las funciones.',
  };
}

function iconoDe(tono: BadgeTone): ReactNode {
  switch (tono) {
    case 'accent':
      return <SparkleIcon size={14} weight="fill" />;
    case 'ok':
      return <CheckCircleIcon size={14} weight="fill" />;
    case 'warn':
      return <WarningCircleIcon size={14} weight="fill" />;
    case 'bad':
      return <ProhibitIcon size={14} weight="bold" />;
    default:
      return <UserIcon size={14} weight="fill" />;
  }
}

type MedidorProps = {
  etiqueta: string;
  texto: string;
  valor: number;
  tope: number | null;
  alerta?: string;
};

// La barra es decorativa: el texto lleva la información (ícono + texto en la alerta, no sólo color).
function Medidor({ etiqueta, texto, valor, tope, alerta }: MedidorProps) {
  const proporcion = tope && tope > 0 ? Math.min(100, Math.round((valor / tope) * 100)) : 0;
  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex flex-wrap items-baseline justify-between gap-x-3">
        <span className="text-hint text-muted">{etiqueta}</span>
        <span className="text-body font-semibold">{texto}</span>
      </div>
      {tope !== null && (
        <div aria-hidden className="h-2.5 w-full overflow-hidden rounded-full bg-sand-100">
          <div
            className={cn('h-full rounded-full', alerta ? 'bg-bad' : 'bg-ok')}
            style={{ width: `${proporcion}%` }}
          />
        </div>
      )}
      {alerta && (
        <p className="m-0 flex items-center gap-1.5 text-hint font-semibold text-bad">
          <WarningCircleIcon aria-hidden size={18} weight="fill" className="shrink-0" />
          {alerta}
        </p>
      )}
    </div>
  );
}
