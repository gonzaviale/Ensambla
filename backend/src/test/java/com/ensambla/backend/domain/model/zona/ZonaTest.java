package com.ensambla.backend.domain.model.zona;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.ZonasDePrueba.LATITUD;
import static com.ensambla.backend.support.ZonasDePrueba.LONGITUD;
import static com.ensambla.backend.support.ZonasDePrueba.unaZonaCentradaEn;
import static com.ensambla.backend.support.ZonasDePrueba.unaZonaCentradaEnSanJusto;
import static com.ensambla.backend.support.ZonasDePrueba.unaZonaNombrada;
import static com.ensambla.backend.support.ZonasDePrueba.unaZonaSinLocalidad;
import static org.assertj.core.api.Assertions.assertThat;

class ZonaTest {

    /** 0,001° de latitud equivalen a ~111,2 m sobre la superficie terrestre. */
    private static final BigDecimal LATITUD_A_111_METROS = new BigDecimal("-34.669300");

    private Zona zona;
    private Zona zonaCercana;
    private List<Zona> zonas;
    private double distancia;
    private boolean contiene;
    private boolean coincide;
    private Optional<Zona> resuelta;

    @Test
    @DisplayName("La distancia al propio centro es cero")
    void laDistanciaAlPropioCentroEsCero() {
        dadoUnaZonaDeRadio(1500);

        cuandoSeMideLaDistanciaAlCentro();

        entoncesLaDistanciaEsCero();
    }

    @Test
    @DisplayName("Mide la distancia sobre la superficie terrestre")
    void mideLaDistanciaSobreLaSuperficieTerrestre() {
        dadoUnaZonaDeRadio(1500);

        cuandoSeMideLaDistanciaAUnPuntoA111Metros();

        entoncesLaDistanciaEsDeAlrededorDe111Metros();
    }

    @Test
    @DisplayName("Contiene un punto dentro del radio")
    void contieneUnPuntoDentroDelRadio() {
        dadoUnaZonaDeRadio(112);

        cuandoSePreguntaSiContieneUnPuntoA111Metros();

        entoncesLoContiene();
    }

    @Test
    @DisplayName("No contiene un punto fuera del radio")
    void noContieneUnPuntoFueraDelRadio() {
        dadoUnaZonaDeRadio(110);

        cuandoSePreguntaSiContieneUnPuntoA111Metros();

        entoncesNoLoContiene();
    }

    @Test
    @DisplayName("Resuelve la zona cuyo radio contiene el punto")
    void resuelveLaZonaCuyoRadioContieneElPunto() {
        dadoUnaZonaQueContieneElPuntoYOtraDeCaba();

        cuandoSeResuelveLaZonaDelPunto();

        entoncesSeResuelveLaZonaCercana();
    }

    @Test
    @DisplayName("Elige la zona de centro más cercano cuando se superponen")
    void eligeLaZonaDeCentroMasCercanoCuandoSeSuperponen() {
        dadoDosZonasSuperpuestasUnaMasCercanaQueLaOtra();

        cuandoSeResuelveLaZonaDelPunto();

        entoncesSeResuelveLaZonaCercana();
    }

    @Test
    @DisplayName("No resuelve zona si ninguna contiene el punto")
    void noResuelveZonaSiNingunaContieneElPunto() {
        dadoSoloUnaZonaDeCabaQueNoContieneElPunto();

        cuandoSeResuelveLaZonaDelPunto();

        entoncesNoSeResuelveNingunaZona();
    }

    @Test
    @DisplayName("Coincide con el texto que nombra a la zona, sin importar mayúsculas ni espacios de los bordes")
    void coincideConElTextoQueNombraALaZonaSinImportarMayusculasNiEspaciosDeLosBordes() {
        dadoUnaZonaDeSanJusto();

        cuandoSeBuscaCoincidenciaConElTexto("  UNLAM ");

        entoncesCoincide();
    }

    @Test
    @DisplayName("Coincide con un texto que repite los espacios del medio")
    void coincideConUnTextoQueRepiteLosEspaciosDelMedio() {
        dadoUnaZonaDeSanJusto();

        cuandoSeBuscaCoincidenciaConElTexto("san   justo");

        entoncesCoincide();
    }

    @Test
    @DisplayName("Coincide con el texto que nombra a la localidad aunque el nombre sea otro")
    void coincideConElTextoQueNombraALaLocalidadAunqueElNombreSeaOtro() {
        dadoUnaZonaDeVillaCrespo();

        cuandoSeBuscaCoincidenciaConElTexto("villa crespo");

        entoncesCoincide();
    }

    @Test
    @DisplayName("Coincide con la localidad sin importar tildes ni mayúsculas")
    void coincideConLaLocalidadSinImportarTildesNiMayusculas() {
        dadoUnaZonaDeVillaCrespo();

        cuandoSeBuscaCoincidenciaConElTexto("VILLA CRESPÓ");

        entoncesCoincide();
    }

    @Test
    @DisplayName("No coincide con un texto vacío")
    void noCoincideConUnTextoVacio() {
        dadoUnaZonaDeSanJusto();

        cuandoSeBuscaCoincidenciaConElTexto("   ");

        entoncesNoCoincide();
    }

    @Test
    @DisplayName("No coincide con un texto ausente")
    void noCoincideConUnTextoAusente() {
        dadoUnaZonaDeSanJusto();

        cuandoSeBuscaCoincidenciaConElTexto(null);

        entoncesNoCoincide();
    }

    @Test
    @DisplayName("No coincide con un texto que no la nombra")
    void noCoincideConUnTextoQueNoLaNombra() {
        dadoUnaZonaDeSanJusto();

        cuandoSeBuscaCoincidenciaConElTexto("Palermo");

        entoncesNoCoincide();
    }

    @Test
    @DisplayName("Una zona sin localidad igual coincide por nombre")
    void unaZonaSinLocalidadIgualCoincidePorNombre() {
        dadoUnaZonaSinLocalidad();

        cuandoSeBuscaCoincidenciaConElTexto("justo");

        entoncesCoincide();
    }

    @Test
    @DisplayName("Una zona sin localidad no coincide con un texto que no nombra su nombre")
    void unaZonaSinLocalidadNoCoincideConUnTextoQueNoNombraSuNombre() {
        dadoUnaZonaSinLocalidad();

        cuandoSeBuscaCoincidenciaConElTexto("Palermo");

        entoncesNoCoincide();
    }

    // --- dado ---
    private void dadoUnaZonaDeRadio(int radioMetros) {
        zona = unaZonaCentradaEnSanJusto(radioMetros);
    }

    private void dadoUnaZonaDeSanJusto() {
        zona = unaZonaNombrada("San Justo - UNLaM", "San Justo");
    }

    private void dadoUnaZonaDeVillaCrespo() {
        zona = unaZonaNombrada("Centro comercial", "Villa Crespo");
    }

    private void dadoUnaZonaSinLocalidad() {
        zona = unaZonaSinLocalidad("San Justo");
    }

    private void dadoUnaZonaQueContieneElPuntoYOtraDeCaba() {
        zonaCercana = unaZonaCentradaEnSanJusto(1500);
        zonas = List.of(unaZonaDeCaba(), zonaCercana);
    }

    private void dadoDosZonasSuperpuestasUnaMasCercanaQueLaOtra() {
        zonaCercana = unaZonaCentradaEnSanJusto(2000);
        zonas = List.of(unaZonaCentradaEn("-34.675000", "-58.562800", 2000), zonaCercana);
    }

    private void dadoSoloUnaZonaDeCabaQueNoContieneElPunto() {
        zonas = List.of(unaZonaDeCaba());
    }

    private Zona unaZonaDeCaba() {
        return unaZonaCentradaEn("-34.603700", "-58.381600", 1500);
    }

    // --- cuando ---
    private void cuandoSeMideLaDistanciaAlCentro() {
        distancia = zona.distanciaMetros(LATITUD, LONGITUD);
    }

    private void cuandoSeMideLaDistanciaAUnPuntoA111Metros() {
        distancia = zona.distanciaMetros(LATITUD_A_111_METROS, LONGITUD);
    }

    private void cuandoSePreguntaSiContieneUnPuntoA111Metros() {
        contiene = zona.contiene(LATITUD_A_111_METROS, LONGITUD);
    }

    private void cuandoSeResuelveLaZonaDelPunto() {
        resuelta = Zona.masCercana(zonas, LATITUD_A_111_METROS, LONGITUD);
    }

    private void cuandoSeBuscaCoincidenciaConElTexto(String texto) {
        coincide = zona.coincideCon(texto);
    }

    // --- entonces ---
    private void entoncesLaDistanciaEsCero() {
        assertThat(distancia).isZero();
    }

    private void entoncesLaDistanciaEsDeAlrededorDe111Metros() {
        assertThat(distancia).isBetween(111.0, 111.4);
    }

    private void entoncesLoContiene() {
        assertThat(contiene).isTrue();
    }

    private void entoncesNoLoContiene() {
        assertThat(contiene).isFalse();
    }

    private void entoncesSeResuelveLaZonaCercana() {
        assertThat(resuelta).containsSame(zonaCercana);
    }

    private void entoncesNoSeResuelveNingunaZona() {
        assertThat(resuelta).isEmpty();
    }

    private void entoncesCoincide() {
        assertThat(coincide).isTrue();
    }

    private void entoncesNoCoincide() {
        assertThat(coincide).isFalse();
    }
}
