import { ArrowsMergeIcon, ShieldCheckIcon, StorefrontIcon, TruckIcon } from '@phosphor-icons/react';
import { InfoTip, Panel } from '@/components/ui';
import { useBarrio, useUmbrales } from '@/hooks/red/useRed';

const PASOS = [
  {
    icono: TruckIcon,
    titulo: 'Los proveedores suben su lista una vez',
    texto: 'Por WhatsApp, PDF o Excel. No hace falta que se la manden a mano a cada comercio.',
  },
  {
    icono: ArrowsMergeIcon,
    titulo: 'Ensambla empareja y cruza',
    texto:
      'Reconoce que es el mismo producto aunque cada lista lo escriba distinto, y cruza costos, stock y ofertas.',
  },
  {
    icono: StorefrontIcon,
    titulo: 'Cada comercio recibe sus precios',
    texto: 'Con sus reglas de ganancia, sus avisos y las ofertas de lo que realmente vende.',
  },
] as const;

const numero = (valor: number) => valor.toLocaleString('es-AR');

// La idea de la red en una mirada: tres pasos, las cifras del barrio y la promesa de privacidad.
export function ComoFunciona() {
  const barrio = useBarrio();
  const { minimoComerciosZona } = useUmbrales();
  const cifras = [
    { valor: barrio.comercios, etiqueta: 'comercios' },
    { valor: barrio.proveedores, etiqueta: 'proveedores' },
    { valor: barrio.productosEmparejados, etiqueta: 'productos emparejados' },
    { valor: barrio.listasProcesadas, etiqueta: 'listas procesadas' },
  ];

  return (
    <section
      aria-labelledby="red-como-funciona"
      data-recorrido="red-como-funciona"
      className="flex flex-col gap-4"
    >
      <h2 id="red-como-funciona" className="m-0 text-h3">
        Cómo funciona la red
      </h2>

      <ol className="m-0 grid list-none gap-3 p-0 md:grid-cols-3">
        {PASOS.map((paso, indice) => {
          const Icono = paso.icono;
          return (
            <li key={paso.titulo} className="flex min-w-0">
              <Panel tight className="flex w-full flex-col gap-2.5">
                <span className="flex items-center gap-3">
                  <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-accent-100 text-accent-800">
                    <Icono aria-hidden size={26} weight="duotone" />
                  </span>
                  <span className="text-caption font-bold tracking-widest text-accent-800 uppercase">
                    Paso {indice + 1}
                  </span>
                </span>
                <h3 className="m-0 text-body font-bold">{paso.titulo}</h3>
                <p className="m-0 text-hint text-soft">{paso.texto}</p>
              </Panel>
            </li>
          );
        })}
      </ol>

      <Panel flat className="flex flex-col gap-4">
        <dl className="m-0 grid grid-cols-2 gap-4 lg:grid-cols-4">
          {cifras.map((cifra) => (
            <div key={cifra.etiqueta} className="flex min-w-0 flex-col-reverse gap-0.5">
              <dt className="text-hint text-soft">{cifra.etiqueta}</dt>
              <dd className="m-0 text-h2 font-extrabold">{numero(cifra.valor)}</dd>
            </div>
          ))}
        </dl>
        <p className="m-0 text-body font-semibold">
          Cada lista que reenviás mejora la red para todos.
        </p>
      </Panel>

      <Panel className="flex items-start gap-4" aria-labelledby="red-datos-tuyos">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-sand-100 text-fg">
          <ShieldCheckIcon aria-hidden size={26} weight="duotone" />
        </span>
        <div className="flex min-w-0 flex-col gap-1.5">
          <div className="flex items-center gap-1">
            <h3 id="red-datos-tuyos" className="m-0 text-body font-bold">
              Tus datos son tuyos
            </h3>
            <InfoTip tema={`mínimo de ${minimoComerciosZona} comercios`}>
              Una comparación de tu zona sólo aparece cuando hay al menos {minimoComerciosZona}{' '}
              comercios que compran ese producto. Con menos, alguien podría adivinar de quién es
              cada precio.
            </InfoTip>
          </div>
          <p className="m-0 text-body text-soft">
            Lo que comprás, lo que vendés y a qué precio nunca se le muestra a otro comercio. Las
            comparaciones son agregadas y anónimas, y sólo aparecen cuando hay {minimoComerciosZona}{' '}
            comercios o más de tu zona.
          </p>
        </div>
      </Panel>
    </section>
  );
}
