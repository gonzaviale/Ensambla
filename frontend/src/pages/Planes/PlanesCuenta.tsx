import { Btn } from '@/components/sistema/Btn';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { GestionRenovacion } from '@/components/planes/GestionRenovacion';
import { TarjetaPlan } from '@/components/planes/TarjetaPlan';
import { useActivandoPrueba } from '@/hooks/planes/mutations';
import { usePlanes } from '@/hooks/planes/queries';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { useSistemaStore } from '@/stores/sistemaStore';

const fecha = (valor: string) =>
  new Intl.DateTimeFormat('es-AR', { dateStyle: 'long', timeStyle: 'short', hour12: false }).format(
    new Date(valor),
  );

function Uso({
  titulo,
  cantidad,
  limite,
}: {
  titulo: string;
  cantidad: number;
  limite: number | null;
}) {
  const superado = limite !== null && cantidad > limite;
  return (
    <div className={`plan-usage ${superado ? 'plan-usage-exceeded' : ''}`}>
      <dt>{titulo}</dt>
      <dd>
        <strong>{cantidad.toLocaleString('es-AR')}</strong>
        <span> / {limite === null ? 'sin límite' : limite.toLocaleString('es-AR')}</span>
        {limite !== null ? (
          <progress aria-label={titulo} value={Math.min(cantidad, limite)} max={limite || 1} />
        ) : null}
        {superado ? <p>Superás el límite de nuevas altas. Tus datos siguen disponibles.</p> : null}
      </dd>
    </div>
  );
}

export function PlanesCuenta() {
  useTituloPagina('Planes');
  const consulta = usePlanes();
  const activando = useActivandoPrueba();
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const datos = consulta.data;
  const actual = datos?.planes.find((plan) => plan.codigo === datos.suscripcion.codigoPlanActual);
  const esPrueba = datos?.suscripcion.pruebaActiva ?? false;
  const pruebaFinalizada =
    datos?.prueba &&
    Date.parse(datos.prueba.finEn) <= Date.parse(datos.consultadoEn) &&
    !esPrueba &&
    datos.suscripcion.codigoPlanActual === 'GRATUITO';

  return (
    <>
      <CabeceraComun active="plans" />
      <main id="main" tabIndex={-1} className="plans-page reveal">
        <header className="plans-heading">
          <div>
            <span className="eyebrow">CADA ETAPA DE TU NEGOCIO CUENTA</span>
            <h1>
              Un plan para seguir
              <br />
              dándole forma.
            </h1>
            <p>Conocé lo que incluye tu espacio y elegí cuándo probar más herramientas.</p>
          </div>
          <span className="plans-heading-piece" aria-hidden="true">
            <Icon name="spark" />
          </span>
        </header>
        {consulta.isPending ? (
          <div role="status" className="plan-loading">
            Estamos consultando tu plan…
          </div>
        ) : null}
        {consulta.isError ? (
          <div role="alert" className="plan-error plan-query-error">
            <p>{consulta.mensajeError}</p>
            {datos ? <p>El estado puede haber cambiado. Actualizalo antes de continuar.</p> : null}
            <Btn
              cls="secondary small"
              disabled={consulta.isFetching}
              onClick={() => void consulta.refetch()}
            >
              Reintentar
            </Btn>
          </div>
        ) : null}
        {datos && actual ? (
          <>
            <section
              className={`plan-current ${esPrueba ? 'plan-current-trial' : ''}`}
              aria-labelledby="plan-current-title"
            >
              <div className="plan-current-heading">
                <div>
                  <span className="eyebrow">TU PLAN HOY</span>
                  <h2 id="plan-current-title">
                    {esPrueba
                      ? 'Tu prueba Premium está activa'
                      : `${actual.nombre}, tu espacio actual`}
                  </h2>
                  {esPrueba && datos.prueba ? (
                    <p>Disfrutá las funciones premium hasta el {fecha(datos.prueba.finEn)}.</p>
                  ) : null}
                  {pruebaFinalizada ? (
                    <p>Tu prueba terminó. Seguís con el gratuito y todos tus datos se conservan.</p>
                  ) : null}
                  {!esPrueba && !pruebaFinalizada ? (
                    <p>
                      {actual.codigo === 'PREMIUM' && datos.suscripcion.finEn
                        ? `Tu período pago está disponible hasta el ${fecha(datos.suscripcion.finEn)}.`
                        : 'Acá podés ver lo que llevás usado en tu negocio.'}
                    </p>
                  ) : null}
                </div>
                <Btn
                  cls="ghost small"
                  disabled={consulta.isFetching}
                  onClick={() => void consulta.refetch()}
                >
                  {consulta.isFetching ? 'Actualizando…' : 'Actualizar estado'}
                </Btn>
              </div>
              <dl className="plan-usage-grid">
                <Uso
                  titulo="Cuadernos"
                  cantidad={datos.uso.proyectos}
                  limite={actual.limiteProyectos}
                />
                <Uso
                  titulo="Registros guardados"
                  cantidad={datos.uso.registros}
                  limite={actual.limiteRegistros}
                />
                <Uso
                  titulo={esPrueba ? 'Páginas de IA de la prueba' : 'Páginas de IA de este mes'}
                  cantidad={datos.uso.paginasIa}
                  limite={datos.uso.limitePaginasIa}
                />
              </dl>
              <p className="plan-usage-note">
                {esPrueba
                  ? 'Las diez páginas son para toda la prueba: cambiar de mes no renueva el cupo.'
                  : `El cupo mensual de IA se renueva el ${fecha(datos.uso.finPeriodoIa)}.`}{' '}
                Los intentos admitidos y los reintentos también consumen páginas.
              </p>
              {esPrueba &&
              datos.uso.limitePaginasIa !== null &&
              datos.uso.paginasIa >= datos.uso.limitePaginasIa ? (
                <p className="plan-quota-notice" role="status">
                  Usaste las páginas de IA incluidas. Las demás funciones premium siguen disponibles
                  hasta que termine la prueba.
                </p>
              ) : null}
            </section>
            <div className="plans-section-heading">
              <h2>Qué incluye cada plan</h2>
              <p>Modalidad mensual · Los límites son para todo tu negocio.</p>
            </div>
            <section className="plans-grid" aria-label="Comparación de planes">
              {datos.planes.map((plan) => (
                <TarjetaPlan
                  key={plan.codigo}
                  plan={plan}
                  actual={plan.codigo === datos.suscripcion.codigoPlanActual}
                  pruebaActiva={esPrueba && plan.codigo === 'PREMIUM'}
                  puedeIniciar={datos.suscripcion.puedeIniciarPrueba && !consulta.isError}
                  activando={activando}
                  pruebaUtilizada={datos.prueba !== null}
                  onIniciar={() => abrirModal({ tipo: 'pruebaPremium' })}
                />
              ))}
            </section>
            <section className="plan-current plans-renovacion" aria-label="Gestión de renovación">
              <GestionRenovacion />
            </section>
            <section className="plans-reassurance" aria-labelledby="plans-data-title">
              <Icon name="check" />
              <div>
                <h2 id="plans-data-title">Tus datos tienen su lugar.</h2>
                <p>
                  Cuando termina la prueba conservás tus cuadernos, registros y recetas. Podés
                  consultar y editar tus datos; las nuevas altas y las funciones premium respetan
                  los límites del plan gratuito.
                </p>
              </div>
            </section>
          </>
        ) : null}
      </main>
      <PageFooter />
    </>
  );
}
