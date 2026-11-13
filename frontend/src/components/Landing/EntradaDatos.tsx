import { useEffect, useRef, useState, type FormEvent } from 'react';
import { ArrowRightIcon } from '@phosphor-icons/react/dist/csr/ArrowRight';
import { MagnifyingGlassIcon } from '@phosphor-icons/react/dist/csr/MagnifyingGlass';
import { CheckCircleIcon } from '@phosphor-icons/react/dist/csr/CheckCircle';

const PASOS = [
  { id: 'papel', nombre: 'Lo tenés en papel' },
  { id: 'revisar', nombre: 'Revisás los datos' },
  { id: 'buscar', nombre: 'Lo encontrás' },
] as const;
type PasoId = (typeof PASOS)[number]['id'];
const PESOS = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});
const OTROS_FIADOS = [
  { nombre: 'Pedro Ruiz', importe: 1400 },
  { nombre: 'Lucía Pérez', importe: 5000 },
];
const normalizar = (valor: string) =>
  valor
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .trim()
    .toLocaleLowerCase('es');

// Recorrido ilustrativo con datos ficticios: no procesa fotos ni escribe en la cuenta.
export function EntradaDatos() {
  const [paso, setPaso] = useState<PasoId>('papel');
  const [importe, setImporte] = useState('3200');
  const [confirmado, setConfirmado] = useState(3200);
  const [busqueda, setBusqueda] = useState('Ana');
  const [error, setError] = useState('');
  const campoImporte = useRef<HTMLInputElement>(null);
  const campoBusqueda = useRef<HTMLInputElement>(null);
  const enfocarCampo = useRef(false);
  const fiados = [{ nombre: 'Ana López', importe: confirmado }, ...OTROS_FIADOS];
  const encontrados = fiados.filter((fiado) =>
    normalizar(fiado.nombre).includes(normalizar(busqueda)),
  );

  useEffect(() => {
    if (!enfocarCampo.current) return;
    const campo = paso === 'revisar' ? campoImporte.current : campoBusqueda.current;
    campo?.focus({ preventScroll: true });
    enfocarCampo.current = false;
  }, [paso]);

  const avanzar = (siguiente: PasoId) => {
    enfocarCampo.current = true;
    setPaso(siguiente);
  };

  const confirmar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const valor = Number(importe);
    if (!Number.isFinite(valor) || valor <= 0) {
      setError('Ingresá un importe mayor a cero.');
      campoImporte.current?.focus();
      return;
    }
    setError('');
    setConfirmado(valor);
    setBusqueda('Ana');
    avanzar('buscar');
  };

  return (
    <section
      className="ens-section ens-demo-section"
      id="como-funciona"
      aria-labelledby="ejemplo-titulo"
    >
      <div className="ens-wrap">
        <div className="ens-demo-intro">
          <p className="ens-eyebrow">De tu cuaderno a la información que necesitás</p>
          <h2 id="ejemplo-titulo">Consultá quién te debe y cuánto</h2>
          <p>
            En este ejemplo, Ana tiene un fiado de {PESOS.format(3200)}. Explorá cómo revisás el
            importe y encontrás la anotación por su nombre.
          </p>
        </div>
        <p className="ens-demo-guide">Explorá el ejemplo paso a paso con los botones.</p>
        <ol className="ens-steps" aria-label="Pasos del ejemplo">
          {PASOS.map(({ id, nombre }, indice) => (
            <li key={id}>
              <button
                type="button"
                className="ens-step"
                aria-pressed={paso === id}
                aria-controls="ejemplo-contenido"
                onClick={() => setPaso(id)}
              >
                <span className="ens-step-number" aria-hidden="true">
                  {indice + 1}
                </span>
                <span>
                  {nombre}
                  <span className="ens-step-action" aria-hidden="true">
                    Ver paso
                  </span>
                </span>
              </button>
            </li>
          ))}
        </ol>
        <div className="ens-example" id="ejemplo-contenido">
          <div className="ens-book" aria-label="Cuaderno de ejemplo con tres fiados">
            <p className="ens-hand ens-hand-head">Fiados del día</p>
            <p className="ens-hand">
              <span className="ens-circle">Ana</span> — {PESOS.format(3200)}
            </p>
            <p className="ens-hand">Pedro — {PESOS.format(1400)}</p>
            <p className="ens-hand">Lucía — {PESOS.format(5000)}</p>
            <p className="ens-paper-question">“¿Dónde lo había anotado?”</p>
          </div>
          <ArrowRightIcon className="ens-example-arrow" aria-hidden size={28} />
          <div className="ens-digital">
            <div className="ens-digital-head">
              <span>Mi cuaderno · Fiados</span>
              <span className="ens-demo-badge">Ejemplo</span>
            </div>
            <p className="ens-demo-progress" aria-live="polite">
              Paso {PASOS.findIndex(({ id }) => id === paso) + 1} de {PASOS.length}
            </p>
            {paso === 'papel' && (
              <div className="ens-demo-start">
                <p className="ens-example-question">
                  Encontrá el dato que necesitás <strong>sin recorrer todas las hojas</strong>
                </p>
                <p>
                  Empezá con una foto de tus anotaciones y revisá los datos antes de guardarlos.
                </p>
                <button type="button" className="ens-secondary" onClick={() => avanzar('revisar')}>
                  Comenzar el ejemplo <ArrowRightIcon aria-hidden size={19} />
                </button>
                <p className="ens-demo-next-hint">Seleccioná el botón para continuar al paso 2.</p>
              </div>
            )}
            {paso === 'revisar' && (
              <form className="ens-review-form" onSubmit={confirmar} noValidate>
                <p>Revisá los datos y, si lo necesitás, corregí el importe antes de confirmar.</p>
                <div className="ens-example-person">
                  <span>Cliente</span>
                  <strong>Ana López</strong>
                </div>
                <label htmlFor="ejemplo-importe">Importe del fiado</label>
                <div className="ens-money-input">
                  <span aria-hidden="true">$</span>
                  <input
                    ref={campoImporte}
                    id="ejemplo-importe"
                    name="importe-ejemplo"
                    type="number"
                    inputMode="decimal"
                    min="0.01"
                    step="any"
                    required
                    value={importe}
                    aria-invalid={Boolean(error)}
                    aria-describedby={error ? 'ejemplo-error' : undefined}
                    onChange={(evento) => {
                      setImporte(evento.target.value);
                      setError('');
                    }}
                  />
                </div>
                {error && (
                  <p className="ens-example-error" id="ejemplo-error" role="alert">
                    {error}
                  </p>
                )}
                <button className="ens-secondary" type="submit">
                  Confirmar y buscar <CheckCircleIcon aria-hidden size={20} />
                </button>
              </form>
            )}
            {paso === 'buscar' && (
              <div className="ens-demo-search">
                <label htmlFor="ejemplo-busqueda">Buscá un nombre en este ejemplo</label>
                <div className="ens-search-input">
                  <MagnifyingGlassIcon aria-hidden size={20} />
                  <input
                    ref={campoBusqueda}
                    id="ejemplo-busqueda"
                    name="buscar-ejemplo"
                    type="search"
                    placeholder="Por ejemplo, Ana…"
                    value={busqueda}
                    autoComplete="off"
                    onChange={(evento) => setBusqueda(evento.target.value)}
                  />
                </div>
                <ul className="ens-example-results" aria-label="Fiados encontrados">
                  {encontrados.map((fiado) => (
                    <li key={fiado.nombre}>
                      <div>
                        <strong>{fiado.nombre}</strong>
                        <span>Fiado registrado</span>
                      </div>
                      <b>{PESOS.format(fiado.importe)}</b>
                    </li>
                  ))}
                </ul>
                <p className="ens-search-status" role="status">
                  {encontrados.length === 0
                    ? 'No hay coincidencias. Buscá a Ana, Pedro o Lucía.'
                    : encontrados.length === 1
                      ? 'Encontraste el fiado con el nombre y el importe que confirmaste.'
                      : encontrados.length +
                        ' fiados de ejemplo. Escribí un nombre para encontrar el que buscás.'}
                </p>
              </div>
            )}
          </div>
        </div>
        <p className="ens-example-disclaimer" id="ejemplo-nota">
          Este ejemplo usa datos ficticios y no guarda información; al leer una foto, los datos
          pueden necesitar correcciones.
        </p>
      </div>
    </section>
  );
}
