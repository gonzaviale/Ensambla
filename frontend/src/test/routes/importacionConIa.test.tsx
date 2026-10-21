import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { filasDeEjemplo } from '@/lib/sistema/lectura';
import { makeSystem } from '@/lib/sistema/core';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

// Con el backend (modo parcial o ninguno) la foto la lee la IA: acá se simula ese modo sin tocar el resto.
vi.mock('@/hooks/sistemas/useLecturaDeImagen', async (importarOriginal) => ({
  ...(await importarOriginal<Record<string, unknown>>()),
  LECTURA_CON_IA: true,
}));

// En este entorno File y Blob son los de Node, que el FileReader de jsdom no sabe leer.
class LectorDeArchivos {
  result: string | null = null;
  onload: (() => void) | null = null;
  readAsDataURL() {
    this.result = 'data:image/jpeg;base64,AAAA';
    queueMicrotask(() => this.onload?.());
  }
}

beforeEach(() => {
  vi.stubGlobal('FileReader', LectorDeArchivos);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

const abrirImportador = async (contexto: 'registros' | 'negocio' = 'registros') => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const sistema = makeSystem('Mi almacén', ['inventory']);
  const inventarioId = sistema.sections[0]?.blocks[0]?.id ?? '';
  estado.sistemas.set(usuario.email, [sistema]);
  renderApp(`/sistemas/${sistema.id}?modo=usar`);
  await screen.findByRole('heading', { level: 1, name: /Mi almacén/ }, ESPERA);
  act(() =>
    useSistemaStore
      .getState()
      .abrirModal({ tipo: 'importar', sistemaId: sistema.id, bloqueId: inventarioId, contexto }),
  );
  await screen.findByRole('button', { name: /Probar con un ejemplo/ });
  return userEvent.setup();
};

const subirFoto = async (user: ReturnType<typeof userEvent.setup>) => {
  const entrada = document.getElementById('photo-file') as HTMLInputElement;
  await user.upload(entrada, new File(['pixeles'], 'cuaderno.jpg', { type: 'image/jpeg' }));
};

describe('la lectura de una foto con IA', () => {
  it('avisa que la foto se envía a un servicio de IA y muestra lo que leyó, sin llamarlo simulado', async () => {
    let imagenEnviada = '';
    server.use(
      http.post('/api/importaciones/lectura', async ({ request }) => {
        const cuerpo = await request.formData();
        imagenEnviada = (cuerpo.get('imagen') as File).name;
        const pedido = JSON.parse(await (cuerpo.get('pedido') as File).text());
        return HttpResponse.json({
          filas: filasDeEjemplo(pedido.tipo, pedido.campos, {}).slice(0, 2),
          descripcion: '',
        });
      }),
    );
    const user = await abrirImportador();
    expect(screen.getByText('Lectura con inteligencia artificial')).toBeVisible();
    expect(screen.queryByText('Lectura de imágenes simulada')).toBeNull();

    await subirFoto(user);

    await screen.findByRole('heading', { name: 'Confirmar registros' }, ESPERA);
    expect(imagenEnviada).toBe('cuaderno.jpg');
    expect(screen.getByText('2 registros leídos de tu foto')).toBeVisible();
    expect(screen.queryByText(/lectura simulada/)).toBeNull();
    expect(screen.getByText('cuaderno.jpg')).toBeVisible();
  });

  it('«Probar con un ejemplo» sigue usando datos de muestra y lo dice', async () => {
    const user = await abrirImportador();

    await user.click(screen.getByRole('button', { name: /Probar con un ejemplo/ }));

    await screen.findByRole('heading', { name: 'Confirmar registros' }, ESPERA);
    expect(screen.getByText('3 registros de ejemplo · lectura simulada')).toBeVisible();
  });

  it('muestra el mensaje del servidor cuando la foto no es de ese bloque', async () => {
    setupServerError(
      'post',
      '/importaciones/lectura',
      400,
      'La estructura leída no es compatible con el bloque de destino',
    );
    const user = await abrirImportador();

    await subirFoto(user);

    expect(
      await screen.findByText(
        'La estructura leída no es compatible con el bloque de destino',
        {},
        ESPERA,
      ),
    ).toBeVisible();
    expect(screen.getByRole('button', { name: /Probar con un ejemplo/ })).toBeVisible();
  });

  it('lo que Sami entiende del negocio sigue siendo de muestra aunque haya IA', async () => {
    const user = await abrirImportador('negocio');
    expect(screen.queryByText('Lectura con inteligencia artificial')).toBeNull();

    await subirFoto(user);

    expect(await screen.findByText('Interpretación de ejemplo', {}, ESPERA)).toBeVisible();
  });
});
