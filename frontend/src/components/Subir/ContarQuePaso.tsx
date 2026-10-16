import { useMemo, useState } from 'react';
import { Link, useNavigate } from '@tanstack/react-router';
import { CandadoPlan } from '@/components/Plan/PlanGate';
import { Button, InfoTip, Panel, TextareaField, Toast } from '@/components/ui';
import { useCargarRelato, type FilaConfirmada } from '@/hooks/negocio/useCargarRelato';
import { useMiNegocio } from '@/hooks/negocio/useMiNegocio';
import { usePlanActual } from '@/hooks/negocio/usePlan';
import { nombreDePieza } from '@/lib/catalogo/piezas';
import { plural } from '@/lib/formato';
import { entenderRelato, leerImporte } from '@/lib/relato/parser';
import { FilaEntendida, type FilaPorConfirmar } from './FilaEntendida';

type Aviso = {
  mensaje: string;
  tipo: 'exito' | 'error';
  accion?: { etiqueta: string; onClick: () => void };
};

const EJEMPLO = 'Vendí 3 velas a 4500. Gasté 12000 en cera. Le fié a Marta 3000';

const unir = (nombres: string[]) =>
  nombres.length <= 1
    ? (nombres[0] ?? '')
    : `${nombres.slice(0, -1).join(', ')} y ${nombres.at(-1)}`;

// «Contá qué pasó»: la persona escribe (o dicta) lo que pasó, Ensambla lo ordena en cosas simples y
// ella confirma antes de que se guarde nada.
export function ContarQuePaso() {
  const negocio = useMiNegocio();
  const plan = usePlanActual();
  const navigate = useNavigate();
  const cargar = useCargarRelato(negocio.sistemaId);

  const [texto, setTexto] = useState('');
  const [errorTexto, setErrorTexto] = useState<string>();
  const [items, setItems] = useState<FilaPorConfirmar[]>([]);
  const [sinEntender, setSinEntender] = useState<string[]>([]);
  const [entendido, setEntendido] = useState(false);
  const [aviso, setAviso] = useState<Aviso | null>(null);

  const { presentes, sistema } = negocio;

  const analisis = useMemo(() => {
    const montos = new Map(items.map((item) => [item.id, leerImporte(item.monto)]));
    const sinPieza = items.filter((item) => !presentes.has(item.fila.pieza));
    const faltantes = [...new Set(sinPieza.map((item) => item.fila.pieza))];
    const bloquesQueTiene = sistema?.bloques.length ?? 0;
    const limite = plan.incluye('bloques-ilimitados')
      ? Number.POSITIVE_INFINITY
      : (plan.definicion.bloques ?? Number.POSITIVE_INFINITY);
    const espacio = Math.max(0, limite - bloquesQueTiene);
    const sumables = faltantes.slice(0, espacio);
    const bloqueadas = faltantes.slice(espacio);
    const sinMonto = items.some((item) => montos.get(item.id) === null);
    const guardables = items.filter(
      (item) => presentes.has(item.fila.pieza) || sumables.includes(item.fila.pieza),
    );
    return { montos, sinPieza, sumables, bloqueadas, sinMonto, guardables };
  }, [items, presentes, sistema, plan]);

  function entender() {
    if (!texto.trim()) {
      setErrorTexto(`Escribí qué pasó, por ejemplo: ${EJEMPLO}`);
      return;
    }
    setErrorTexto(undefined);
    setAviso(null);
    const resultado = entenderRelato(texto);
    const marca = Date.now();
    setItems(
      resultado.filas.map((fila, indice) => ({
        id: `${marca}-${indice}`,
        fila,
        monto: String(fila.valores.monto ?? ''),
      })),
    );
    setSinEntender(resultado.sinEntender);
    setEntendido(true);
  }

  function quitar(item: FilaPorConfirmar) {
    const indice = items.findIndex((otro) => otro.id === item.id);
    setItems((actuales) => actuales.filter((otro) => otro.id !== item.id));
    setAviso({
      mensaje: 'Quité una cosa de la lista.',
      tipo: 'exito',
      accion: {
        etiqueta: 'Deshacer',
        onClick: () => {
          setItems((actuales) => {
            const copia = [...actuales];
            copia.splice(Math.min(indice, copia.length), 0, item);
            return copia;
          });
          setAviso(null);
        },
      },
    });
  }

  async function guardar() {
    const filas: FilaConfirmada[] = analisis.guardables.map((item) => ({
      id: item.id,
      fila: item.fila,
      monto: analisis.montos.get(item.id) ?? 0,
    }));
    if (filas.length === 0) return;
    try {
      const resultado = await cargar.mutateAsync({
        filas,
        presentes,
        sumar: analisis.sumables,
      });
      const guardadas = new Set(resultado.guardadas);
      const quedan = items.filter((item) => !guardadas.has(item.id));
      setItems(quedan);
      if (quedan.length === 0) {
        setTexto('');
        setSinEntender([]);
        setEntendido(false);
      }
      if (resultado.fallidas.length > 0) {
        setAviso({
          mensaje: `No pude guardar ${plural(resultado.fallidas.length, 'cosa', 'cosas')}. Probá de nuevo.`,
          tipo: 'error',
        });
      } else if (resultado.guardadas.length > 0) {
        setAviso({
          mensaje: `Listo, guardé ${plural(resultado.guardadas.length, 'cosa', 'cosas')}`,
          tipo: 'exito',
          accion: {
            etiqueta: 'Ver mis respuestas',
            onClick: () => void navigate({ to: '/respuestas' }),
          },
        });
      }
    } catch {
      setAviso({ mensaje: 'No pude guardar. Probá de nuevo en un rato.', tipo: 'error' });
    }
  }

  const hayItems = items.length > 0;
  const nombresASumar = analisis.sumables.map((id) => nombreDePieza(id));
  const nombresBloqueados = analisis.bloqueadas.map((id) => nombreDePieza(id));
  const etiquetaGuardar =
    nombresASumar.length > 0
      ? `Sumar ${unir(nombresASumar)} y guardar`
      : `Guardar ${plural(analisis.guardables.length, 'cosa', 'cosas')}`;

  return (
    <Panel
      data-recorrido="subir-contar"
      aria-labelledby="subir-contar-titulo"
      className="flex flex-col gap-5"
    >
      <div className="flex flex-wrap items-center gap-x-2">
        <h2 id="subir-contar-titulo" className="m-0 text-h3">
          Contá qué pasó
        </h2>
        <InfoTip tema="qué entiende Ensambla">
          Entiende ventas, gastos y fiados. Por ejemplo: «Vendí 3 velas a 4500», «Gasté 12000 en
          cera» o «Le fié a Marta 3000». Si algo no lo entiende, te lo muestra aparte.
        </InfoTip>
      </div>

      <form
        className="flex flex-col gap-4"
        onSubmit={(event) => {
          event.preventDefault();
          entender();
        }}
      >
        <TextareaField
          label="¿Qué pasó en tu negocio?"
          placeholder={EJEMPLO}
          hint="Podés dictar con el micrófono del teclado del celular."
          value={texto}
          onChange={(event) => setTexto(event.target.value)}
          error={errorTexto}
          rows={5}
        />
        <div>
          <Button type="submit" variant={hayItems ? 'primary' : 'solid'}>
            {hayItems ? 'Entender de nuevo' : 'Entender'}
          </Button>
        </div>
      </form>

      {entendido && (
        <div className="flex flex-col gap-4" aria-live="polite">
          {hayItems && (
            <>
              <h3 className="m-0 text-body font-bold">Entendí esto. ¿Está bien?</h3>
              <ul className="m-0 flex list-none flex-col gap-3 p-0">
                {items.map((item) => (
                  <FilaEntendida
                    key={item.id}
                    item={item}
                    montoValido={analisis.montos.get(item.id) ?? null}
                    sinPieza={analisis.sinPieza.some((otro) => otro.id === item.id)}
                    onMonto={(monto) =>
                      setItems((actuales) =>
                        actuales.map((otro) => (otro.id === item.id ? { ...otro, monto } : otro)),
                      )
                    }
                    onQuitar={() => quitar(item)}
                  />
                ))}
              </ul>
            </>
          )}

          {sinEntender.length > 0 && (
            <div className="flex flex-col gap-2 rounded-lg border border-line bg-canvas p-4">
              <h3 className="m-0 text-body font-bold">No lo entendí</h3>
              <ul className="m-0 flex list-none flex-col gap-1 p-0 text-body text-soft">
                {sinEntender.map((frase) => (
                  <li key={frase}>«{frase}»</li>
                ))}
              </ul>
              <p className="m-0 text-hint text-soft">
                No pasa nada: eso no se guarda. Probá escribirlo así: «Vendí 2 tortas a 8000».
              </p>
            </div>
          )}

          {!hayItems && sinEntender.length === 0 && (
            <p className="m-0 text-body text-soft">
              No queda nada para guardar. Podés escribir otra cosa arriba.
            </p>
          )}

          {nombresASumar.length > 0 && (
            <p className="m-0 text-body text-soft">
              Todavía no tenés <strong>{unir(nombresASumar)}</strong>. Lo sumamos y guardamos todo
              junto.
            </p>
          )}

          {nombresBloqueados.length > 0 && (
            <CandadoPlan
              funcion="bloques-ilimitados"
              beneficio={`Para guardar lo de ${unir(nombresBloqueados)} necesitás sumar ese bloque. Mientras tanto guardamos lo que sí tiene su lugar.`}
            />
          )}

          {hayItems && (
            <div className="flex flex-col items-start gap-2">
              <Button
                variant="solid"
                loading={cargar.isPending}
                disabled={analisis.sinMonto || analisis.guardables.length === 0}
                onClick={() => void guardar()}
              >
                {etiquetaGuardar}
              </Button>
              {analisis.sinMonto && (
                <p className="m-0 text-hint text-soft">
                  Corregí los montos marcados para poder guardar.
                </p>
              )}
              <p className="m-0 text-hint text-soft">
                Nada se guarda hasta que apretás este botón.{' '}
                <Link to="/respuestas" className="font-semibold">
                  Ver mis respuestas
                </Link>
              </p>
            </div>
          )}
        </div>
      )}

      {aviso && (
        <Toast
          mensaje={aviso.mensaje}
          tipo={aviso.tipo}
          accion={aviso.accion}
          onClose={() => setAviso(null)}
        />
      )}
    </Panel>
  );
}
