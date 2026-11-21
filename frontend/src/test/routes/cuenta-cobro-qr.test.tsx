import { describe, it } from 'vitest';
import {
  ALIAS_DE_PRUEBA,
  TITULAR_DE_PRUEBA,
  cuandoSeBorraElAlias,
  cuandoSeEscribeElAlias,
  cuandoSeEscribeElTitular,
  cuandoSeGuardaElCobro,
  dadaLaCuentaConElCobroGuardado,
  dadaLaCuentaSinCobroConQr,
  dadoQueNoSePuedeGuardarElNegocio,
  entoncesElCampoDelAliasTiene,
  entoncesElCampoDelTitularTiene,
  entoncesElNegocioCobraEn,
  entoncesElNegocioNoGuardoNada,
  entoncesNoSeMuestraElQrDePrueba,
  entoncesNoSePuedeEditarElTitular,
  entoncesNoSePuedeGuardar,
  entoncesSeAclaraQueNoSeVerificaElPago,
  entoncesSeAvisaQueNoSePudoGuardar,
  entoncesSeAvisaQueSeGuardo,
  entoncesSeMuestraElQrDePrueba,
  entoncesSePuedeCargarElAliasYElTitular,
  entoncesSePuedeGuardar,
} from '@/test/cobroConQr';

describe('cobro con QR en tu cuenta', () => {
  it('ofrece cargar el alias o CVU y el titular', async () => {
    await dadaLaCuentaSinCobroConQr();

    await entoncesSePuedeCargarElAliasYElTitular();
  });

  it('aclara que Ensambla no verifica el pago', async () => {
    await dadaLaCuentaSinCobroConQr();

    await entoncesSeAclaraQueNoSeVerificaElPago();
  });

  it('sin alias no muestra el QR de prueba', async () => {
    await dadaLaCuentaSinCobroConQr();

    entoncesNoSeMuestraElQrDePrueba();
  });

  it('sin alias no permite cargar un titular que no se guardaría', async () => {
    await dadaLaCuentaSinCobroConQr();

    entoncesNoSePuedeEditarElTitular();
  });

  it('al escribir el alias muestra el QR de prueba', async () => {
    await dadaLaCuentaSinCobroConQr();

    await cuandoSeEscribeElAlias(ALIAS_DE_PRUEBA);

    await entoncesSeMuestraElQrDePrueba();
  });

  it('guardar sólo se habilita cuando hay un cambio', async () => {
    await dadaLaCuentaSinCobroConQr();

    entoncesNoSePuedeGuardar();
  });

  it('escribir el alias habilita guardar', async () => {
    await dadaLaCuentaSinCobroConQr();

    await cuandoSeEscribeElAlias(ALIAS_DE_PRUEBA);

    entoncesSePuedeGuardar();
  });

  it('guarda el alias y el titular en el negocio y avisa', async () => {
    await dadaLaCuentaSinCobroConQr();
    await cuandoSeEscribeElAlias(ALIAS_DE_PRUEBA);
    await cuandoSeEscribeElTitular(TITULAR_DE_PRUEBA);

    await cuandoSeGuardaElCobro();

    await entoncesSeAvisaQueSeGuardo();
    await entoncesElNegocioCobraEn(ALIAS_DE_PRUEBA, TITULAR_DE_PRUEBA);
  });

  it('muestra lo que ya estaba guardado', async () => {
    await dadaLaCuentaConElCobroGuardado();

    entoncesElCampoDelAliasTiene(ALIAS_DE_PRUEBA);
    entoncesElCampoDelTitularTiene(TITULAR_DE_PRUEBA);
  });

  it('con un alias guardado muestra el QR de prueba y deja guardar sólo con un cambio', async () => {
    await dadaLaCuentaConElCobroGuardado();

    await entoncesSeMuestraElQrDePrueba();
    entoncesNoSePuedeGuardar();
  });

  it('borrar el alias y guardar quita el cobro con QR', async () => {
    await dadaLaCuentaConElCobroGuardado();
    await cuandoSeBorraElAlias();

    entoncesNoSePuedeEditarElTitular();

    await cuandoSeGuardaElCobro();

    await entoncesElNegocioCobraEn(null, null);
  });

  it('si no se puede guardar avisa y no cambia lo guardado', async () => {
    await dadaLaCuentaSinCobroConQr();
    dadoQueNoSePuedeGuardarElNegocio();
    await cuandoSeEscribeElAlias(ALIAS_DE_PRUEBA);

    await cuandoSeGuardaElCobro();

    await entoncesSeAvisaQueNoSePudoGuardar();
    entoncesElNegocioNoGuardoNada();
  });
});
