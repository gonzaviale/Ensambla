package com.ensambla.backend.domain.model.negocio;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.zona.ZonaId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.ensambla.backend.support.NegociosDePrueba.unNegocioDeSanJusto;
import static com.ensambla.backend.support.NegociosDePrueba.unNegocioSinUbicacion;
import static com.ensambla.backend.support.NegociosDePrueba.unaUbicacionConEspacios;
import static com.ensambla.backend.support.NegociosDePrueba.unaZonaConId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ConfiguracionNegocioTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final ZonaId ZONA_ID = ZonaId.de("00000000-0000-4000-8000-000000000001");
    private static final ZonaId OTRA_ZONA_ID = ZonaId.de("00000000-0000-4000-8000-000000000002");

    private Negocio negocio;
    private UbicacionNegocio ubicacion;
    private boolean cambia;
    private boolean necesitaZona;
    private Throwable rechazo;

    @Test
    @DisplayName("Conserva todos los campos ausentes y la zona al editar sólo el nombre")
    void conservaTodosLosCamposAusentesYLaZonaAlEditarSoloElNombre() {
        dadoUnNegocioDeSanJustoConZona();

        cuandoSeConfiguraSoloElNombre("  Nuevo nombre  ");

        entoncesElNombreEsNuevoYSeActualizoAhora();
        entoncesConservaLaUbicacionElRubroLaPrivacidadYLaZona();
    }

    @Test
    @DisplayName("Actualiza ubicación, rubro y privacidad sin cambiar el nombre")
    void actualizaUbicacionRubroYPrivacidadSinCambiarElNombre() {
        dadoUnNegocioDeSanJustoConZona();

        cuandoSeConfiguraUnaUbicacionNuevaUnRubroYLaPrivacidad();

        entoncesConservaElNombre("Mi negocio");
        entoncesQuedaConLaUbicacionNuevaYSinZona();
        entoncesQuedaComoKioscoSinCompartirDatosDeZona();
    }

    @Test
    @DisplayName("Conserva la zona si se confirma el mismo punto")
    void conservaLaZonaSiSeConfirmaElMismoPunto() {
        dadoUnNegocioDeSanJustoConZona();

        cuandoSeConfiguraLaUbicacion("-34", "-58");

        entoncesConservaLaZona();
    }

    @Test
    @DisplayName("Invalida la zona anterior si cambia sólo la longitud")
    void invalidaLaZonaAnteriorSiCambiaSoloLaLongitud() {
        dadoUnNegocioDeSanJustoConZona();

        cuandoSeConfiguraLaUbicacion("-34", "-59");

        entoncesQuedaSinZona();
    }

    @Test
    @DisplayName("La misma ubicación guardada no cuenta como cambio")
    void laMismaUbicacionGuardadaNoCuentaComoCambio() {
        dadoUnNegocioDeSanJustoConZona();

        cuandoSeConsultaSiCambiaLaUbicacion(unaUbicacionConEspacios("-34", "-58"));

        entoncesNoCuentaComoCambio();
    }

    @Test
    @DisplayName("Cambiar la latitud cuenta como cambio")
    void cambiarLaLatitudCuentaComoCambio() {
        dadoUnNegocioDeSanJustoConZona();

        cuandoSeConsultaSiCambiaLaUbicacion(unaUbicacionConEspacios("-35", "-58"));

        entoncesCuentaComoCambio();
    }

    @Test
    @DisplayName("Cambiar la longitud cuenta como cambio")
    void cambiarLaLongitudCuentaComoCambio() {
        dadoUnNegocioDeSanJustoConZona();

        cuandoSeConsultaSiCambiaLaUbicacion(unaUbicacionConEspacios("-34", "-59"));

        entoncesCuentaComoCambio();
    }

    @Test
    @DisplayName("Cambiar la localidad cuenta como cambio")
    void cambiarLaLocalidadCuentaComoCambio() {
        dadoUnNegocioDeSanJustoConZona();

        cuandoSeConsultaSiCambiaLaUbicacion(unaUbicacionEnRamosMejia());

        entoncesCuentaComoCambio();
    }

    @Test
    @DisplayName("Necesita zona si tiene punto y no tiene zona")
    void necesitaZonaSiTienePuntoYNoTieneZona() {
        dadoUnNegocioDeSanJustoSinZona();

        cuandoSeConsultaSiNecesitaZona();

        entoncesNecesitaZona();
    }

    @Test
    @DisplayName("No necesita zona si ya tiene una")
    void noNecesitaZonaSiYaTieneUna() {
        dadoUnNegocioDeSanJustoConZona();

        cuandoSeConsultaSiNecesitaZona();

        entoncesNoNecesitaZona();
    }

    @Test
    @DisplayName("No necesita zona si no tiene punto")
    void noNecesitaZonaSiNoTienePunto() {
        dadoUnNegocioSinPunto();

        cuandoSeConsultaSiNecesitaZona();

        entoncesNoNecesitaZona();
    }

    @Test
    @DisplayName("Asigna el id de la zona resuelta")
    void asignaElIdDeLaZonaResuelta() {
        dadoUnNegocioDeSanJustoConZona();

        cuandoSeAsignaLaZonaResuelta();

        entoncesQuedaEnLaZona(OTRA_ZONA_ID);
    }

    @Test
    @DisplayName("Quita la zona si no se resolvió ninguna")
    void quitaLaZonaSiNoSeResolvioNinguna() {
        dadoUnNegocioDeSanJustoConZona();

        cuandoSeAsignaUnaZonaNoResuelta();

        entoncesQuedaSinZona();
    }

    @Test
    @DisplayName("Rechaza un nombre vacío")
    void rechazaUnNombreVacio() {
        dadoUnNegocioDeSanJustoConZona();

        cuandoSeIntentaConfigurarElNombre("  ");

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Rechaza un nombre mayor a 120 caracteres")
    void rechazaUnNombreMayorA120Caracteres() {
        dadoUnNegocioDeSanJustoConZona();

        cuandoSeIntentaConfigurarUnNombreDe121Caracteres();

        entoncesSeRechaza();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Rechaza dirección o localidad sin contenido")
    void rechazaDireccionOLocalidadSinContenido(String texto) {
        cuandoSeIntentaCrearLaUbicacion(texto, "San Justo", BigDecimal.ZERO, BigDecimal.ZERO);

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Rechaza una dirección demasiado larga")
    void rechazaUnaDireccionDemasiadoLarga() {
        cuandoSeIntentaCrearLaUbicacion("a".repeat(501), "San Justo", BigDecimal.ZERO, BigDecimal.ZERO);

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Rechaza una localidad demasiado larga")
    void rechazaUnaLocalidadDemasiadoLarga() {
        cuandoSeIntentaCrearLaUbicacion("Calle 1", "a".repeat(121), BigDecimal.ZERO, BigDecimal.ZERO);

        entoncesSeRechaza();
    }

    @ParameterizedTest
    @ValueSource(strings = {"90.00001", "-90.00001"})
    @DisplayName("Rechaza latitudes fuera de rango antes de redondear")
    void rechazaLatitudesFueraDeRangoAntesDeRedondear(String latitud) {
        cuandoSeIntentaCrearLaUbicacionConCoordenadas(latitud, "0");

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Rechaza una coordenada ausente")
    void rechazaUnaCoordenadaAusente() {
        cuandoSeIntentaCrearLaUbicacion("Calle 1", "San Justo", null, BigDecimal.ZERO);

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Rechaza longitudes fuera de rango")
    void rechazaLongitudesFueraDeRango() {
        cuandoSeIntentaCrearLaUbicacionConCoordenadas("0", "181");

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Normaliza los textos y redondea al formato de coordenadas de PostgreSQL")
    void normalizaLosTextosYRedondeaAlFormatoDeCoordenadasDePostgreSQL() {
        cuandoSeCreaLaUbicacionConCoordenadas("-34.1234567", "-58.1234567");

        entoncesLosTextosQuedanSinEspacios();
        entoncesLasCoordenadasQuedanConSeisDecimales();
    }

    // --- dado ---
    private void dadoUnNegocioDeSanJustoConZona() {
        negocio = unNegocioDeSanJusto(ZONA_ID);
    }

    private void dadoUnNegocioDeSanJustoSinZona() {
        negocio = unNegocioDeSanJusto(null);
    }

    private void dadoUnNegocioSinPunto() {
        negocio = unNegocioSinUbicacion();
    }

    // --- cuando ---
    private void cuandoSeConfiguraSoloElNombre(String nombre) {
        negocio.configurar(nombre, null, null, null, AHORA);
    }

    private void cuandoSeConfiguraUnaUbicacionNuevaUnRubroYLaPrivacidad() {
        negocio.configurar(null, RubroNegocio.KIOSCO, unaUbicacionConEspacios("-35", "-59"), false, AHORA);
    }

    private void cuandoSeConfiguraLaUbicacion(String latitud, String longitud) {
        negocio.configurar(null, null, unaUbicacionConEspacios(latitud, longitud), null, AHORA);
    }

    private void cuandoSeConsultaSiCambiaLaUbicacion(UbicacionNegocio nueva) {
        cambia = negocio.cambiaUbicacion(nueva);
    }

    private void cuandoSeConsultaSiNecesitaZona() {
        necesitaZona = negocio.necesitaZona();
    }

    private void cuandoSeAsignaLaZonaResuelta() {
        negocio.asignarZona(unaZonaConId(OTRA_ZONA_ID));
    }

    private void cuandoSeAsignaUnaZonaNoResuelta() {
        negocio.asignarZona(null);
    }

    private void cuandoSeIntentaConfigurarElNombre(String nombre) {
        rechazo = catchThrowable(() -> cuandoSeConfiguraSoloElNombre(nombre));
    }

    private void cuandoSeIntentaConfigurarUnNombreDe121Caracteres() {
        cuandoSeIntentaConfigurarElNombre("a".repeat(121));
    }

    private void cuandoSeIntentaCrearLaUbicacion(String direccion, String localidad, BigDecimal latitud,
                                                 BigDecimal longitud) {
        rechazo = catchThrowable(() -> new UbicacionNegocio(direccion, localidad, latitud, longitud));
    }

    private void cuandoSeIntentaCrearLaUbicacionConCoordenadas(String latitud, String longitud) {
        rechazo = catchThrowable(() -> unaUbicacionConEspacios(latitud, longitud));
    }

    private void cuandoSeCreaLaUbicacionConCoordenadas(String latitud, String longitud) {
        ubicacion = unaUbicacionConEspacios(latitud, longitud);
    }

    private UbicacionNegocio unaUbicacionEnRamosMejia() {
        return new UbicacionNegocio("Calle 1", "Ramos Mejía", new BigDecimal("-34"), new BigDecimal("-58"));
    }

    // --- entonces ---
    private void entoncesElNombreEsNuevoYSeActualizoAhora() {
        assertThat(negocio.getNombre()).isEqualTo("Nuevo nombre");
        assertThat(negocio.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesConservaLaUbicacionElRubroLaPrivacidadYLaZona() {
        assertThat(negocio.getRubro()).isEqualTo(RubroNegocio.ALMACEN);
        assertThat(negocio.getDireccion()).isEqualTo("Calle 1");
        assertThat(negocio.getLocalidad()).isEqualTo("San Justo");
        assertThat(negocio.getLatitud()).isEqualByComparingTo("-34");
        assertThat(negocio.getLongitud()).isEqualByComparingTo("-58");
        assertThat(negocio.getComparteDatosZona()).isTrue();
        assertThat(negocio.getZonaId()).isEqualTo(ZONA_ID);
    }

    private void entoncesConservaElNombre(String nombre) {
        assertThat(negocio.getNombre()).isEqualTo(nombre);
    }

    private void entoncesQuedaConLaUbicacionNuevaYSinZona() {
        assertThat(negocio.getDireccion()).isEqualTo("Nueva dirección");
        assertThat(negocio.getLocalidad()).isEqualTo("San Justo");
        assertThat(negocio.getLatitud()).isEqualByComparingTo("-35");
        assertThat(negocio.getLongitud()).isEqualByComparingTo("-59");
        assertThat(negocio.getZonaId()).isNull();
    }

    private void entoncesQuedaComoKioscoSinCompartirDatosDeZona() {
        assertThat(negocio.getRubro()).isEqualTo(RubroNegocio.KIOSCO);
        assertThat(negocio.getComparteDatosZona()).isFalse();
    }

    private void entoncesConservaLaZona() {
        assertThat(negocio.getZonaId()).isEqualTo(ZONA_ID);
    }

    private void entoncesQuedaSinZona() {
        assertThat(negocio.getZonaId()).isNull();
    }

    private void entoncesQuedaEnLaZona(ZonaId zonaId) {
        assertThat(negocio.getZonaId()).isEqualTo(zonaId);
    }

    private void entoncesCuentaComoCambio() {
        assertThat(cambia).isTrue();
    }

    private void entoncesNoCuentaComoCambio() {
        assertThat(cambia).isFalse();
    }

    private void entoncesNecesitaZona() {
        assertThat(necesitaZona).isTrue();
    }

    private void entoncesNoNecesitaZona() {
        assertThat(necesitaZona).isFalse();
    }

    private void entoncesSeRechaza() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesLosTextosQuedanSinEspacios() {
        assertThat(ubicacion.direccion()).isEqualTo("Nueva dirección");
        assertThat(ubicacion.localidad()).isEqualTo("San Justo");
    }

    private void entoncesLasCoordenadasQuedanConSeisDecimales() {
        assertThat(ubicacion.latitud()).isEqualTo(new BigDecimal("-34.123457"));
        assertThat(ubicacion.longitud()).isEqualTo(new BigDecimal("-58.123457"));
    }
}
