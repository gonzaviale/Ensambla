import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';
import { SistemaPage } from '@/pages/Sistema/SistemaPage';

const busquedaSchema = z.object({
  modo: z.enum(['construir', 'usar']).catch('usar'),
});

export const Route = createFileRoute('/_app/sistemas/$sistemaId')({
  validateSearch: busquedaSchema,
  component: SistemaPage,
});
