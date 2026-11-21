import { describe, it } from 'vitest';
import {
  cuandoSeAbreElMenuDeSecciones,
  cuandoSeAbreLaLanding,
  cuandoSeAbreLaPreguntaSobreElCuaderno,
  cuandoSeAbreLaPreguntaSobreLaDescarga,
  cuandoSeAvanzaAlPasoDeLaBusqueda,
  cuandoSeBuscaPorNombre,
  cuandoSeCierraLaPreguntaSobreElCuaderno,
  cuandoSeConfirmaElImporte,
  cuandoSeEligeLaCategoria,
  cuandoSeEligeLaPrimeraLlamadaAArmarElNegocio,
  cuandoSeEligeYaTengoCuenta,
  cuandoSePideVerComoSeOrganizaLaCategoria,
  cuandoSePulsaEscape,
  cuandoSeRecorreElEjemploConElTeclado,
  cuandoSeRecorrenTodasLasCategorias,
  cuandoSeVuelveALaAnotacionConElTeclado,
  cuandoSeVuelveAlInicioDesdeElLogo,
  cuandoSeVuelveArribaConElBoton,
  dadoQueElDesplazamientoDeLaPaginaSeEspia,
  dadoQueSeAbreLaLanding,
  dadoQueSeAbrioElMenuDeSecciones,
  dadoQueSeComenzoElEjemplo,
  dadoQueSeEligioLaCategoriaConElTeclado,
  dadoQueSeLlegoALaBusqueda,
  dadoQueSeOrganizoLaCategoria,
  entoncesCadaCategoriaTieneUnaPoseDistinta,
  entoncesCadaEnlaceInternoTieneUnDestino,
  entoncesElEjemploSeAlcanzaDesdeLaPrimeraPantalla,
  entoncesElFocoPasaAlPasoSiguiente,
  entoncesElFocoVaALaBusquedaEnElPasoTres,
  entoncesElFocoVaAlImporteEnElPasoDos,
  entoncesElMenuQuedaCerradoConElFocoEnSuBoton,
  entoncesHayUnSoloSamiEnCadaCategoria,
  entoncesHayUnSoloTituloPrincipal,
  entoncesLaCategoriaElegidaTieneElFocoYEstaPresionada,
  entoncesLaCategoriaQuedaElegidaConSuAnotacion,
  entoncesLaDescargaFiguraComoPendienteSinAccionFalsa,
  entoncesLaListaIncluyeA,
  entoncesLaListaMuestraLosTresNombres,
  entoncesLaListaMuestraSoloA,
  entoncesLaPaginaVuelveAlInicio,
  entoncesLaPoseNoCambiaAlOrganizar,
  entoncesLaPreguntaSobreElCuadernoEstaAbierta,
  entoncesLaPreguntaSobreElCuadernoEstaCerrada,
  entoncesLasLlamadasPrincipalesApuntanAlRegistro,
  entoncesNoHayCoincidencias,
  entoncesNoHayProblemasDeAccesibilidad,
  entoncesNoSeGuardoNadaEnElNavegador,
  entoncesSamiOrganizaLaCategoria,
  entoncesSeAbreElIngreso,
  entoncesSeAbreElRegistro,
  entoncesSeEncuentraElFiadoConElImporteCorregido,
  entoncesSeRechazaElImporteSinPasarALaConfirmacion,
  entoncesSeVeElMenuDeSecciones,
  entoncesSeVuelveAlApunteOriginalDe,
} from '@/test/landing';

const EJEMPLOS_POR_CATEGORIA = [
  ['Ventas', 'Total de la venta', '$ 7.000'],
  ['Gastos', 'Medio de pago', 'Transferencia'],
  ['Fiados', 'Importe pendiente', '$ 3.200'],
  ['Productos', 'Cantidad disponible', '12 unidades'],
] as const;

describe('landing de Ensambla', () => {
  it('presenta un solo h1 y sus llamadas principales apuntan al registro real', async () => {
    await cuandoSeAbreLaLanding();

    entoncesHayUnSoloTituloPrincipal();
    entoncesLasLlamadasPrincipalesApuntanAlRegistro();
  });

  it('la llamada principal abre el registro real', async () => {
    await dadoQueSeAbreLaLanding();

    await cuandoSeEligeLaPrimeraLlamadaAArmarElNegocio();

    await entoncesSeAbreElRegistro();
  });

  it('lleva al ingreso existente desde el cierre', async () => {
    await dadoQueSeAbreLaLanding();

    await cuandoSeEligeYaTengoCuenta();

    await entoncesSeAbreElIngreso();
  });

  it('permite corregir un importe y encontrar el resultado confirmado', async () => {
    await dadoQueSeAbreLaLanding();

    await cuandoSeConfirmaElImporte('4500.5');

    entoncesSeEncuentraElFiadoConElImporteCorregido();
    entoncesNoSeGuardoNadaEnElNavegador();
  });

  it('la búsqueda muestra todos los nombres cuando se la vacía', async () => {
    await dadoQueSeAbreLaLanding();
    await dadoQueSeLlegoALaBusqueda();

    await cuandoSeBuscaPorNombre('');

    entoncesLaListaMuestraLosTresNombres();
  });

  it('la búsqueda encuentra por nombre sin depender de mayúsculas ni tildes', async () => {
    await dadoQueSeAbreLaLanding();
    await dadoQueSeLlegoALaBusqueda();

    await cuandoSeBuscaPorNombre('LOPEZ');

    entoncesLaListaMuestraSoloA('Ana López');
  });

  it('la búsqueda encuentra por el comienzo del nombre', async () => {
    await dadoQueSeAbreLaLanding();
    await dadoQueSeLlegoALaBusqueda();

    await cuandoSeBuscaPorNombre('luci');

    entoncesLaListaIncluyeA('Lucía Pérez');
  });

  it('la búsqueda resuelve el vacío cuando nadie coincide', async () => {
    await dadoQueSeAbreLaLanding();
    await dadoQueSeLlegoALaBusqueda();

    await cuandoSeBuscaPorNombre('nadie');

    entoncesNoHayCoincidencias();
  });

  it('rechaza un importe vacío o no positivo sin pasar a la confirmación', async () => {
    await dadoQueSeAbreLaLanding();

    await cuandoSeConfirmaElImporte('0');

    entoncesSeRechazaElImporteSinPasarALaConfirmacion();
  });

  it('permite recorrer los pasos del ejemplo con el teclado', async () => {
    await dadoQueSeAbreLaLanding();

    await cuandoSeRecorreElEjemploConElTeclado();

    entoncesElFocoPasaAlPasoSiguiente();
  });

  it.each(EJEMPLOS_POR_CATEGORIA)(
    'Sami muestra la anotación de %s antes de organizarla',
    async (categoria) => {
      await dadoQueSeAbreLaLanding();

      await cuandoSeEligeLaCategoria(categoria);

      entoncesLaCategoriaQuedaElegidaConSuAnotacion(categoria);
    },
  );

  it.each(EJEMPLOS_POR_CATEGORIA)(
    'Sami organiza %s en campos y valores al pedírselo',
    async (categoria, campo, valor) => {
      await dadoQueSeAbreLaLanding();

      await cuandoSePideVerComoSeOrganizaLaCategoria(categoria);

      entoncesSamiOrganizaLaCategoria(categoria, campo, valor);
      entoncesNoSeGuardoNadaEnElNavegador();
    },
  );

  it('mantiene un solo Sami en cada categoría, incluso al volver a elegirlas', async () => {
    await dadoQueSeAbreLaLanding();

    await cuandoSeRecorrenTodasLasCategorias();

    entoncesHayUnSoloSamiEnCadaCategoria();
  });

  it('muestra una pose diferente por categoría', async () => {
    await dadoQueSeAbreLaLanding();

    await cuandoSeRecorrenTodasLasCategorias();

    entoncesCadaCategoriaTieneUnaPoseDistinta();
  });

  it('conserva la pose de Sami al organizar la anotación', async () => {
    await dadoQueSeAbreLaLanding();

    await cuandoSeRecorrenTodasLasCategorias();

    entoncesLaPoseNoCambiaAlOrganizar();
  });

  it('permite elegir una categoría con el teclado', async () => {
    await dadoQueSeAbreLaLanding();

    await dadoQueSeEligioLaCategoriaConElTeclado('Gastos');

    entoncesLaCategoriaElegidaTieneElFocoYEstaPresionada('Gastos');
  });

  it('permite regresar al apunte original con el teclado', async () => {
    await dadoQueSeAbreLaLanding();
    await dadoQueSeEligioLaCategoriaConElTeclado('Gastos');
    await dadoQueSeOrganizoLaCategoria('Gastos');

    await cuandoSeVuelveALaAnotacionConElTeclado('Gastos');

    entoncesSeVuelveAlApunteOriginalDe('Gastos', /Pagué \$ 18.500 al proveedor/);
  });

  it('alcanza el ejemplo desde la primera pantalla', async () => {
    await cuandoSeAbreLaLanding();

    entoncesElEjemploSeAlcanzaDesdeLaPrimeraPantalla();
  });

  it('cada enlace interno tiene un destino en la página', async () => {
    await cuandoSeAbreLaLanding();

    entoncesCadaEnlaceInternoTieneUnDestino();
  });

  it('identifica la descarga como pendiente sin ofrecer una acción falsa', async () => {
    await dadoQueSeAbreLaLanding();

    await cuandoSeAbreLaPreguntaSobreLaDescarga();

    entoncesLaDescargaFiguraComoPendienteSinAccionFalsa();
  });

  it('guía el ejemplo con acciones de avance y lleva el foco al importe del segundo paso', async () => {
    await dadoQueSeAbreLaLanding();

    await dadoQueSeComenzoElEjemplo();

    entoncesElFocoVaAlImporteEnElPasoDos();
  });

  it('lleva el foco a la búsqueda al confirmar el importe', async () => {
    await dadoQueSeAbreLaLanding();
    await dadoQueSeComenzoElEjemplo();

    await cuandoSeAvanzaAlPasoDeLaBusqueda();

    entoncesElFocoVaALaBusquedaEnElPasoTres();
  });

  it('abre las preguntas sin salir de la página', async () => {
    await dadoQueSeAbreLaLanding();

    await cuandoSeAbreLaPreguntaSobreElCuaderno();

    entoncesLaPreguntaSobreElCuadernoEstaAbierta();
  });

  it('cierra las preguntas sin salir de la página', async () => {
    await dadoQueSeAbreLaLanding();
    await cuandoSeAbreLaPreguntaSobreElCuaderno();

    await cuandoSeCierraLaPreguntaSobreElCuaderno();

    entoncesLaPreguntaSobreElCuadernoEstaCerrada();
  });

  it('abre el menú de secciones', async () => {
    await dadoQueSeAbreLaLanding();

    await cuandoSeAbreElMenuDeSecciones();

    entoncesSeVeElMenuDeSecciones();
  });

  it('cierra el menú de secciones con Escape y devuelve el foco a su botón', async () => {
    await dadoQueSeAbreLaLanding();
    await dadoQueSeAbrioElMenuDeSecciones();

    await cuandoSePulsaEscape();

    entoncesElMenuQuedaCerradoConElFocoEnSuBoton();
  });

  it('el logo conserva su navegación local', async () => {
    dadoQueElDesplazamientoDeLaPaginaSeEspia();
    await dadoQueSeAbreLaLanding();

    await cuandoSeVuelveAlInicioDesdeElLogo();

    entoncesLaPaginaVuelveAlInicio();
  });

  it('el botón de volver arriba conserva su navegación local', async () => {
    dadoQueElDesplazamientoDeLaPaginaSeEspia();
    await dadoQueSeAbreLaLanding();

    await cuandoSeVuelveArribaConElBoton();

    entoncesLaPaginaVuelveAlInicio();
  });

  it('no tiene problemas de accesibilidad detectables sin layout', async () => {
    await cuandoSeAbreLaLanding();

    await entoncesNoHayProblemasDeAccesibilidad();
  });
});
