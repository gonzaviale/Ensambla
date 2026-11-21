import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';
import { requerirUsuario } from '@/config/guards';
import { ElegirNegocioPage } from '@/pages/Negocios/ElegirNegocioPage';

const searchSchema = z.object({ redirect: z.string().optional() });

// Afuera de `_app` a propósito: acá todavía no se cargan los datos de ningún negocio. Se entra a la app recién
// cuando quedó activo el negocio elegido (T-204).
export const Route = createFileRoute('/elegir-negocio')({
  validateSearch: searchSchema,
  beforeLoad: requerirUsuario,
  component: ElegirNegocioPage,
});
