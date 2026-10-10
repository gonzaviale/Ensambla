import { LightbulbIcon } from '@phosphor-icons/react';
import { Panel } from '@/components/ui';

const CONSEJOS = [
  'Apoyá el cuaderno sobre una mesa, con buena luz y sin sombras.',
  'Que se vea la hoja entera y nada más alrededor.',
  'Sacá una foto por página, con el celular derecho y quieto.',
  'Si la letra se ve borrosa, sacala de nuevo.',
];

export function ConsejosFoto() {
  return (
    <Panel flat tight>
      <h2 className="m-0 flex items-center gap-2 text-[17px]">
        <LightbulbIcon aria-hidden size={22} className="text-accent-800" />
        Consejos para una buena foto
      </h2>
      <ul className="mt-2 mb-0 flex list-disc flex-col gap-1 pl-5 text-soft">
        {CONSEJOS.map((consejo) => (
          <li key={consejo}>{consejo}</li>
        ))}
      </ul>
    </Panel>
  );
}
