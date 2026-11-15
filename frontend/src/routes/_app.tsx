import { createFileRoute } from '@tanstack/react-router';
import { requerirUsuario } from '@/config/guards';
import { preferenciasQuery } from '@/hooks/preferencias/queries';
import { negociosQuery } from '@/hooks/negocios/queries';
import { sistemasQuery } from '@/hooks/sistemas/queries';
import { zonaQuery } from '@/hooks/zona/queries';
import { SistemaLayout } from '@/layouts/SistemaLayout';

export const Route = createFileRoute('/_app')({
  beforeLoad: requerirUsuario,
  // Se traen antes de mostrar nada, así el inicio no aparece vacío por un instante.
  loader: ({ context }) =>
    Promise.all([
      context.queryClient.ensureQueryData(sistemasQuery()),
      context.queryClient.ensureQueryData(preferenciasQuery()),
      context.queryClient.ensureQueryData(zonaQuery()),
      context.queryClient.ensureQueryData(negociosQuery()),
    ]),
  component: SistemaLayout,
});
