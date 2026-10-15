import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { PasoContar, type DatosRelato } from '@/components/Bienvenida/PasoContar';
import { PasoEntendido } from '@/components/Bienvenida/PasoEntendido';
import { PasoPrimeraLista } from '@/components/Bienvenida/PasoPrimeraLista';
import { PasoReglas, type DatosReglas } from '@/components/Bienvenida/PasoReglas';
import { Button, PageHeading, Stepper } from '@/components/ui';
import { useAccionesRed, useNegocio, useReglas, useUmbrales } from '@/hooks/red/useRed';
import {
  BLOQUES,
  ORDEN_BLOQUES,
  interpretarRelato,
  sugerirReglas,
  type BloqueId,
} from '@/lib/red/relato';

type Pantalla = 'contar' | 'entendido' | 'reglas' | 'lista';

const PASOS = ['Tu comercio', 'Tus reglas de precio', 'Tu primera lista'];
const PASO_DE: Record<Pantalla, number> = { contar: 0, entendido: 0, reglas: 1, lista: 2 };

const PROVEEDOR_DE_EJEMPLO = 'prov-sur';

// «El relato prende los bloques» (docs/diferencial-red-proactiva.md §3) + tutorial inicial (T-057):
// tres pasos cortos y saltables. Todo queda guardado recién al confirmar cada paso.
export function BienvenidaPage() {
  const negocio = useNegocio();
  const reglasActuales = useReglas();
  const umbrales = useUmbrales();
  const { guardarNegocio, guardarReglas, guardarUmbrales, subirLista } = useAccionesRed();
  const navigate = useNavigate();

  const [pantalla, setPantalla] = useState<Pantalla>('contar');
  const [datos, setDatos] = useState<DatosRelato>({
    relato: negocio.relato,
    rubro: negocio.rubro,
    nombre: negocio.nombre,
    zona: negocio.zona,
  });
  const [elegidos, setElegidos] = useState<ReadonlySet<BloqueId>>(new Set());
  const [reglas, setReglas] = useState<DatosReglas | null>(null);
  const [errorLista, setErrorLista] = useState<string | undefined>();

  const interpretacion = useMemo(
    () => interpretarRelato(datos.relato, datos.rubro),
    [datos.relato, datos.rubro],
  );

  // Al cambiar de pantalla el foco va al contenedor, para que el lector de pantalla anuncie el paso.
  const contenedor = useRef<HTMLDivElement>(null);
  const primeraVez = useRef(true);
  useEffect(() => {
    if (primeraVez.current) {
      primeraVez.current = false;
      return;
    }
    contenedor.current?.focus();
    window.scrollTo?.({ top: 0 });
  }, [pantalla]);

  function irAEntendido() {
    setElegidos(new Set(interpretacion.bloques));
    setPantalla('entendido');
  }

  function alternarBloque(id: BloqueId) {
    setElegidos((actuales) => {
      const siguientes = new Set(actuales);
      if (siguientes.has(id)) siguientes.delete(id);
      else siguientes.add(id);
      return siguientes;
    });
  }

  function irAReglas() {
    // Si el rubro es el que ya estaba guardado se parte de las reglas existentes; si cambió, de lo
    // que suele andar bien en ese rubro. Lo que la persona ya tocó en esta pantalla se respeta.
    const sugerido = sugerirReglas(interpretacion.rubro);
    const mismoRubro = negocio.onboardingHecho && interpretacion.rubro === negocio.rubro;
    setReglas(
      (actuales) =>
        actuales ??
        (mismoRubro
          ? {
              margen: reglasActuales.general.margen,
              redondeo: reglasActuales.general.redondeo,
              margenMinimo: reglasActuales.margenMinimo,
            }
          : sugerido),
    );
    setPantalla('reglas');
  }

  function guardarReglasYSeguir(valores: DatosReglas) {
    setReglas(valores);
    guardarNegocio({
      nombre: datos.nombre.trim(),
      zona: datos.zona.trim(),
      relato: datos.relato.trim(),
      ...(interpretacion.rubro ? { rubro: interpretacion.rubro } : {}),
    });
    guardarReglas({
      ...reglasActuales,
      general: { margen: valores.margen, redondeo: valores.redondeo },
      margenMinimo: valores.margenMinimo,
    });
    // Los avisos de cada bloque se prenden o apagan según lo que la persona dejó tildado. Un bloque
    // que no se ofreció (ni se tildó) no se toca: los avisos vienen prendidos por defecto.
    const activas = { ...umbrales.activas };
    for (const id of ORDEN_BLOQUES) {
      const tildado = elegidos.has(id);
      const ofrecido = interpretacion.bloques.includes(id);
      if (!tildado && !ofrecido) continue;
      for (const aviso of BLOQUES[id].aviso) activas[aviso] = tildado;
    }
    guardarUmbrales({ ...umbrales, activas });
    setPantalla('lista');
  }

  function terminar() {
    guardarNegocio({ onboardingHecho: true });
  }

  function saltar() {
    terminar();
    void navigate({ to: '/hoy' });
  }

  function usarEjemplo() {
    const listaId = subirLista(PROVEEDOR_DE_EJEMPLO, 'whatsapp');
    if (!listaId) {
      setErrorLista('No pudimos cargar la lista de ejemplo. Probá de nuevo o elegí otra opción.');
      return;
    }
    terminar();
    void navigate({ to: '/listas/$listaId', params: { listaId } });
  }

  return (
    <div className="mx-auto flex w-full max-w-2xl animate-fade-in flex-col gap-6 px-4 py-8">
      <PageHeading
        kicker="Bienvenida"
        title="Armemos Ensambla a tu medida"
        lead="Tres pasos cortos. Podés saltarlos y volver cuando quieras desde Mi cuenta."
      />
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Stepper pasos={PASOS} actual={PASO_DE[pantalla]} label="Pasos de la bienvenida" />
        <Button variant="ghost" onClick={saltar}>
          Saltar
        </Button>
      </div>

      <div
        ref={contenedor}
        tabIndex={-1}
        aria-label={PASOS[PASO_DE[pantalla]]}
        role="group"
        className="rounded-lg outline-none"
      >
        {pantalla === 'contar' && (
          <PasoContar
            valores={datos}
            onCambiar={(cambio) => setDatos((actuales) => ({ ...actuales, ...cambio }))}
            onSeguir={irAEntendido}
          />
        )}
        {pantalla === 'entendido' && (
          <PasoEntendido
            relato={interpretacion}
            elegidos={elegidos}
            onAlternar={alternarBloque}
            onVolver={() => setPantalla('contar')}
            onSeguir={irAReglas}
          />
        )}
        {pantalla === 'reglas' && reglas && (
          <PasoReglas
            rubro={interpretacion.rubro}
            inicial={reglas}
            sugerido={sugerirReglas(interpretacion.rubro)}
            onVolver={() => setPantalla('entendido')}
            onSeguir={guardarReglasYSeguir}
          />
        )}
        {pantalla === 'lista' && (
          <PasoPrimeraLista
            error={errorLista}
            onTerminar={terminar}
            onUsarEjemplo={usarEjemplo}
            onVolver={() => setPantalla('reglas')}
          />
        )}
      </div>
    </div>
  );
}
