import { Link } from '@tanstack/react-router';

export function InvitacionConfigurarComercio({
  visible,
  onIr,
}: {
  visible: boolean;
  onIr?: () => void;
}) {
  if (!visible) return null;
  return (
    <aside className="ubicacion-aviso">
      <strong>Completá la ubicación y el rubro de tu comercio</strong>
      <p>Así queda preparada tu configuración para las comparaciones de zona.</p>
      <Link to="/ubicacion" className="text-link" onClick={onIr}>
        Configurar mi comercio
      </Link>
    </aside>
  );
}
