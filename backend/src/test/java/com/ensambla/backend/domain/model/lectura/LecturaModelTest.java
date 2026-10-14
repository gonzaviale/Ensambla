package com.ensambla.backend.domain.model.lectura;

import com.ensambla.backend.domain.model.enums.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LecturaModelTest {

    private static final double UMBRAL = 0.8;
    private static final List<CampoPropuesto> CAMPOS =
            List.of(new CampoPropuesto("monto", "Monto", TipoCampo.NUMERO, List.of()));

    @Test
    @DisplayName("Un dato es dudoso cuando la confianza no llega al umbral")
    void esDudosoShouldBeTrueWhenConfianzaIsBelowUmbral() {
        assertThat(new DatoLeido("Marta", 0.79).esDudoso(UMBRAL)).isTrue();
    }

    @Test
    @DisplayName("Un dato no es dudoso cuando la confianza alcanza el umbral")
    void esDudosoShouldBeFalseWhenConfianzaReachesUmbral() {
        assertThat(new DatoLeido("Marta", 0.8).esDudoso(UMBRAL)).isFalse();
    }

    @Test
    @DisplayName("Una fila tiene dudas si algún dato está por debajo del umbral")
    void tieneDudasShouldBeTrueWhenAnyDatoIsDudoso() {
        FilaLeida fila = new FilaLeida(1, 1, Map.of("cliente", new DatoLeido("Rosa", 0.6),
                "monto", new DatoLeido("8000", 0.95)));

        assertThat(fila.tieneDudas(UMBRAL)).isTrue();
    }

    @Test
    @DisplayName("Una fila no tiene dudas si todos sus datos son confiables")
    void tieneDudasShouldBeFalseWhenEveryDatoIsConfiable() {
        FilaLeida fila = new FilaLeida(1, 1, Map.of("monto", new DatoLeido("8000", 0.95)));

        assertThat(fila.tieneDudas(UMBRAL)).isFalse();
    }

    @Test
    @DisplayName("La fila copia sus datos: cambiar el mapa original no la altera")
    void filaShouldCopyDatos() {
        Map<String, DatoLeido> datos = new HashMap<>(Map.of("monto", new DatoLeido("8000", 0.95)));
        FilaLeida fila = new FilaLeida(1, 1, datos);

        datos.clear();

        assertThat(fila.datos()).containsKey("monto");
    }

    @Test
    @DisplayName("Expone el bloque existente cuando la IA sugiere sumar las filas a uno del negocio")
    void bloqueExistenteShouldContainIdWhenPresent() {
        UUID id = UUID.randomUUID();

        assertThat(new BloquePropuesto("Ventas", id, CAMPOS).bloqueExistente()).contains(id);
    }

    @Test
    @DisplayName("No expone bloque existente cuando la IA propone uno nuevo")
    void bloqueExistenteShouldBeEmptyWhenNull() {
        assertThat(new BloquePropuesto("Ventas", null, CAMPOS).bloqueExistente()).isEmpty();
    }

    @Test
    @DisplayName("El bloque conocido copia la lista de campos")
    void bloqueConocidoShouldCopyCampos() {
        CampoContexto fecha = new CampoContexto("Fecha", TipoCampo.FECHA, List.of());
        List<CampoContexto> campos = new ArrayList<>(List.of(fecha));
        BloqueConocido bloque = new BloqueConocido(UUID.randomUUID(), "Ventas", campos);

        campos.add(new CampoContexto("Monto", TipoCampo.NUMERO, List.of()));

        assertThat(bloque.campos()).containsExactly(fecha);
    }

    @Test
    @DisplayName("El campo de contexto copia la lista de opciones")
    void campoContextoShouldCopyOpciones() {
        List<String> opciones = new ArrayList<>(List.of("Pagado"));
        CampoContexto campo = new CampoContexto("Estado", TipoCampo.OPCION, opciones);

        opciones.add("Debe");

        assertThat(campo.opciones()).containsExactly("Pagado");
    }
}
