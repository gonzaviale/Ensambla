import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';
import { TallerPage } from '@/pages/Taller/TallerPage';

// `?sistema=<id>`: «Ajustar con Sami» sobre un sistema que ya existe. Sin él, se arma uno nuevo.
const busquedaSchema = z.object({
  sistema: z.string().min(1).optional().catch(undefined),
});

export const Route = createFileRoute('/_app/taller')({
  validateSearch: busquedaSchema,
  component: TallerPage,
});
