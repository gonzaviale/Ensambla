import { HandshakeIcon, ScalesIcon } from '@phosphor-icons/react';
import { Badge, Panel } from '@/components/ui';

// Las reglas del juego para el mayorista y el modelo de negocio (hipótesis a validar).
export function ReglasDeOro() {
  return (
    <section aria-labelledby="mayorista-reglas" className="flex flex-col gap-4">
      <h2 id="mayorista-reglas" className="m-0 text-h3">
        Reglas de oro
      </h2>

      <div className="grid gap-3 md:grid-cols-2">
        <Panel className="flex flex-col gap-2">
          <span className="flex size-12 items-center justify-center rounded-full bg-accent-100 text-accent-800">
            <ScalesIcon aria-hidden size={26} weight="duotone" />
          </span>
          <h3 className="m-0 text-body font-bold">Neutralidad</h3>
          <p className="m-0 text-body text-soft">
            Pagás por llegar a más comercios, nunca por esconder información. Si otro proveedor
            vende más barato, el comercio lo ve igual.
          </p>
        </Panel>
        <Panel className="flex flex-col gap-2">
          <span className="flex size-12 items-center justify-center rounded-full bg-accent-100 text-accent-800">
            <HandshakeIcon aria-hidden size={26} weight="duotone" />
          </span>
          <h3 className="m-0 text-body font-bold">El mayorista no se elimina</h3>
          <p className="m-0 text-body text-soft">
            Un mayorista no se elimina: se elimina el trabajo manual con la información. El comercio
            te sigue comprando a vos.
          </p>
        </Panel>
      </div>

      <Panel flat className="flex flex-col gap-2" aria-labelledby="mayorista-plan">
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
          <h3 id="mayorista-plan" className="m-0 text-body font-bold">
            Plan para mayoristas
          </h3>
          <Badge tone="warn">Hipótesis a validar</Badge>
        </div>
        <p className="m-0 text-body text-soft">
          Pensamos una suscripción para publicar listas y ofertas, o un costo por cada oferta
          enviada. Todavía lo estamos validando con mayoristas reales: no hay precios definidos.
        </p>
      </Panel>
    </section>
  );
}
