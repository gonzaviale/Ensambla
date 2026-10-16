import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate } from '@tanstack/react-router';
import { PasoArmado } from '@/components/Bienvenida/PasoArmado';
import { PasoContar, type DatosNegocio } from '@/components/Bienvenida/PasoContar';
import { PasoPrimerDato } from '@/components/Bienvenida/PasoPrimerDato';
import {
  Button,
  ErrorState,
  InfoTip,
  PageHeading,
  PageSpinner,
  Panel,
  Stepper,
  buttonClasses,
} from '@/components/ui';
import { useArmarSistema } from '@/hooks/negocio/useArmarSistema';
import { useDatosDeEjemplo } from '@/hooks/negocio/useDatosDeEjemplo';
import { useMiNegocio } from '@/hooks/negocio/useMiNegocio';
import { usePlanActual } from '@/hooks/negocio/usePlan';
import { ensamblar } from '@/lib/ensamblador';
import type { PiezaEnsamblada, PiezaId } from '@/types/negocio';
import type { Proyecto } from '@/types/proyecto';

const PASOS = ['Tu negocio', 'Tu sistema', 'Primer dato'];
const TOPE_GRATIS = 3;

// Onboarding: tres pasos, un dato por paso. 1) contar el negocio, 2) ver lo que Ensambla armó
// (y sacar lo que sobra), 3) cargar el primer dato. Si ya hay sistema, no se arma de nuevo.
export function BienvenidaPage() {
  const negocio = useMiNegocio();
  const plan = usePlanActual();
  const armar = useArmarSistema();
  const ejemplo = useDatosDeEjemplo();
  const navigate = useNavigate();

  const [paso, setPaso] = useState(0);
  const [datos, setDatos] = useState<DatosNegocio>({ nombre: '', relato: '', rubro: null });
  const [sacadas, setSacadas] = useState<PiezaId[]>([]);
  const [armado, setArmado] = useState<Proyecto | null>(null);
  const [armando, setArmando] = useState(false);
  const [errorArmado, setErrorArmado] = useState<string | undefined>();
  const [cargadas, setCargadas] = useState<number | undefined>();
  const [errorEjemplo, setErrorEjemplo] = useState<string | undefined>();

  const entendido = useMemo(
    () => ensamblar(datos.relato, datos.rubro ?? undefined),
    [datos.relato, datos.rubro],
  );
  const piezas: PiezaEnsamblada[] = entendido.piezas.filter((p) => !sacadas.includes(p.id));
  const ultimaSacada = entendido.piezas.find((p) => p.id === sacadas[sacadas.length - 1]);
  const tope = plan.incluye('bloques-ilimitados') ? undefined : TOPE_GRATIS;

  // Al cambiar de paso el foco va al contenedor, para que el lector de pantalla anuncie el paso.
  const contenedor = useRef<HTMLDivElement>(null);
  const primeraVez = useRef(true);
  useEffect(() => {
    if (primeraVez.current) {
      primeraVez.current = false;
      return;
    }
    contenedor.current?.focus();
    window.scrollTo?.({ top: 0 });
  }, [paso]);

  async function armarSistema() {
    if (armando || piezas.length === 0) return;
    const aArmar = (tope === undefined ? piezas : piezas.slice(0, tope)).map((p) => p.id);
    setArmando(true);
    setErrorArmado(undefined);
    try {
      const proyecto = await armar.mutateAsync({
        nombre: datos.nombre.trim(),
        piezas: aArmar,
        rubro: entendido.rubro,
      });
      setArmado(proyecto);
      setPaso(2);
    } catch {
      setErrorArmado('No pudimos armar tu sistema. Probá de nuevo en un momento.');
      setArmando(false);
    }
  }

  async function cargarEjemplo() {
    const sistemaId = armado?.id ?? negocio.sistemaId;
    if (!sistemaId) return;
    setErrorEjemplo(undefined);
    try {
      const resultado = await ejemplo.mutateAsync({ sistemaId, rubro: entendido.rubro });
      setCargadas(resultado.cargadas);
    } catch {
      setErrorEjemplo('No pudimos cargar los datos de ejemplo. Probá de nuevo.');
    }
  }

  const saltar = () => void navigate({ to: '/inicio' });

  const yaTieneSistema = negocio.hayNegocio && !armado && !armando;

  let contenido;
  if (!armado && !armando && negocio.cargando) {
    contenido = <PageSpinner />;
  } else if (!armado && !armando && negocio.error) {
    contenido = <ErrorState message="No pudimos ver si ya tenés un sistema armado." />;
  } else if (yaTieneSistema) {
    contenido = (
      <Panel className="flex flex-col items-start gap-3">
        <h2 className="m-0 text-h3">Ya tenés tu sistema</h2>
        <p className="m-0 text-body text-soft">
          Tu sistema <strong>{negocio.sistema?.nombre}</strong> ya está armado. Podés seguir
          sumándole bloques desde «Mis bloques».
        </p>
        <Link to="/inicio" className={buttonClasses({ variant: 'solid', size: 'lg' })}>
          Ir a mi inicio
        </Link>
      </Panel>
    );
  } else {
    contenido = (
      <>
        <Stepper pasos={PASOS} actual={paso} label="Pasos de la bienvenida" />
        <div
          ref={contenedor}
          tabIndex={-1}
          role="group"
          aria-label={PASOS[paso]}
          className="rounded-lg outline-none"
        >
          {paso === 0 && (
            <PasoContar
              valores={datos}
              entendido={entendido}
              onCambiar={(cambio) => setDatos((actuales) => ({ ...actuales, ...cambio }))}
              onSeguir={() => setPaso(1)}
            />
          )}
          {paso === 1 && (
            <PasoArmado
              piezas={piezas}
              ultimaSacada={ultimaSacada}
              tope={tope}
              armando={armando}
              error={errorArmado}
              onSacar={(id) => setSacadas((actuales) => [...actuales.filter((s) => s !== id), id])}
              onDeshacer={() => setSacadas((actuales) => actuales.slice(0, -1))}
              onVolver={() => setPaso(0)}
              onArmar={() => void armarSistema()}
            />
          )}
          {paso === 2 && (
            <PasoPrimerDato
              cargandoEjemplo={ejemplo.isPending}
              cargadas={cargadas}
              error={errorEjemplo}
              puedeCargarEjemplo={Boolean(armado?.id ?? negocio.sistemaId)}
              onCargarEjemplo={() => void cargarEjemplo()}
            />
          )}
        </div>
      </>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-2xl animate-fade-in flex-col gap-6 px-4 py-8">
      <PageHeading
        kicker="Bienvenida"
        title="Armemos tu sistema"
        lead={
          <>
            Contanos de tu negocio y Ensambla lo arma con bloques.{' '}
            <InfoTip tema="qué es un bloque">
              Un bloque es una pieza de tu sistema, como «Ventas» o «Gastos». Cada bloque que sumás
              te desbloquea respuestas nuevas.
            </InfoTip>
          </>
        }
      />
      <div className="flex justify-end">
        <Button variant="ghost" onClick={saltar}>
          Saltar
        </Button>
      </div>
      {contenido}
    </div>
  );
}
