import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';
import { SECCIONES_DE_CUENTA } from '@/lib/cuenta';
import { CuentaPage } from '@/pages/Cuenta/CuentaPage';

// `?seccion=`: qué parte de la configuración se ve. Sin ella (o con una que no existe), el perfil.
const busquedaSchema = z.object({
  seccion: z.enum(SECCIONES_DE_CUENTA).optional().catch(undefined),
});

export const Route = createFileRoute('/_app/cuenta')({
  validateSearch: busquedaSchema,
  component: CuentaPage,
});
