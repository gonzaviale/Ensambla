import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { Link } from '@tanstack/react-router';
import {
  CheckCircleIcon,
  CreditCardIcon,
  InfoIcon,
  WalletIcon,
  XCircleIcon,
} from '@phosphor-icons/react';
import { Button, InputField, Sheet, Spinner, Stepper, buttonClasses } from '@/components/ui';
import { usePlanActual, useAccionesRed } from '@/hooks/red/useRed';
import { fechaLarga, pesos } from '@/lib/red/formato';
import {
  NOMBRE_FUNCION,
  ORDEN_PLANES,
  PLANES,
  importeACobrar,
  planIncluye,
  precioPorMes,
} from '@/lib/red/planes';
import type { MedioPago, PeriodoPlan, PlanId } from '@/types/red';
import {
  TARJETA_DE_PRUEBA_RECHAZADA,
  detalleDeTarjeta,
  formatearNumero,
  formatearVencimiento,
  tarjetaSeRechaza,
  validarTarjeta,
  type ErroresTarjeta,
} from './pagoSimulado';
import { SelectorPeriodo } from './SelectorPeriodo';

const PASOS = ['Resumen', 'Pago', 'Listo'];
const MS_DE_PROCESO = 1200;

type FlujoProps = { plan: PlanId; periodoInicial: PeriodoPlan; onClose: () => void };
type Fase = 'eligiendo' | 'procesando' | 'rechazado';
type Pendiente = { medio: MedioPago; rechazar: boolean };

// Suscripción simulada en una hoja: Resumen → Pago → Listo. Nada se cobra de verdad y del número
// de tarjeta sólo se conserva «Visa •••• 4242».
export function FlujoSuscripcion({ plan, periodoInicial, onClose }: FlujoProps) {
  const actual = usePlanActual();
  const { suscribirse } = useAccionesRed();
  const definicion = PLANES[plan];

  // Lo que regía al abrir el flujo (después del pago el plan actual ya es el nuevo).
  const [antes] = useState(() => ({
    plan: actual.plan,
    activa: actual.suscripcion.estado === 'activa',
    proximoCobro: actual.suscripcion.proximoCobro,
  }));

  const [paso, setPaso] = useState(0);
  const [periodo, setPeriodo] = useState<PeriodoPlan>(periodoInicial);
  const [fase, setFase] = useState<Fase>('eligiendo');
  const [conTarjeta, setConTarjeta] = useState(false);
  const [pendiente, setPendiente] = useState<Pendiente | null>(null);
  const [tarjeta, setTarjeta] = useState({ numero: '', nombre: '', vencimiento: '' });
  const [errores, setErrores] = useState<ErroresTarjeta>({});

  const titulo = useRef<HTMLHeadingElement>(null);
  const montado = useRef(false);

  // El pago es una simulación con un «Procesando…» corto; el temporizador se limpia al cerrar.
  useEffect(() => {
    if (!pendiente) return;
    const id = window.setTimeout(() => {
      if (pendiente.rechazar) {
        setFase('rechazado');
      } else {
        suscribirse({ plan, periodo, medioPago: pendiente.medio });
        setFase('eligiendo');
        setPaso(2);
      }
      setPendiente(null);
    }, MS_DE_PROCESO);
    return () => window.clearTimeout(id);
  }, [pendiente, plan, periodo, suscribirse]);

  // Al cambiar de paso el foco va al título del paso nuevo (no al abrir: ahí lo ubica la hoja).
  useEffect(() => {
    if (!montado.current) {
      montado.current = true;
      return;
    }
    titulo.current?.focus();
  }, [paso, fase]);

  const sube = antes.activa && ORDEN_PLANES.indexOf(plan) > ORDEN_PLANES.indexOf(antes.plan);
  const baja = antes.activa && ORDEN_PLANES.indexOf(plan) < ORDEN_PLANES.indexOf(antes.plan);
  const importe = importeACobrar(plan, periodo);
  const medioGuardado = actual.suscripcion.medioPago;

  const pagar = (medio: MedioPago, rechazar = false) => {
    setFase('procesando');
    setPendiente({ medio, rechazar });
  };

  const enviarTarjeta = (event: FormEvent) => {
    event.preventDefault();
    const encontrados = validarTarjeta(tarjeta);
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) return;
    const medio: MedioPago = { tipo: 'tarjeta', detalle: detalleDeTarjeta(tarjeta.numero) };
    const rechazar = tarjetaSeRechaza(tarjeta.numero);
    // El número completo no se guarda en ningún lado: se descarta apenas se usa.
    setTarjeta({ numero: '', nombre: '', vencimiento: '' });
    pagar(medio, rechazar);
  };

  const probarOtroMedio = () => {
    setFase('eligiendo');
    setConTarjeta(false);
    setErrores({});
  };

  const procesando = fase === 'procesando';
  const habilitadas = definicion.funciones.filter((funcion) => !planIncluye(antes.plan, funcion));
  const seHabilita = habilitadas.length > 0 ? habilitadas : definicion.funciones;

  return (
    <Sheet
      open
      onClose={onClose}
      locked={procesando}
      title={`Suscribirte a ${definicion.nombre}`}
      description="Es una demostración: no se cobra nada de verdad."
      footer={
        paso === 0 ? (
          <Button variant="solid" onClick={() => setPaso(1)}>
            Seguir al pago
          </Button>
        ) : paso === 1 && fase === 'eligiendo' ? (
          <>
            <Button variant="secondary" onClick={() => setPaso(0)}>
              Volver al resumen
            </Button>
            {conTarjeta && (
              <Button variant="solid" type="submit" form="form-tarjeta-demo">
                Pagar {pesos(importe)}
              </Button>
            )}
          </>
        ) : paso === 2 ? (
          <>
            <Button variant="secondary" onClick={onClose}>
              Volver a Mi plan
            </Button>
            <Link to="/hoy" className={buttonClasses({ variant: 'solid' })}>
              Ir a Hoy
            </Link>
          </>
        ) : undefined
      }
    >
      <div className="flex flex-col gap-5">
        <Stepper pasos={PASOS} actual={paso} label="Pasos de la suscripción" />

        {paso === 0 && (
          <section aria-labelledby="flujo-resumen" className="flex flex-col gap-4">
            <h3 id="flujo-resumen" ref={titulo} tabIndex={-1} className="m-0 text-h3">
              Resumen
            </h3>
            <dl className="m-0 flex flex-col gap-3 text-body">
              <Fila etiqueta="Plan" valor={definicion.nombre} />
              <Fila etiqueta="Precio" valor={`${pesos(precioPorMes(plan, periodo))} por mes`} />
              <Fila
                etiqueta={periodo === 'anual' ? 'Importe por año' : 'Importe por mes'}
                valor={pesos(importe)}
              />
            </dl>
            <SelectorPeriodo value={periodo} onChange={setPeriodo} />

            {sube && (
              <Aviso>
                Subís desde {PLANES[antes.plan].nombre}: el cambio se activa ahora y desde ya tenés
                todo lo del plan {definicion.nombre}.
              </Aviso>
            )}
            {baja && (
              <Aviso>
                Bajás desde {PLANES[antes.plan].nombre}: el cambio rige a partir del próximo cobro
                {antes.proximoCobro ? `, el ${fechaLarga(antes.proximoCobro)}` : ''}. Hasta entonces
                conservás lo que ya pagaste. En esta demostración el cambio se ve enseguida.
              </Aviso>
            )}

            <div className="flex flex-col gap-2">
              <p className="m-0 text-base font-semibold">Incluye</p>
              <ul className="m-0 flex list-none flex-col gap-1.5 p-0 text-hint text-soft">
                {definicion.funciones.map((funcion) => (
                  <li key={funcion} className="flex items-start gap-2">
                    <CheckCircleIcon
                      aria-hidden
                      size={20}
                      weight="fill"
                      className="mt-0.5 shrink-0 text-ok"
                    />
                    {NOMBRE_FUNCION[funcion]}
                  </li>
                ))}
              </ul>
            </div>
            <p className="m-0 text-hint text-muted">
              Cancelás cuando quieras y no borramos tus datos: si cancelás, seguís con el plan
              Gratis.
            </p>
          </section>
        )}

        {paso === 1 && fase === 'eligiendo' && (
          <section aria-labelledby="flujo-pago" className="flex flex-col gap-4">
            <h3 id="flujo-pago" ref={titulo} tabIndex={-1} className="m-0 text-h3">
              ¿Cómo querés pagar {pesos(importe)}?
            </h3>
            <p
              role="note"
              className="m-0 flex items-start gap-2 rounded-lg bg-amber-100 px-4 py-3 text-base font-semibold text-amber-700"
            >
              <InfoIcon aria-hidden size={24} weight="fill" className="mt-0.5 shrink-0" />
              Es una simulación: no ingreses datos reales.
            </p>

            <div className="flex flex-col gap-3">
              {medioGuardado && (
                <Button variant="primary" fullWidth onClick={() => pagar(medioGuardado)}>
                  <WalletIcon aria-hidden size={22} />
                  Usar {medioGuardado.detalle}
                </Button>
              )}
              <Button
                variant="primary"
                fullWidth
                onClick={() => pagar({ tipo: 'mercado-pago', detalle: 'Mercado Pago' })}
              >
                <WalletIcon aria-hidden size={22} />
                Pagar con Mercado Pago
              </Button>
              <Button
                variant={conTarjeta ? 'secondary' : 'primary'}
                fullWidth
                aria-expanded={conTarjeta}
                onClick={() => setConTarjeta((abierto) => !abierto)}
              >
                <CreditCardIcon aria-hidden size={22} />
                Pagar con tarjeta
              </Button>
            </div>

            {conTarjeta && (
              <form
                id="form-tarjeta-demo"
                aria-label="Datos de la tarjeta (demostración)"
                noValidate
                onSubmit={enviarTarjeta}
                className="flex flex-col gap-4 rounded-lg border border-line bg-canvas p-4"
              >
                <InputField
                  label="Número de la tarjeta"
                  hint={`Para la demo: 4242 4242 4242 4242 se aprueba y ${TARJETA_DE_PRUEBA_RECHAZADA} se rechaza.`}
                  inputMode="numeric"
                  autoComplete="off"
                  placeholder="0000 0000 0000 0000"
                  autoFocus
                  value={tarjeta.numero}
                  error={errores.numero}
                  onChange={(event) =>
                    setTarjeta({ ...tarjeta, numero: formatearNumero(event.target.value) })
                  }
                />
                <InputField
                  label="Nombre en la tarjeta"
                  autoComplete="off"
                  value={tarjeta.nombre}
                  error={errores.nombre}
                  onChange={(event) => setTarjeta({ ...tarjeta, nombre: event.target.value })}
                />
                <InputField
                  label="Vencimiento"
                  hint="Mes y año, por ejemplo 12/30."
                  inputMode="numeric"
                  autoComplete="off"
                  placeholder="MM/AA"
                  value={tarjeta.vencimiento}
                  error={errores.vencimiento}
                  onChange={(event) =>
                    setTarjeta({
                      ...tarjeta,
                      vencimiento: formatearVencimiento(event.target.value),
                    })
                  }
                />
              </form>
            )}
          </section>
        )}

        {paso === 1 && fase === 'procesando' && (
          <section aria-labelledby="flujo-proceso" className="flex flex-col gap-3 py-6">
            <h3 id="flujo-proceso" ref={titulo} tabIndex={-1} className="m-0 text-h3">
              Estamos confirmando el pago
            </h3>
            <Spinner label="Procesando…" />
            <p className="m-0 text-hint text-muted">Un momento, no cierres esta ventana.</p>
          </section>
        )}

        {paso === 1 && fase === 'rechazado' && (
          <section aria-labelledby="flujo-rechazo" className="flex flex-col gap-4">
            <h3 id="flujo-rechazo" ref={titulo} tabIndex={-1} className="m-0 text-h3">
              No pudimos aprobar el pago
            </h3>
            <p
              role="alert"
              className="m-0 flex items-start gap-2 rounded-lg bg-coral-100 px-4 py-3 text-base font-semibold text-bad"
            >
              <XCircleIcon aria-hidden size={24} weight="fill" className="mt-0.5 shrink-0" />
              La tarjeta fue rechazada. No te cobramos nada y tu plan sigue igual.
            </p>
            <p className="m-0 text-body text-soft">
              Puede pasar por falta de saldo o por un dato mal escrito. Probá con otra tarjeta o con
              Mercado Pago.
            </p>
            <div>
              <Button variant="solid" onClick={probarOtroMedio}>
                Probar con otro medio
              </Button>
            </div>
          </section>
        )}

        {paso === 2 && (
          <section aria-labelledby="flujo-listo" className="flex flex-col gap-4">
            <h3
              id="flujo-listo"
              ref={titulo}
              tabIndex={-1}
              className="m-0 flex items-center gap-2 text-h3"
            >
              <CheckCircleIcon aria-hidden size={30} weight="fill" className="text-ok" />
              ¡Listo! Ya tenés el plan {definicion.nombre}
            </h3>
            {actual.suscripcion.proximoCobro && (
              <p className="m-0 text-body text-soft">
                El próximo cobro es el {fechaLarga(actual.suscripcion.proximoCobro)}:{' '}
                {pesos(importe)}. Podés cancelar cuando quieras desde esta pantalla.
              </p>
            )}
            <div className="flex flex-col gap-2">
              <p className="m-0 text-base font-semibold">
                {habilitadas.length > 0 ? 'Ahora tenés' : 'Tu plan incluye'}
              </p>
              <ul className="m-0 flex list-none flex-col gap-1.5 p-0 text-body text-soft">
                {seHabilita.map((funcion) => (
                  <li key={funcion} className="flex items-start gap-2">
                    <CheckCircleIcon
                      aria-hidden
                      size={22}
                      weight="fill"
                      className="mt-0.5 shrink-0 text-ok"
                    />
                    {NOMBRE_FUNCION[funcion]}
                  </li>
                ))}
              </ul>
            </div>
          </section>
        )}
      </div>
    </Sheet>
  );
}

function Fila({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <div className="flex flex-wrap items-baseline justify-between gap-x-4 border-b border-line pb-2">
      <dt className="text-hint text-muted">{etiqueta}</dt>
      <dd className="m-0 font-semibold">{valor}</dd>
    </div>
  );
}

function Aviso({ children }: { children: ReactNode }) {
  return (
    <p className="m-0 flex items-start gap-2 rounded-lg bg-sand-100 px-4 py-3 text-base text-fg">
      <InfoIcon aria-hidden size={22} weight="fill" className="mt-0.5 shrink-0" />
      <span>{children}</span>
    </p>
  );
}
