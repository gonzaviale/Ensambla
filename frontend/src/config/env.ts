import { z } from 'zod';

// completo: todo lo atiende MSW. parcial: MSW solo atiende lo que el backend todavía no tiene.
// ninguno: todo va al backend real.
const modoMocksSchema = z.enum(['completo', 'parcial', 'ninguno']).default('ninguno');

const envSchema = z.object({
  VITE_API_URL: z.string().default('/api'),
  VITE_API_MOCKS: modoMocksSchema,
  // `si` (npm run dev:demo): la cuenta simulada arranca con el almacén cargado y Premium.
  VITE_DEMO_CARGADA: z.enum(['si', 'no']).default('no'),
});

const parsed = envSchema.parse(import.meta.env);

export type ModoMocks = typeof parsed.VITE_API_MOCKS;

export const env = {
  apiUrl: parsed.VITE_API_URL,
  mocks: parsed.VITE_API_MOCKS,
  demoCargada: parsed.VITE_DEMO_CARGADA === 'si',
};
