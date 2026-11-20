import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';
import { SistemaPage } from '@/pages/Sistema/SistemaPage';

const busquedaSchema = z.object({
  modo: z.enum(['construir', 'usar']).catch('usar'),
  escanear: z.string().trim().min(1).max(200).optional().catch(undefined),
});

export const Route = createFileRoute('/_app/sistemas/$sistemaId')({
  validateSearch: busquedaSchema,
  component: SistemaPage,
});
