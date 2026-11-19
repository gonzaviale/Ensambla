import { describe, it } from 'vitest';
import {
  cuandoElServidorTraeLoMismo,
  cuandoElServidorTraeOtroNombre,
  cuandoElServidorTraeUnoNuevoYElEliminado,
  cuandoEntraOtraPersona,
  cuandoLaMismaPersonaVuelveACargar,
  cuandoSeAvisaOtraVez,
  cuandoSeCambiaElNombre,
  cuandoSeCambiaElNombreCuarentaVeces,
  cuandoSeCierraElAviso,
  cuandoSeDeshace,
  cuandoSeDeshaceDosVeces,
  cuandoSeEligeElPrimerBloque,
  cuandoSeEligeLaPrimeraPestana,
  cuandoSeEliminaElSistema,
  cuandoSeIncorporaElQueSeElimino,
  cuandoSeIncorporaUnaVersionViejaYUnoNuevo,
  cuandoSeMarcaGuardadoElPrimero,
  cuandoSeVuelveAAbrirComoNuevo,
  dadoUnAviso,
  dadosDosSistemasSinGuardar,
  dadoUnSistemaAbierto,
  dadoUnSistemaAbiertoConUnaPestanaNuevaElegida,
  dadoUnSistemaAbiertoConUnBloqueElegidoYUnCambio,
  dadoUnSistemaAbiertoQueSeElimino,
  dadoUnSistemaAbiertoYEditado,
  dadoUnSistemaAbiertoYOtroQueSeElimino,
  entoncesDevuelveLoQueDevolvioElCambio,
  entoncesElAvisoTieneOtroNumero,
  entoncesElBloqueElegidoEsElPrimero,
  entoncesElEstadoNoCambio,
  entoncesElHistorialTiene,
  entoncesElOriginalNoSeToco,
  entoncesElPrimeroNoSeLlama,
  entoncesElPrimeroSeLlama,
  entoncesElSistemaSeLlama,
  entoncesLaPestanaActivaEsLaPrimera,
  entoncesLaPestanaElegidaEsLaPrimera,
  entoncesQuedanElAbiertoYElCreadoPorSami,
  entoncesNoHayAviso,
  entoncesNoHayBloqueElegido,
  entoncesNoHayNadaPendiente,
  entoncesNoHaySistemas,
  entoncesQuedaLaOtraPersonaSinNadaAbierto,
  entoncesQuedaListoParaUsarSinSeleccionNiHistorial,
  entoncesQuedaMarcadoParaBorrarYCerrado,
  entoncesQuedaPendienteDeGuardar,
  entoncesSePudoDeshacer,
  entoncesSoloElSistemaQuedaPendiente,
  entoncesSoloQuedaPendienteElSegundo,
} from '@/test/storeDelSistema';

describe('cargar los sistemas de la cuenta', () => {
  it('no pisa lo editado si vuelve a cargar la misma persona', () => {
    dadoUnSistemaAbiertoYEditado('Editado');

    cuandoLaMismaPersonaVuelveACargar();

    entoncesElSistemaSeLlama('Editado');
  });

  it('cambiar de cuenta reemplaza todo lo que había', () => {
    dadoUnSistemaAbierto();

    cuandoEntraOtraPersona();

    entoncesQuedaLaOtraPersonaSinNadaAbierto();
  });
});

describe('sumar lo que creó el servidor', () => {
  it('suma los sistemas nuevos sin repetir los que ya estaban', () => {
    dadoUnSistemaAbierto();

    cuandoSeIncorporaUnaVersionViejaYUnoNuevo();

    entoncesQuedanElAbiertoYElCreadoPorSami();
  });

  it('no pisa los sistemas que ya estaban', () => {
    dadoUnSistemaAbierto();

    cuandoSeIncorporaUnaVersionViejaYUnoNuevo();

    entoncesElPrimeroNoSeLlama('Viejo');
  });

  it('lo que llegó del servidor no queda pendiente de guardar', () => {
    dadoUnSistemaAbierto();

    cuandoSeIncorporaUnaVersionViejaYUnoNuevo();

    entoncesNoHayNadaPendiente();
  });

  it('no vuelve a sumar un sistema que la persona acaba de eliminar', () => {
    dadoUnSistemaAbiertoQueSeElimino();

    cuandoSeIncorporaElQueSeElimino();

    entoncesNoHaySistemas();
  });
});

describe('traer lo que cambió en el servidor', () => {
  it('trae lo que el servidor tiene de más en un sistema que la pestaña no cambió', () => {
    dadoUnSistemaAbierto();

    cuandoElServidorTraeOtroNombre('Con filas de WhatsApp');

    entoncesElPrimeroSeLlama('Con filas de WhatsApp');
  });

  it('lo traído del servidor no queda pendiente de guardar', () => {
    dadoUnSistemaAbierto();

    cuandoElServidorTraeOtroNombre('Con filas de WhatsApp');

    entoncesNoHayNadaPendiente();
  });

  it('no pisa un sistema con cambios sin guardar', () => {
    dadoUnSistemaAbiertoYEditado('Editado acá');

    cuandoElServidorTraeOtroNombre('Del servidor');

    entoncesElPrimeroSeLlama('Editado acá');
  });

  it('el sistema con cambios sigue pendiente de guardar', () => {
    dadoUnSistemaAbiertoYEditado('Editado acá');

    cuandoElServidorTraeOtroNombre('Del servidor');

    entoncesQuedaPendienteDeGuardar();
  });

  it('suma los sistemas nuevos y respeta los que se eliminaron', () => {
    dadoUnSistemaAbiertoYOtroQueSeElimino();

    cuandoElServidorTraeUnoNuevoYElEliminado();

    entoncesQuedanElAbiertoYElCreadoPorSami();
  });

  it('si el servidor no trae nada distinto no toca el estado', () => {
    dadoUnSistemaAbierto();

    cuandoElServidorTraeLoMismo();

    entoncesElEstadoNoCambio();
  });
});

describe('abrir y editar un sistema', () => {
  it('abrir un sistema limpia la selección y el historial', () => {
    dadoUnSistemaAbiertoConUnBloqueElegidoYUnCambio();

    cuandoSeVuelveAAbrirComoNuevo();

    entoncesQuedaListoParaUsarSinSeleccionNiHistorial();
  });

  it('abrir un sistema elige su primera pestaña', () => {
    dadoUnSistemaAbiertoConUnBloqueElegidoYUnCambio();

    cuandoSeVuelveAAbrirComoNuevo();

    entoncesLaPestanaActivaEsLaPrimera();
  });

  it('un cambio devuelve lo que devuelve la función que cambia', () => {
    dadoUnSistemaAbierto();

    cuandoSeCambiaElNombre('Mi almacén nuevo');

    entoncesDevuelveLoQueDevolvioElCambio();
  });

  it('un cambio se aplica sobre una copia', () => {
    dadoUnSistemaAbierto();

    cuandoSeCambiaElNombre('Mi almacén nuevo');

    entoncesElOriginalNoSeToco();
  });

  it('el sistema abierto queda con el cambio', () => {
    dadoUnSistemaAbierto();

    cuandoSeCambiaElNombre('Mi almacén nuevo');

    entoncesElSistemaSeLlama('Mi almacén nuevo');
  });

  it('un cambio queda pendiente de guardar', () => {
    dadoUnSistemaAbierto();

    cuandoSeCambiaElNombre('Mi almacén nuevo');

    entoncesSoloElSistemaQuedaPendiente();
  });

  it('un cambio se puede deshacer', () => {
    dadoUnSistemaAbiertoYEditado('Mi almacén nuevo');

    cuandoSeDeshace();

    entoncesElSistemaSeLlama('Mi almacén');
  });

  it('sin cambios no hay nada que deshacer', () => {
    dadoUnSistemaAbiertoYEditado('Mi almacén nuevo');

    cuandoSeDeshaceDosVeces();

    entoncesSePudoDeshacer(false);
  });

  it('el historial guarda como máximo 30 pasos', () => {
    dadoUnSistemaAbierto();

    cuandoSeCambiaElNombreCuarentaVeces();

    entoncesElHistorialTiene(30);
  });

  it('deshacer una pestaña que ya no existe vuelve a la primera', () => {
    dadoUnSistemaAbiertoConUnaPestanaNuevaElegida();

    cuandoSeDeshace();

    entoncesLaPestanaElegidaEsLaPrimera();
  });
});

describe('quitar y guardar sistemas', () => {
  it('quitar un sistema lo marca para borrar, no lo deja pendiente y lo cierra', () => {
    dadoUnSistemaAbiertoYEditado('x');

    cuandoSeEliminaElSistema();

    entoncesQuedaMarcadoParaBorrarYCerrado();
  });

  it('marcar guardados limpia sólo lo enviado', () => {
    dadosDosSistemasSinGuardar();

    cuandoSeMarcaGuardadoElPrimero();

    entoncesSoloQuedaPendienteElSegundo();
  });
});

describe('pestañas, bloques y avisos', () => {
  it('los lectores encuentran el bloque elegido', () => {
    dadoUnSistemaAbierto();

    cuandoSeEligeElPrimerBloque();

    entoncesElBloqueElegidoEsElPrimero();
  });

  it('elegir una pestaña limpia el bloque elegido', () => {
    dadoUnSistemaAbierto();

    cuandoSeEligeLaPrimeraPestana();

    entoncesNoHayBloqueElegido();
  });

  it('cada aviso tiene un número distinto', () => {
    dadoUnAviso();

    cuandoSeAvisaOtraVez();

    entoncesElAvisoTieneOtroNumero();
  });

  it('cerrar el aviso lo saca', () => {
    dadoUnAviso();

    cuandoSeCierraElAviso();

    entoncesNoHayAviso();
  });
});
