import { createFileRoute } from '@tanstack/react-router';
import { sesionQuery } from '@/hooks/auth/queries';
import { preferenciasQuery } from '@/hooks/preferencias/queries';
import { sistemasQuery } from '@/hooks/sistemas/queries';
import { zonaQuery } from '@/hooks/zona/queries';
import { PlanesLayout } from '@/layouts/PlanesLayout';

export const Route = createFileRoute('/_planes')({
  loader: async ({ context }) => {
    const usuario = await context.queryClient.fetchQuery(sesionQuery());
    if (usuario)
      await Promise.all([
        context.queryClient.ensureQueryData(sistemasQuery()),
        context.queryClient.ensureQueryData(preferenciasQuery()),
        context.queryClient.ensureQueryData(zonaQuery()),
      ]);
  },
  component: PlanesLayout,
});
