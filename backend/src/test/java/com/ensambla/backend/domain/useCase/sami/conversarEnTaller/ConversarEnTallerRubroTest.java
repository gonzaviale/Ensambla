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

    @BeforeEach
    void setUp() {
        taller = new TallerDePrueba(SamiPruebas.kitsReales());
    }

    @Test
    @DisplayName("Reconoce el rubro en el mensaje aunque venga en plural, con mayúsculas o con tildes")
    void executeShouldFindKitInMessage() {
        Optional<KitRubro> kit = detectar("Vendo VELAS y anoto quién me debe");

        thenKitIs(kit, "velas");
    }

    @Test
    @DisplayName("Reconoce términos de varias palabras")
    void executeShouldFindMultiWordTerms() {
        Optional<KitRubro> kit = detectar("tengo un taller mecánico");

        thenKitIs(kit, "taller-mecanico");
    }

    @Test
    @DisplayName("Sin términos de ningún rubro no elige kit")
    void executeShouldReturnEmptyWithoutTerms() {
        Optional<KitRubro> kit = detectar("llevo los arreglos de bicis");

        assertThat(kit).isEmpty();
    }

    @Test
    @DisplayName("Si el mensaje no lo dice, usa la charla reciente y el nombre del sistema")
    void executeShouldUseHistoryAndSystemName() {
        Optional<KitRubro> porCharla = taller.rubro("sí, dale", SIN_NOMBRE,
                List.of(new MensajeTaller("you", "tengo una peluquería")));
        Optional<KitRubro> porNombre = taller.rubro("sumá gastos",
                new EstadoTaller("Ferretería Don Luis", "mosaic", List.of()), List.of());

        thenKitIs(porCharla, "peluqueria");
        thenKitIs(porNombre, "ferreteria");
    }

    @Test
    @DisplayName("Lo que dice el mensaje pesa más que la charla anterior")
    void executeShouldPreferTheMessageOverHistory() {
        Optional<KitRubro> kit = taller.rubro("en realidad tengo una panadería", SIN_NOMBRE,
                List.of(new MensajeTaller("you", "vendo velas")));

        thenKitIs(kit, "panaderia");
    }

    @Test
    @DisplayName("Un término de varias palabras le gana a uno suelto de otro rubro")
    void executeShouldPreferLongerTerms() {
        Optional<KitRubro> kit = detectar("vendo frutos secos en mi almacén");

        thenKitIs(kit, "dietetica");
    }

    @Test
    @DisplayName("Ante un empate gana el kit que figura primero")
    void executeShouldBreakTiesByOrder() {
        Optional<KitRubro> kit = detectar("kiosco y ferretería");

        thenKitIs(kit, "almacen");
    }

    @Test
    @DisplayName("Sin kits o con un mensaje vacío no elige nada")
    void executeShouldReturnEmptyWithoutKitsOrMessage() {
        Optional<KitRubro> sinKits = new TallerDePrueba(new KitsRubro(List.of()))
                .rubro("vendo velas", SIN_NOMBRE, List.of());
        Optional<KitRubro> sinMensaje = taller.rubro(null, SIN_NOMBRE, List.of());

        assertThat(sinKits).isEmpty();
        assertThat(sinMensaje).isEmpty();
    }

    // --- act ---
    private Optional<KitRubro> detectar(String mensaje) {
        return taller.rubro(mensaje, SIN_NOMBRE, List.of());
    }

    // --- assert ---
    private void thenKitIs(Optional<KitRubro> kit, String id) {
        assertThat(kit).map(KitRubro::id).contains(id);
    }
}
