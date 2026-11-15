import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { estado, objetivosDe } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

const hablar = async (user: ReturnType<typeof userEvent.setup>, texto: string) => {
  await user.type(await screen.findByLabelText('Contale tu objetivo a Sami', {}, ESPERA), texto);
  await user.click(screen.getByRole('button', { name: /Hablar con Sami/ }));
};

const EL_MES = () => `${new Date().toISOString().slice(0, 7)}`;

describe('definir un objetivo conversando con Sami', () => {
  it('con una frase propone mini-objetivos que se leen antes de guardar y muestra la cuota', async () => {
    setupUsuarioConSesion();
    renderApp('/objetivos');
    const user = userEvent.setup();

    expect(
      await screen.findByRole('heading', { name: 'Definí tu primer objetivo' }, ESPERA),
    ).toBeVisible();
    await hablar(user, 'Quiero vender más y que me conozcan en el barrio');

    const lectura = within(
      await screen.findByRole('region', { name: 'Lo que entendió Sami' }, ESPERA),
    );
    expect(lectura.getByText('Quiero vender más y que me conozcan en el barrio')).toBeVisible();
    expect(lectura.getByText('Una parte la mide Sami y otra me la contás vos')).toBeVisible();
    expect(lectura.getByText(/Se apoyó en: ventas, margen, inventario, gastos\./)).toBeVisible();
    expect(lectura.getByText(/Sin datos de: zona\./)).toBeVisible();
    expect(
      screen.getAllByRole('listitem').filter((item) => item.className === 'sami-mini'),
    ).toHaveLength(3);
    expect(screen.getByText('Vender un 10 % más por semana')).toBeVisible();
    expect(screen.getByText('Lo mide Sami')).toBeVisible();
    expect(screen.getAllByText('Me lo contás vos')).toHaveLength(2);
    expect(screen.getByText('Te quedan 5 de 5 objetivos con IA este mes')).toBeVisible();
    // Todavía no se guardó nada ni se gastó cuota.
    expect(objetivosDe(estado.usuarios.values().next().value?.email ?? '')).toHaveLength(0);
  });

  it('se puede quitar y retocar un mini-objetivo antes de guardar; al guardar no se inventa una meta', async () => {
    const usuario = setupUsuarioConSesion();
    renderApp('/objetivos');
    const user = userEvent.setup();
    await hablar(user, 'Quiero vender más y que me conozcan en el barrio');
    await screen.findByRole('region', { name: 'Lo que entendió Sami' }, ESPERA);

    await user.click(screen.getByRole('button', { name: /Quitar «Poné un cartel en la vereda/ }));
    await user.click(
      screen.getByRole('button', { name: /Editar «Pedile a 3 clientes que te recomienden/ }),
    );
    const titulo = screen.getByLabelText('Qué vas a hacer');
    await user.clear(titulo);
    await user.type(titulo, 'Pedile a 5 clientes que te recomienden');
    await user.click(screen.getByRole('button', { name: 'Listo' }));
    await user.click(screen.getByRole('button', { name: 'Guardar objetivo' }));

    await screen.findByRole('heading', { name: 'Cómo van tus objetivos' }, ESPERA);
    const [guardado] = objetivosDe(usuario.email);
    expect(guardado).toMatchObject({
      texto: 'Quiero vender más y que me conozcan en el barrio',
      tipo: 'HIBRIDO',
      meta: null,
      armadoConSami: true,
    });
    // Un objetivo sin importe no recibe una meta falsa: la tarjeta no habla de pesos ni de lo que falta.
    expect(screen.queryByText(/\$500\.000/)).not.toBeInTheDocument();
    expect(screen.queryByText(/te faltan/)).not.toBeInTheDocument();
    const mini = await screen.findByRole('list', { name: 'Mini-objetivos de esta semana' }, ESPERA);
    expect(within(mini).getAllByRole('listitem')).toHaveLength(2);
    expect(within(mini).getByText('Pedile a 5 clientes que te recomienden')).toBeVisible();
    expect(estado.objetivosConIa.get(`${usuario.email}|${EL_MES()}`)).toBe(1);
  });

  it('una acción sin datos no puede afirmar un importe ni un porcentaje', async () => {
    setupUsuarioConSesion();
    renderApp('/objetivos');
    const user = userEvent.setup();
    await hablar(user, 'Quiero que más gente conozca mi local');
    await screen.findByRole('region', { name: 'Lo que entendió Sami' }, ESPERA);

    await user.click(
      screen.getByRole('button', { name: /Editar «Pedile a 3 clientes que te recomienden/ }),
    );
    await user.type(screen.getByLabelText('Para qué'), ' y vender 20 %');

    expect(screen.getByRole('alert')).toHaveTextContent('Sin importes ni porcentajes');
    expect(screen.getByRole('button', { name: 'Listo' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Guardar objetivo' })).toBeDisabled();
  });

  it('una meta en pesos se define con el formulario de siempre, con lo que ya escribió', async () => {
    setupUsuarioConSesion();
    renderApp('/objetivos');
    const user = userEvent.setup();
    await hablar(user, 'Quiero ganar $400.000 por mes');

    await user.click(await screen.findByRole('button', { name: 'Poner mi meta en pesos' }, ESPERA));

    expect(screen.getByLabelText('Contale tu objetivo a Sami')).toHaveValue(
      'Quiero ganar $400.000 por mes',
    );
    expect(screen.getByLabelText(/Tu meta/)).toHaveValue('400.000');
    expect(screen.queryByRole('button', { name: 'Guardar objetivo' })).not.toBeInTheDocument();
  });

  it('si la IA no responde ofrece guardar como seguimiento manual, que no gasta cuota', async () => {
    const usuario = setupUsuarioConSesion();
    setupServerError('post', '/objetivos/sami/propuesta', 503, 'La IA no está disponible');
    renderApp('/objetivos');
    const user = userEvent.setup();
    await hablar(user, 'Quiero vender más');

    expect(await screen.findByText(/Sami no pudo responder ahora/, {}, ESPERA)).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Guardar objetivo' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Guardar como seguimiento manual' }));

    await screen.findByRole('heading', { name: 'Cómo van tus objetivos' }, ESPERA);
    expect(objetivosDe(usuario.email)).toMatchObject([
      { texto: 'Quiero vender más', tipo: 'SIN_DATOS', meta: null, armadoConSami: false },
    ]);
    expect(estado.objetivosConIa.size).toBe(0);
  });

  it('con la cuota agotada explica, lleva a Planes y deja guardar igual como seguimiento manual', async () => {
    const usuario = setupUsuarioConSesion();
    estado.objetivosConIa.set(`${usuario.email}|${EL_MES()}`, 5);
    const router = renderApp('/objetivos');
    const user = userEvent.setup();
    await hablar(user, 'Quiero vender más');

    expect(
      await screen.findByText(/Usaste todos los objetivos con IA de este mes/, {}, ESPERA),
    ).toBeVisible();
    expect(screen.getByText('Usaste los 5 objetivos con IA de este mes')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Guardar como seguimiento manual' })).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Ver planes' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/planes'));
  });

  it('con Premium la cuota es de 20', async () => {
    const usuario = setupUsuarioConSesion();
    estado.pruebasPremium.set(usuario.email, {
      inicioEn: new Date(Date.now() - 86_400_000).toISOString(),
      finEn: new Date(Date.now() + 86_400_000 * 5).toISOString(),
      paginasIaUtilizadas: 0,
      limitePaginasIa: 10,
    });
    renderApp('/objetivos');
    const user = userEvent.setup();
    await hablar(user, 'Quiero gastar menos en luz');

    expect(
      await screen.findByText('Te quedan 20 de 20 objetivos con IA este mes', {}, ESPERA),
    ).toBeVisible();
  });

  it('avisa si el servidor rechaza lo que se quiere guardar y no lo da por guardado', async () => {
    const usuario = setupUsuarioConSesion();
    renderApp('/objetivos');
    const user = userEvent.setup();
    await hablar(user, 'Quiero gastar menos en luz');
    await screen.findByRole('region', { name: 'Lo que entendió Sami' }, ESPERA);
    setupServerError('post', '/objetivos/sami', 400, 'Revisá los mini-objetivos que elegiste.');

    await user.click(screen.getByRole('button', { name: 'Guardar objetivo' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Revisá los mini-objetivos que elegiste.',
    );
    expect(objetivosDe(usuario.email)).toHaveLength(0);
  });

  it('tiene un tope de mensajes por conversación', async () => {
    setupUsuarioConSesion();
    renderApp('/objetivos');
    const user = userEvent.setup();
    await hablar(user, 'Quiero gastar menos en luz');
    for (let vuelta = 0; vuelta < 5; vuelta += 1) {
      await screen.findByRole('region', { name: 'Lo que entendió Sami' }, ESPERA);
      await user.type(screen.getByLabelText('¿Querés cambiar algo?'), `Más barato ${vuelta}`);
      await user.click(screen.getByRole('button', { name: 'Enviar' }));
      await waitFor(() =>
        expect(
          within(screen.getByRole('list', { name: 'Conversación con Sami' })).getAllByRole(
            'listitem',
          ),
        ).toHaveLength(2 * (vuelta + 2)),
      );
    }

    expect(
      await screen.findByText(/Llegaste al máximo de mensajes de esta conversación/, {}, ESPERA),
    ).toBeVisible();
    expect(screen.queryByLabelText('¿Querés cambiar algo?')).not.toBeInTheDocument();
  });
});
