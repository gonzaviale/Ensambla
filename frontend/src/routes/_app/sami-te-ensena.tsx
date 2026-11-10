import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';
import { LECCIONES_DE_SAMI } from '@/lib/ensenanza';
import { SamiEnsenaPage } from '@/pages/SamiEnsena/SamiEnsenaPage';

// `?leccion=`: cuál se ve (la primera, si falta o no existe). `?objetivo=`: el objetivo con el que se prueba el escenario.
const busquedaSchema = z.object({
  leccion: z.enum(LECCIONES_DE_SAMI).catch('resultado'),
  objetivo: z.string().optional().catch(undefined),
});

export const Route = createFileRoute('/_app/sami-te-ensena')({
  validateSearch: busquedaSchema,
  component: function SamiEnsenaRoute() {
    const { leccion, objetivo } = Route.useSearch();
    return <SamiEnsenaPage leccion={leccion} objetivoId={objetivo} />;
  },
});
