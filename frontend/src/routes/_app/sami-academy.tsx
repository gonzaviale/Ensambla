import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';
import { LECCIONES_DE_ACADEMY } from '@/lib/academy';
import { SamiAcademyPage } from '@/pages/SamiAcademy/SamiAcademyPage';

// `?leccion=`: cuál se ve (la primera, si falta o no existe). `?objetivo=`: el objetivo con el que se prueba el escenario.
const busquedaSchema = z.object({
  leccion: z.enum(LECCIONES_DE_ACADEMY).catch('resultado'),
  objetivo: z.string().optional().catch(undefined),
});

export const Route = createFileRoute('/_app/sami-academy')({
  validateSearch: busquedaSchema,
  component: function SamiAcademyRoute() {
    const { leccion, objetivo } = Route.useSearch();
    return <SamiAcademyPage leccion={leccion} objetivoId={objetivo} />;
  },
});
