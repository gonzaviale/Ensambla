import { describe, expect, it } from 'vitest';
import { HttpResponse, http } from 'msw';
import { consultarPlanes, iniciarPruebaPremium } from '@/api/planes';
import { estado } from '@/mocks/db/estado';
import { planesDe } from '@/mocks/db/planes';
import { server } from '@/mocks/node';
import { setupServerResponse, setupUsuarioConSesion } from '@/test/testHelpers';

describe('API de planes', () => {
  it('consulta catálogo y uso de la sesión sin iniciar una prueba', async () => {
    setupUsuarioConSesion();

    const result = await consultarPlanes();

    expect(result.suscripcion.codigoPlanActual).toBe('GRATUITO');
    expect(result.suscripcion.puedeIniciarPrueba).toBe(true);
    expect(result.planes).toHaveLength(2);
    expect(result.planes[1]?.precioMensual).toBe(14900);
    expect(estado.pruebasPremium.size).toBe(0);
  });

  it('activa siete días con diez páginas totales usando la sesión y CSRF', async () => {
    setupUsuarioConSesion();

    const prueba = await iniciarPruebaPremium();
    const planes = await consultarPlanes();

    expect(Date.parse(prueba.finEn) - Date.parse(prueba.inicioEn)).toBe(7 * 24 * 60 * 60 * 1000);
    expect(prueba.limitePaginasIa).toBe(10);
    expect(prueba.paginasIaUtilizadas).toBe(0);
    expect(planes.suscripcion.pruebaActiva).toBe(true);
    expect(planes.suscripcion.puedeIniciarPrueba).toBe(false);
    expect(planes.uso.periodoIa).toBe('PRUEBA');
  });

  it('conserva el código comercial y no repite un POST rechazado como si fuera CSRF', async () => {
    setupUsuarioConSesion();
    let intentos = 0;
    server.use(
      http.post('/api/planes/prueba', () => {
        intentos++;
        return HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 403,
            error: 'Forbidden',
            message: 'Este negocio ya utilizó su prueba premium.',
            path: '/api/planes/prueba',
            traceId: 'prueba',
            codigo: 'PRUEBA_UTILIZADA',
          },
          { status: 403 },
        );
      }),
    );

    await expect(iniciarPruebaPremium()).rejects.toMatchObject({
      status: 403,
      codigo: 'PRUEBA_UTILIZADA',
    });

    expect(intentos).toBe(1);
  });

  it('una segunda activación conserva las fechas y devuelve PRUEBA_UTILIZADA', async () => {
    const usuario = setupUsuarioConSesion();
    const primera = await iniciarPruebaPremium();

    await expect(iniciarPruebaPremium()).rejects.toMatchObject({ codigo: 'PRUEBA_UTILIZADA' });

    expect(estado.pruebasPremium.get(usuario.email)).toEqual(primera);
  });

  it('una sesión diferente no recibe la prueba de la cuenta anterior', async () => {
    setupUsuarioConSesion({ email: 'primera@example.test' });
    await iniciarPruebaPremium();
    setupUsuarioConSesion({ email: 'segunda@example.test' });

    const result = await consultarPlanes();

    expect(result.prueba).toBeNull();
    expect(result.suscripcion.puedeIniciarPrueba).toBe(true);
  });

  it('rechaza un contrato inválido sin interpretar una cuota negativa como disponible', async () => {
    const usuario = setupUsuarioConSesion();
    const datos = planesDe(usuario.email);
    setupServerResponse('get', '/planes', 200, { ...datos, uso: { ...datos.uso, paginasIa: -1 } });

    await expect(consultarPlanes()).rejects.toThrow();
  });
});
