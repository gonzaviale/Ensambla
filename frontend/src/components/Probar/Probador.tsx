import { useMemo, useState } from 'react';
import { CheckIcon, LockSimpleIcon, PlusIcon } from '@phosphor-icons/react';
import { TarjetaBloque } from '@/components/Bienvenida/TarjetaBloque';
import { Badge, Button, InputField, Panel } from '@/components/ui';
import { cn } from '@/lib/cn';
import { NOMBRE_RUBRO, RUBROS, nombreDePieza, pieza } from '@/lib/catalogo/piezas';
import { ensamblar } from '@/lib/ensamblador';
import { definicion, faltan } from '@/lib/respuestas/definiciones';
import type { PiezaEnsamblada, PiezaId, RubroId } from '@/types/negocio';
import { RESPUESTAS_DE_MUESTRA, muestraDe } from './muestras';

// Simulador público: todo ocurre en el navegador, sin llamadas a la API. Una frase (o un rubro)
// arma los bloques; cada bloque que se suma desbloquea una respuesta con números de muestra.
export function Probador() {
  const [frase, setFrase] = useState('');
  const [rubro, setRubro] = useState<RubroId | null>(null);
  const [sumadas, setSumadas] = useState<PiezaId[]>([]);

  const hayAlgo = frase.trim() !== '' || rubro !== null;
  const ensamblado = useMemo(() => ensamblar(frase, rubro ?? undefined), [frase, rubro]);

  const piezas: PiezaEnsamblada[] = useMemo(() => {
    const ids = new Set(ensamblado.piezas.map((p) => p.id));
    const extra = sumadas
      .filter((id) => !ids.has(id))
      .map((id) => ({
        id,
        nombre: nombreDePieza(id, ensamblado.rubro),
        motivo: 'La sumaste vos.',
      }));
    return [...ensamblado.piezas, ...extra];
  }, [ensamblado, sumadas]);

  const presentes = piezas.map((p) => p.id);
  const respuestas = RESPUESTAS_DE_MUESTRA[ensamblado.rubro].map(definicion);

  return (
    <div className="flex flex-col gap-8">
      <Panel className="flex flex-col gap-4">
        <InputField
          label="Contá qué hacés, en una frase"
          hint="Por ejemplo: «Vendo viandas por encargo y cobro una seña»."
          maxLength={200}
          autoComplete="off"
          value={frase}
          onChange={(evento) => setFrase(evento.target.value)}
        />
        <fieldset className="m-0 flex min-w-0 flex-col gap-2 border-0 p-0">
          <legend className="mb-1.5 p-0 text-base font-semibold text-fg">O elegí tu rubro</legend>
          <div className="flex flex-wrap gap-2">
            {RUBROS.map((id) => {
              const elegido = rubro === id;
              return (
                <button
                  key={id}
                  type="button"
                  aria-pressed={elegido}
                  onClick={() => setRubro(elegido ? null : id)}
                  className={cn(
                    'inline-flex min-h-12 cursor-pointer items-center gap-1.5 rounded-full border px-4 text-base font-semibold text-fg transition-colors',
                    elegido
                      ? 'border-accent bg-accent'
                      : 'border-fg/30 bg-surface hover:bg-fg/6 active:bg-fg/12',
                  )}
                >
                  {elegido && <CheckIcon aria-hidden size={18} weight="bold" />}
                  {NOMBRE_RUBRO[id]}
                </button>
              );
            })}
          </div>
        </fieldset>
      </Panel>

      {!hayAlgo ? (
        <p className="m-0 rounded-lg border border-dashed border-fg/30 p-5 text-body text-soft">
          Escribí una frase o elegí tu rubro y mirá cómo se arma tu sistema, bloque a bloque.
        </p>
      ) : (
        <>
          <section aria-labelledby="probar-bloques" className="flex flex-col gap-3">
            <h2 id="probar-bloques" className="m-0 text-h3">
              Esto se armaría para vos
            </h2>
            <ul
              aria-label="Bloques armados"
              className="m-0 grid list-none gap-3 p-0 sm:grid-cols-2"
            >
              {piezas.map((p, i) => (
                <TarjetaBloque
                  key={p.id}
                  id={p.id}
                  nombre={p.nombre}
                  motivo={p.motivo}
                  detalle={pieza(p.id).descripcion}
                  orden={i}
                />
              ))}
            </ul>
          </section>

          <section aria-labelledby="probar-respuestas" className="flex flex-col gap-3">
            <div className="flex flex-wrap items-center gap-3">
              <h2 id="probar-respuestas" className="m-0 text-h3">
                Tres respuestas de ejemplo
              </h2>
              <Badge tone="warn">Datos de ejemplo</Badge>
            </div>
            <p className="m-0 text-body text-soft">
              Los números son de muestra, no son de ningún negocio. Con tus datos, Ensambla te
              responde lo tuyo. Sumá un bloque para desbloquear una respuesta.
            </p>
            <ul className="m-0 grid list-none gap-3 p-0 sm:grid-cols-3" aria-live="polite">
              {respuestas.map((respuesta) => {
                const faltantes = faltan(respuesta.requiere, presentes);
                const lista = faltantes.length === 0;
                const muestra = muestraDe(respuesta.id, ensamblado.rubro);
                return (
                  <li key={respuesta.id} className="m-0 list-none">
                    <Panel
                      tight
                      className={cn(
                        'flex h-full flex-col gap-2',
                        !lista && 'border-dashed bg-canvas shadow-none',
                      )}
                    >
                      <h3 className="m-0 text-body font-bold">{respuesta.pregunta}</h3>
                      {lista ? (
                        <>
                          <p className="m-0 text-h3 font-bold break-words">{muestra.titular}</p>
                          <p className="m-0 text-hint text-soft">{muestra.detalle}</p>
                        </>
                      ) : (
                        <>
                          <p className="m-0 flex items-start gap-2 text-hint text-soft">
                            <LockSimpleIcon aria-hidden size={20} className="mt-0.5 shrink-0" />
                            Te falta {faltantes.length === 1 ? 'el bloque' : 'los bloques'}{' '}
                            {faltantes.map((id) => nombreDePieza(id, ensamblado.rubro)).join(' y ')}
                            .
                          </p>
                          <div className="flex flex-col gap-2">
                            {faltantes.map((id) => (
                              <Button
                                key={id}
                                variant="primary"
                                size="compact"
                                onClick={() =>
                                  setSumadas((actuales) =>
                                    actuales.includes(id) ? actuales : [...actuales, id],
                                  )
                                }
                              >
                                <PlusIcon aria-hidden size={18} />
                                Sumar {nombreDePieza(id, ensamblado.rubro)}
                              </Button>
                            ))}
                          </div>
                        </>
                      )}
                    </Panel>
                  </li>
                );
              })}
            </ul>
          </section>
        </>
      )}
    </div>
  );
}
