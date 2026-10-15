import { useRef, useState } from 'react';
import { Link } from '@tanstack/react-router';
import { ConfettiIcon, SlidersHorizontalIcon, UsersThreeIcon } from '@phosphor-icons/react';
import { AjustarAvisos } from '@/components/Hoy/AjustarAvisos';
import { AvisosBloqueados } from '@/components/Hoy/AvisosBloqueados';
import { AvisosIgnorados } from '@/components/Hoy/AvisosIgnorados';
import {
  META_AVISO,
  TITULO_GRUPO,
  type GrupoAviso,
  type MensajeHoy,
} from '@/components/Hoy/avisoMeta';
import { formatearTiempo } from '@/components/Hoy/formatoHoy';
import { PedidoPanel } from '@/components/Hoy/PedidoPanel';
import { ResumenDelDia } from '@/components/Hoy/ResumenDelDia';
import { TarjetaAviso } from '@/components/Hoy/TarjetaAviso';
import { ValorAportado } from '@/components/Hoy/ValorAportado';
import { OfertaRecorrido } from '@/components/Recorrido/OfertaRecorrido';
import { Badge, Button, PageHeading, Panel, Toast, buttonClasses } from '@/components/ui';
import { useAccionesRed, useAlertas, useBarrio, useNegocio, useValor } from '@/hooks/red/useRed';
import { fechaLarga, nombreDelDia, pesos, plural } from '@/lib/red/formato';
import type { Alerta } from '@/types/red';

const GRUPOS: GrupoAviso[] = ['corregir', 'comprar'];

// «Hoy» (doc §2): la pantalla de inicio. El comercio no busca un reporte: la app le avisa qué precio
// corregir, qué comprar y cuánto valor ya generó. Cada aviso se resuelve con un toque.
export function HoyPage() {
  const negocio = useNegocio();
  const barrio = useBarrio();
  const valor = useValor();
  const { activas, bloqueadas, ignoradas } = useAlertas();
  const { ignorarAlerta, restaurarAlertas } = useAccionesRed();

  const [ajustando, setAjustando] = useState(false);
  const [mensaje, setMensaje] = useState<MensajeHoy | null>(null);
  const tituloAvisos = useRef<HTMLHeadingElement>(null);
  const tituloPedido = useRef<HTMLHeadingElement>(null);

  const hoy = new Date();
  const titulo = `${negocio.nombre}, ${nombreDelDia(hoy)} ${fechaLarga(hoy)}`;

  const porGrupo = (grupo: GrupoAviso): Alerta[] =>
    activas.filter((alerta) => META_AVISO[alerta.tipo].grupo === grupo);
  // La acción principal (sólida) es una sola: la del aviso con más plata en juego.
  const destacadaId = GRUPOS.flatMap(porGrupo)[0]?.id;

  function avisar(nuevo: MensajeHoy) {
    setMensaje(nuevo);
  }

  function ignorar(alerta: Alerta) {
    const previas = ignoradas.map((a) => a.id);
    ignorarAlerta(alerta.id);
    tituloAvisos.current?.focus();
    avisar({
      tipo: 'exito',
      mensaje: 'Ignoraste este aviso. Lo podés volver a mostrar más abajo.',
      accion: {
        etiqueta: 'Deshacer',
        // No hay «des-ignorar» de uno solo: se restauran todos y se vuelven a ignorar los de antes.
        onClick: () => {
          restaurarAlertas();
          previas.forEach((id) => ignorarAlerta(id));
        },
      },
    });
  }

  function recuperar(id: string) {
    restaurarAlertas();
    ignoradas.filter((a) => a.id !== id).forEach((a) => ignorarAlerta(a.id));
  }

  const lead =
    activas.length === 0
      ? 'Hoy no hay nada urgente. Acá ves lo que ya ganaste y cómo viene tu pedido.'
      : `Tenés ${plural(activas.length, 'aviso', 'avisos')} para mirar, ordenados por la plata que hay en juego.`;

  return (
    <div className="flex flex-col gap-8">
      <PageHeading
        kicker="Hoy"
        title={titulo}
        lead={lead}
        actions={
          <Button
            variant="secondary"
            data-recorrido="hoy-ajustar"
            onClick={() => setAjustando(true)}
          >
            <SlidersHorizontalIcon aria-hidden size={22} />
            Ajustar avisos
          </Button>
        }
      />

      <OfertaRecorrido />

      {!negocio.onboardingHecho && (
        <Panel flat className="flex flex-wrap items-center justify-between gap-3">
          <p className="m-0 min-w-0 flex-1 basis-64 text-body text-soft">
            <strong className="text-fg">Te falta terminar de configurar tu comercio.</strong>{' '}
            Contanos cómo trabajás y dejamos tus avisos listos.
          </p>
          <Link to="/bienvenida" className={buttonClasses({ variant: 'primary' })}>
            Terminar de configurar
          </Link>
        </Panel>
      )}

      <ValorAportado />

      <section
        aria-labelledby="hoy-avisos"
        data-recorrido="hoy-avisos"
        className="flex flex-col gap-5"
      >
        <h2 id="hoy-avisos" ref={tituloAvisos} tabIndex={-1} className="m-0 text-h3">
          Lo que importa hoy
        </h2>

        {activas.length === 0 ? (
          <Panel flat className="flex items-start gap-3">
            <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-sand-100 text-fg">
              <ConfettiIcon aria-hidden size={26} />
            </span>
            <div className="flex min-w-0 flex-col gap-1">
              <p className="m-0 text-h3 font-bold">
                {bloqueadas.length > 0
                  ? 'No tenés avisos para accionar con tu plan'
                  : 'Todo en orden por hoy'}
              </p>
              <p className="m-0 text-body text-soft">
                {bloqueadas.length > 0
                  ? 'Mirá abajo lo que no estás viendo.'
                  : 'Ningún precio en riesgo, ninguna lista para revisar y nada que se termine.'}
                {valor.plataProtegidaMes > 0 && (
                  <>
                    {' '}
                    Hasta ahora protegiste{' '}
                    <strong className="text-fg">≈ {pesos(valor.plataProtegidaMes)} por mes</strong>
                    {valor.minutosAhorrados > 0 &&
                      ` y te ahorraste ${formatearTiempo(valor.minutosAhorrados)} de trabajo`}
                    .
                  </>
                )}
              </p>
            </div>
          </Panel>
        ) : (
          GRUPOS.map((grupo) => {
            const avisos = porGrupo(grupo);
            if (avisos.length === 0) return null;
            return (
              <div key={grupo} className="flex flex-col gap-3">
                <h3 className="m-0 flex flex-wrap items-center gap-x-3 gap-y-1 text-body">
                  {TITULO_GRUPO[grupo]}
                  <Badge tone="neutral">{plural(avisos.length, 'aviso', 'avisos')}</Badge>
                </h3>
                <ul className="m-0 flex list-none flex-col gap-3 p-0">
                  {avisos.map((alerta) => (
                    <TarjetaAviso
                      key={alerta.id}
                      alerta={alerta}
                      destacada={alerta.id === destacadaId}
                      onIgnorar={ignorar}
                      avisar={avisar}
                    />
                  ))}
                </ul>
              </div>
            );
          })
        )}

        <AvisosIgnorados
          ignoradas={ignoradas}
          onRecuperar={recuperar}
          onRecuperarTodos={() => restaurarAlertas()}
        />
      </section>

      <AvisosBloqueados bloqueadas={bloqueadas} />

      <section
        aria-labelledby="hoy-pedido"
        data-recorrido="hoy-pedido"
        className="flex flex-col gap-4"
      >
        <h2 id="hoy-pedido" ref={tituloPedido} tabIndex={-1} className="m-0 text-h3">
          Tu pedido
        </h2>
        <PedidoPanel avisar={avisar} alQuitar={() => tituloPedido.current?.focus()} />
      </section>

      <ResumenDelDia />

      <Panel flat className="flex flex-wrap items-center justify-between gap-3">
        <p className="m-0 flex min-w-0 flex-1 basis-64 items-start gap-3 text-body text-soft">
          <UsersThreeIcon aria-hidden size={26} className="mt-0.5 shrink-0 text-accent-800" />
          <span>
            <strong className="text-fg">Tu red:</strong> {barrio.comercios} comercios de{' '}
            {barrio.zona} ya comparan proveedores y precios acá. Nunca vemos tus datos junto a los
            de otro: sólo promedios anónimos.
          </span>
        </p>
        <Link to="/red" className={buttonClasses({ variant: 'primary' })}>
          Ver mi red
        </Link>
      </Panel>

      <AjustarAvisos open={ajustando} onClose={() => setAjustando(false)} avisar={avisar} />

      {mensaje && (
        <Toast
          mensaje={mensaje.mensaje}
          tipo={mensaje.tipo}
          onClose={() => setMensaje(null)}
          accion={
            mensaje.accion && {
              etiqueta: mensaje.accion.etiqueta,
              onClick: () => {
                mensaje.accion?.onClick();
                setMensaje(null);
              },
            }
          }
        />
      )}
    </div>
  );
}
