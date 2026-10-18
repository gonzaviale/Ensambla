import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Providers } from '@/config/providers';
import { env } from '@/config/env';
import '@/styles/index.css';

async function iniciar() {
  if (env.mocks !== 'ninguno') {
    const { iniciarMocks } = await import('@/mocks/browser');
    await iniciarMocks();
  }

  const root = document.getElementById('root');
  if (!root) throw new Error('Falta el elemento #root');

  createRoot(root).render(
    <StrictMode>
      <Providers />
    </StrictMode>,
  );
}

void iniciar();
