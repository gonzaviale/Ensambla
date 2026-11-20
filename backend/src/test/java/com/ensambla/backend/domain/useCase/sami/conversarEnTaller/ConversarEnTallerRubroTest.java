package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.support.SamiPruebas;
import com.ensambla.backend.support.TallerDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ConversarEnTallerRubroTest {

    private static final EstadoTaller SIN_NOMBRE = new EstadoTaller(null, "mosaic", List.of());

    private TallerDePrueba taller;
    private EstadoTaller estado = SIN_NOMBRE;
    private List<MensajeTaller> historial = List.of();
    private Optional<KitRubro> kit;

    @BeforeEach
    void setUp() {
        taller = new TallerDePrueba(SamiPruebas.kitsReales());
    }

    @Test
    @DisplayName("Reconoce el rubro en el mensaje aunque venga en plural, con mayúsculas o con tildes")
    void reconoceElRubroEnElMensajeAunqueVengaEnPluralConMayusculasOConTildes() {
        cuandoSeDetectaElRubroDe("Vendo VELAS y anoto quién me debe");

        entoncesElKitEs("velas");
    }

    @Test
    @DisplayName("Reconoce términos de varias palabras")
    void reconoceTerminosDeVariasPalabras() {
        cuandoSeDetectaElRubroDe("tengo un taller mecánico");

        entoncesElKitEs("taller-mecanico");
    }

    @Test
    @DisplayName("Sin términos de ningún rubro no elige kit")
    void sinTerminosDeNingunRubroNoEligeKit() {
        cuandoSeDetectaElRubroDe("llevo los arreglos de bicis");

        entoncesNoSeEligeNingunKit();
    }

    @Test
    @DisplayName("Si el mensaje no lo dice, usa la charla reciente")
    void siElMensajeNoLoDiceUsaLaCharlaReciente() {
        dadoUnaCharlaPreviaDondeDiceQueTieneUnaPeluqueria();

        cuandoSeDetectaElRubroDe("sí, dale");

        entoncesElKitEs("peluqueria");
    }

    @Test
    @DisplayName("Si el mensaje no lo dice, usa el nombre del sistema")
    void siElMensajeNoLoDiceUsaElNombreDelSistema() {
        dadoUnSistemaLlamado("Ferretería Don Luis");

        cuandoSeDetectaElRubroDe("sumá gastos");

        entoncesElKitEs("ferreteria");
    }

    @Test
    @DisplayName("Lo que dice el mensaje pesa más que la charla anterior")
    void loQueDiceElMensajePesaMasQueLaCharlaAnterior() {
        dadoUnaCharlaPreviaDondeDiceQueVendeVelas();

        cuandoSeDetectaElRubroDe("en realidad tengo una panadería");

        entoncesElKitEs("panaderia");
    }

    @Test
    @DisplayName("Un término de varias palabras le gana a uno suelto de otro rubro")
    void unTerminoDeVariasPalabrasLeGanaAUnoSueltoDeOtroRubro() {
        cuandoSeDetectaElRubroDe("vendo frutos secos en mi almacén");

        entoncesElKitEs("dietetica");
    }

    @Test
    @DisplayName("Ante un empate gana el kit que figura primero")
    void anteUnEmpateGanaElKitQueFiguraPrimero() {
        cuandoSeDetectaElRubroDe("kiosco y ferretería");

        entoncesElKitEs("almacen");
    }

    @Test
    @DisplayName("Sin kits no elige nada")
    void sinKitsNoEligeNada() {
        dadoUnTallerSinKits();

        cuandoSeDetectaElRubroDe("vendo velas");

        entoncesNoSeEligeNingunKit();
    }

    @Test
    @DisplayName("Con un mensaje vacío no elige nada")
    void conUnMensajeVacioNoEligeNada() {
        cuandoSeDetectaElRubroDe(null);

        entoncesNoSeEligeNingunKit();
    }

    // --- dado ---

    private void dadoUnaCharlaPreviaDondeDiceQueTieneUnaPeluqueria() {
        historial = List.of(new MensajeTaller("you", "tengo una peluquería"));
    }

    private void dadoUnaCharlaPreviaDondeDiceQueVendeVelas() {
        historial = List.of(new MensajeTaller("you", "vendo velas"));
    }

    private void dadoUnSistemaLlamado(String nombre) {
        estado = new EstadoTaller(nombre, "mosaic", List.of());
    }

    private void dadoUnTallerSinKits() {
        taller = new TallerDePrueba(new KitsRubro(List.of()));
    }

    // --- cuando ---

    private void cuandoSeDetectaElRubroDe(String mensaje) {
        kit = taller.rubro(mensaje, estado, historial);
    }

    // --- entonces ---

    private void entoncesElKitEs(String id) {
        assertThat(kit).map(KitRubro::id).contains(id);
    }

    private void entoncesNoSeEligeNingunKit() {
        assertThat(kit).isEmpty();
    }
}
