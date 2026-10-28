import { z } from 'zod';
import { modoApiElegido, modoMocksEfectivo } from '@/lib/modoApi';

// completo: todo lo atiende MSW. parcial: MSW solo atiende lo que el backend todavía no tiene.
// ninguno: todo va al backend real.
const modoMocksSchema = z.enum(['completo', 'parcial', 'ninguno']).default('ninguno');

const envSchema = z.object({
  VITE_API_URL: z.string().default('/api'),
  VITE_API_MOCKS: modoMocksSchema,
});

const parsed = envSchema.parse(import.meta.env);

export type ModoMocks = typeof parsed.VITE_API_MOCKS;

export const env = {
  apiUrl: parsed.VITE_API_URL,
  // El interruptor del header (mock / real) manda sobre lo compilado: ver lib/modoApi.
  mocks: modoMocksEfectivo(parsed.VITE_API_MOCKS, modoApiElegido()),
};
