import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { estado, guardarObjetivosDe } from '@/mocks/db/estado';
import { ID_OBJETIVO_DE_EJEMPLO } from '@/lib/objetivos';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupObjetivoDeEjemplo,
  setupServerError,
  setupUsuarioConSesion,
} from '@/test/testHelpers';

const DETALLE = `/objetivos/${ID_OBJETIVO_DE_EJEMPLO}`;

const conObjetivoDeEjemplo = () => {
  const usuario = setupUsuarioConSesion();
  setupObjetivoDeEjemplo(usuario.email);
  return usuario;
};

const minis = async () =>
  within(await screen.findByRole('region', { name: 'Tus 3 mini-objetivos' }, ESPERA));

describe('la semana de un objetivo que se autogestiona', () => {
  it('con seis semanas simuladas muestra los 3 minis, el gráfico con las decisiones y los reajustes con su motivo', async () => {
    conObjetivoDeEjemplo();
    renderApp(DETALLE);

    const lista = await minis();
    expect(await lista.findAllByRole('article')).toHaveLength(3);
    expect(
      screen.getByText(/Si cumplís estos mini-objetivos sumás cerca de \$[\d.]+ por mes/),
    ).toBeVisible();
    expect(lista.getByRole('heading', { name: 'Subile $250 a Yerba 1 kg' })).toBeVisible();
    expect(lista.getByRole('heading', { name: 'Mové 6 Fernet por semana' })).toBeVisible();

    const semana = within(screen.getByRole('region', { name: 'Cómo te fue cada semana' }));
    expect(
      await semana.findByRole('img', { name: /meta de \$500\.000/ }, ESPERA),
    ).toBeInTheDocument();
    expect(within(semana.getByRole('table')).getAllByRole('row')).toHaveLength(8);
    expect(
      semana.getByText(/Aplicaste: Subí el precio de Yerba 1 kg/, { selector: 'strong' }),
    ).toBeVisible();
    expect(semana.getByText(/Después vendiste \$4\.500 más por semana/)).toBeVisible();
    expect(semana.getByText(/Te faltan \$295\.000 para tu meta/)).toBeVisible();
    const verTodas = semana.getByRole('button', { name: 'Ver todas las semanas' });
    expect(verTodas).toHaveAttribute('aria-expanded', 'false');
    await userEvent.click(verTodas);
    expect(semana.getByRole('button', { name: 'Ver solo las últimas' })).toHaveAttribute(
      'aria-expanded',
      'true',
    );
    const recorrido = document.getElementById('recorrido-semanas') as HTMLElement;
    recorrido.scrollTop = 200;
    await userEvent.click(semana.getByRole('button', { name: 'Ver solo las últimas' }));
    expect(recorrido.scrollTop).toBe(0);

    const cambios = within(screen.getByRole('region', { name: 'Qué cambió Sami' }));
    expect(
      await cambios.findByText(/Ahora el objetivo es \$16\.000 por semana/, {}, ESPERA),
    ).toBeVisible();
    expect(
      cambios.getByText(/no avanza\. Lo cambiamos por «Mové 6 Fernet por semana»/),
    ).toBeVisible();
    expect(screen.queryByText(/Camino combinado/)).not.toBeInTheDocument();
  });

  it('avisa sin molestar que el resumen del domingo está listo y no lo repite una vez visto', async () => {
    conObjetivoDeEjemplo();
    renderApp(DETALLE);
    const user = userEvent.setup();

    const banda = await screen.findByText('Tu resumen de la semana está listo.', {}, ESPERA);
    await user.click(screen.getByRole('button', { name: 'Ver resumen' }));

    expect(banda).not.toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Cómo te fue cada semana' })).toHaveFocus();
  });

  it('desde la tarjeta del objetivo se llega a su semana, con el chip de resumen listo', async () => {
    conObjetivoDeEjemplo();
    const router = renderApp('/objetivos');
    const user = userEvent.setup();

    expect(await screen.findByText('Resumen de la semana listo', {}, ESPERA)).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Ver mi semana' }));

    await waitFor(() => expect(router.state.location.pathname).toBe(DETALLE));
    await user.click(await screen.findByRole('button', { name: 'Listo' }, ESPERA));
    expect(screen.queryByText('Tu resumen de la semana está listo.')).not.toBeInTheDocument();
  });

  it('«Lo hago» queda anotado como decisión de la semana', async () => {
    conObjetivoDeEjemplo();
    renderApp(DETALLE);
    const user = userEvent.setup();

    const tarjeta = within((await (await minis()).findAllByRole('article'))[0] as HTMLElement);
    await user.click(tarjeta.getByRole('button', { name: 'Lo hago' }));

    expect(await tarjeta.findByText('Esta semana dijiste que lo hacés', {}, ESPERA)).toBeVisible();
    const decisiones = estado.autogestion.get(ID_OBJETIVO_DE_EJEMPLO)?.decisiones ?? [];
    expect(decisiones.at(-1)).toMatchObject({
      tipo: 'PASO_ACEPTADO',
      descripcion: 'Subile $250 a Yerba 1 kg',
    });
  });

  it('lo que dejó para después se restaura y puede volver a decir «Lo hago»', async () => {
    conObjetivoDeEjemplo();
    renderApp(DETALLE);
    const user = userEvent.setup();

    const tarjeta = within((await (await minis()).findAllByRole('article'))[0] as HTMLElement);
    expect(tarjeta.queryByRole('button', { name: /Cambiar|Quitar/ })).not.toBeInTheDocument();
    await user.click(tarjeta.getByRole('button', { name: 'Dejarlo para después' }));
    await user.click(
      await tarjeta.findByRole(
        'button',
        { name: /^Restaurar «.*» para volver a decidir$/ },
        ESPERA,
      ),
    );
    await user.click(tarjeta.getByRole('button', { name: 'Lo hago' }));

    expect(await tarjeta.findByText('Esta semana dijiste que lo hacés', {}, ESPERA)).toBeVisible();
    const decisiones = estado.autogestion.get(ID_OBJETIVO_DE_EJEMPLO)?.decisiones ?? [];
    expect(decisiones.slice(-2).map((decision) => decision.tipo)).toEqual([
      'PASO_IGNORADO',
      'PASO_ACEPTADO',
    ]);
  });

  it('el recuadro de Sami cuenta qué hace con el objetivo, no un «Sami está listo» genérico', async () => {
    conObjetivoDeEjemplo();
    renderApp(DETALLE);

    await minis();
    expect(screen.queryByText('Sami está listo')).not.toBeInTheDocument();
    expect(
      screen.getByText(/Sami sigue tu semana|Tu resumen está listo|Sami espera tus ventas/),
    ).toBeVisible();
  });

  it('si el servidor no trae los mini-objetivos lo explica sin romper la pantalla', async () => {
    conObjetivoDeEjemplo();
    setupServerError('get', `/objetivos/${ID_OBJETIVO_DE_EJEMPLO}/mini-objetivos`, 503, 'Caído');
    renderApp(DETALLE);

    expect(
      await screen.findByText(/No pudimos traer tus mini-objetivos/, {}, ESPERA),
    ).toBeVisible();
  });

  it('el objetivo de otra cuenta no se muestra', async () => {
    setupUsuarioConSesion();
    guardarObjetivosDe('otra@ensambla.test', [
      { id: 'ajeno', texto: 'Ajeno', meta: 100000, creadoEn: '2026-10-01T12:00:00' },
    ]);
    renderApp('/objetivos/ajeno');

    expect(
      await screen.findByRole('heading', { name: 'No encontramos ese objetivo' }, ESPERA),
    ).toBeVisible();
  });

  it('al abrir la semana y al volver a la pestaña mide el negocio y trae los mini-objetivos de nuevo', async () => {
    conObjetivoDeEjemplo();
    let mediciones = 0;
    let lecturasDeMinis = 0;
    const contarMinis = ({ request }: { request: Request }) => {
      if (request.url.endsWith(`/objetivos/${ID_OBJETIVO_DE_EJEMPLO}/mini-objetivos`))
        lecturasDeMinis++;
    };
    server.events.on('request:start', contarMinis);
    server.use(
      http.post('/api/objetivos/medir', () => {
        mediciones++;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderApp(DETALLE);
    await minis();
    await waitFor(() => expect(mediciones).toBe(1), ESPERA);
    await waitFor(() => expect(lecturasDeMinis).toBeGreaterThan(0), ESPERA);
    const lecturasAlAbrir = lecturasDeMinis;

    fireEvent(window, new Event('focus'));

    await waitFor(() => expect(mediciones).toBe(2), ESPERA);
    await waitFor(() => expect(lecturasDeMinis).toBeGreaterThan(lecturasAlAbrir), ESPERA);
    server.events.removeListener('request:start', contarMinis);
  });

  it('si medir falla, igual muestra lo último que midió Sami', async () => {
    conObjetivoDeEjemplo();
    setupServerError('post', '/objetivos/medir', 503, 'Caído');
    renderApp(DETALLE);

    expect(await (await minis()).findAllByRole('article')).toHaveLength(3);
  });
});
