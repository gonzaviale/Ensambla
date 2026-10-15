import { Link } from '@tanstack/react-router';
import { ScalesIcon } from '@phosphor-icons/react';
import { InfoTip, Panel, buttonClasses } from '@/components/ui';

// La razón para elegir a Ensambla antes que a la app de un mayorista.
export function Neutralidad() {
  return (
    <Panel
      aria-labelledby="red-neutralidad"
      data-recorrido="red-neutralidad"
      className="flex flex-col gap-3"
    >
      <div className="flex items-start gap-4">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-accent-100 text-accent-800">
          <ScalesIcon aria-hidden size={26} weight="duotone" />
        </span>
        <div className="flex min-w-0 flex-col gap-1.5">
          <div className="flex items-center gap-1">
            <h2 id="red-neutralidad" className="m-0 text-h3">
              Estamos del lado del comercio
            </h2>
            <InfoTip tema="neutralidad">
              Ser neutrales quiere decir que nadie puede pagar para salir mejor parado en tus
              comparaciones. Un mayorista paga por llegar a más comercios, no por tapar a otro.
            </InfoTip>
          </div>
          <p className="m-0 text-body text-soft">
            Un mayorista paga por llegar a más comercios, nunca por esconder que otro vende más
            barato. Si pudiera pagar para que no te enteres, esta pantalla no serviría de nada.
          </p>
        </div>
      </div>
      <div>
        <Link to="/red/mayorista" className={buttonClasses({ variant: 'secondary' })}>
          Ver cómo lo ve un mayorista
        </Link>
      </div>
    </Panel>
  );
}
