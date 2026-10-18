import type { NombreDeIcono } from './iconos';

export function Icon({ name }: { name: NombreDeIcono }) {
  return (
    <svg className="icon" aria-hidden="true">
      <use href={`#i-${name}`} />
    </svg>
  );
}
