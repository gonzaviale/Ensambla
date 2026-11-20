import { describe, it } from 'vitest';
import {
  cuandoPasanLosMilisegundos,
  cuandoSeResaltaDosVecesElMismoElemento,
  cuandoSeResaltaElElemento,
  cuandoSeResaltaUnElementoInexistente,
  dadoQueElTiempoLoManejaLaPrueba,
  dadoUnElementoEnPantalla,
  entoncesElMarcoEstaSaliendo,
  entoncesHayUnSoloMarcoSobreElElemento,
  entoncesNoQuedaNingunMarco,
} from '@/test/resaltado';

describe('resaltar', () => {
  it('pone un marco sobre el elemento y lo saca solo pasado el tiempo', () => {
    dadoQueElTiempoLoManejaLaPrueba();
    dadoUnElementoEnPantalla();

    cuandoSeResaltaElElemento({ duracion: 1000 });

    entoncesHayUnSoloMarcoSobreElElemento();
    cuandoPasanLosMilisegundos(1000);
    entoncesElMarcoEstaSaliendo();
    cuandoPasanLosMilisegundos(500);
    entoncesNoQuedaNingunMarco();
  });

  it('no deja dos marcos sobre el mismo elemento', () => {
    dadoUnElementoEnPantalla();

    cuandoSeResaltaDosVecesElMismoElemento();

    entoncesHayUnSoloMarcoSobreElElemento();
  });

  it('sin elemento no hace nada', () => {
    cuandoSeResaltaUnElementoInexistente();

    entoncesNoQuedaNingunMarco();
  });
});
