import { useState } from 'react';
import { CopyIcon, PrinterIcon } from '@phosphor-icons/react';
import { RequierePlan } from '@/components/Plan/PlanGate';
import { Button, Panel, Toast } from '@/components/ui';
import { pesos } from '@/lib/red/formato';
import type { CambioAplicado } from '@/types/red';

// Al imprimir sólo queda la zona de etiquetas: el resto de la pantalla (menús, avisos, botones) se oculta.
const ESTILO_IMPRESION = `
@media print {
  body * { visibility: hidden !important; }
  .zona-de-etiquetas, .zona-de-etiquetas * { visibility: visible !important; }
  .zona-de-etiquetas { position: absolute; top: 0; left: 0; width: 100%; padding: 0; }
  .zona-de-etiquetas .no-imprimir { display: none !important; }
}
`;

type EtiquetasProps = { cambios: CambioAplicado[]; nombreDelNegocio: string };

// Etiquetas listas para imprimir y la lista de cambios para copiar: parte del plan Comercio.
export function EtiquetasParaImprimir({ cambios, nombreDelNegocio }: EtiquetasProps) {
  return (
    <section
      aria-labelledby="etiquetas"
      data-recorrido="mostrador-etiquetas"
      className="flex flex-col gap-4"
    >
      <h2 id="etiquetas" className="m-0 text-h2">
        Etiquetas para imprimir
      </h2>
      <RequierePlan
        funcion="etiquetas"
        beneficio="Imprimí las etiquetas con el precio nuevo y copiá la lista de cambios."
      >
        <Etiquetas cambios={cambios} nombreDelNegocio={nombreDelNegocio} />
      </RequierePlan>
    </section>
  );
}

function Etiquetas({ cambios, nombreDelNegocio }: EtiquetasProps) {
  const [aviso, setAviso] = useState<{ tipo: 'exito' | 'error'; mensaje: string } | null>(null);

  if (cambios.length === 0) {
    return (
      <p className="m-0 text-body text-soft">
        Cuando haya precios para cambiar, acá vas a tener las etiquetas listas para imprimir.
      </p>
    );
  }

  async function copiar() {
    const lineas = cambios.map(
      (c) => `${c.nombre}: ${pesos(c.precioAnterior)} → ${pesos(c.precioNuevo)}`,
    );
    const texto = [`Precios para cambiar en ${nombreDelNegocio}`, '', ...lineas].join('\n');
    try {
      await navigator.clipboard.writeText(texto);
      setAviso({
        tipo: 'exito',
        mensaje: 'Copiamos la lista de cambios. Ya podés pegarla donde quieras.',
      });
    } catch {
      setAviso({
        tipo: 'error',
        mensaje: 'No pudimos copiar la lista. Probá imprimir las etiquetas.',
      });
    }
  }

  return (
    <>
      <div className="zona-de-etiquetas flex flex-col gap-4">
        <style>{ESTILO_IMPRESION}</style>
        <div className="no-imprimir flex flex-wrap gap-3 print:hidden">
          <Button variant="primary" onClick={() => window.print()}>
            <PrinterIcon aria-hidden size={22} />
            Imprimir las etiquetas
          </Button>
          <Button onClick={() => void copiar()}>
            <CopyIcon aria-hidden size={22} />
            Copiar la lista de cambios
          </Button>
        </div>

        <ul className="m-0 grid list-none grid-cols-1 gap-4 p-0 min-[480px]:grid-cols-2 lg:grid-cols-3 print:grid-cols-3">
          {cambios.map((cambio) => (
            <li key={cambio.id} className="break-inside-avoid">
              <Panel
                flat
                className="print:border-black print:text-black flex h-full flex-col items-center justify-between gap-3 border-2 border-dashed border-fg/50 bg-surface text-center"
              >
                <p className="m-0 text-h3 font-bold break-words">{cambio.nombre}</p>
                <p className="m-0 text-h1 font-extrabold print:text-display">
                  {pesos(cambio.precioNuevo)}
                </p>
              </Panel>
            </li>
          ))}
        </ul>
      </div>
      {aviso && <Toast tipo={aviso.tipo} mensaje={aviso.mensaje} onClose={() => setAviso(null)} />}
    </>
  );
}
