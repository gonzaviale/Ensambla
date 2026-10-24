package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen.CampoDeImagen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class LecturaImagenTest {

    private final BloqueDeImagen bloque = BloqueDeImagen.de("Ventas", List.of(
            CampoDeImagen.nuevo("importe", "Monto", TipoCampo.NUMERO, List.of()),
            CampoDeImagen.nuevo("cliente", "Cliente", TipoCampo.TEXTO, List.of())));

    @Test
    @DisplayName("Los datos leídos van por la clave del campo del bloque y la columna que falta se propone como nueva")
    void desdeShouldKeyValuesByBlockFieldAndProposeNewColumn() {
        PropuestaLectura propuesta = givenPropuesta(
                List.of(columna("telefono", "Teléfono", TipoCampo.TEXTO)),
                fila(dato("monto", "1500", 0.95), dato("cliente", "Ana", 0.9), dato("telefono", "1155", 0.5)));

        LecturaImagen lectura = LecturaImagen.desde(propuesta, bloque, true);

        assertThat(lectura.camposNuevos())
                .extracting(CampoPropuesto::clave, CampoPropuesto::nombre, CampoPropuesto::tipo)
                .containsExactly(tuple("telefono", "Teléfono", TipoCampo.TEXTO));
        assertThat(lectura.filas()).hasSize(1);
        Map<String, DatoLeido> datos = lectura.filas().getFirst();
        assertThat(datos.keySet()).containsExactlyInAnyOrder("importe", "cliente", "telefono");
        assertThat(datos.get("importe").valor()).isEqualTo("1500");
        assertThat(datos.get("telefono").confianza()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("Si el bloque todavía no existe no se proponen columnas nuevas ni se devuelven sus valores")
    void desdeShouldIgnoreNewColumnsWhenTheBlockDoesNotExist() {
        PropuestaLectura propuesta = givenPropuesta(
                List.of(columna("telefono", "Teléfono", TipoCampo.TEXTO)),
                fila(dato("monto", "1500", 0.9), dato("telefono", "1155", 0.9)));

        LecturaImagen lectura = LecturaImagen.desde(propuesta, bloque, false);

        assertThat(lectura.camposNuevos()).isEmpty();
        assertThat(lectura.filas().getFirst().keySet()).containsExactly("importe");
    }

    @Test
    @DisplayName("La clave de una columna nueva no pisa la de un campo del bloque ni la de otra columna nueva")
    void desdeShouldGiveNewColumnsUniqueKeys() {
        BloqueDeImagen conMarca = BloqueDeImagen.de("Ventas", List.of(
                CampoDeImagen.nuevo("marca", "Producto", TipoCampo.TEXTO, List.of())));
        PropuestaLectura propuesta = new PropuestaLectura(
                lectura(List.of(columna("a", "Producto", TipoCampo.TEXTO)),
                        fila(dato("a", "Yerba", 0.9), dato("b", "Taragüí", 0.9), dato("c", "Otra", 0.9),
                                dato("d", "x", 0.9))),
                Map.of("a", conMarca.campos().getFirst().id()),
                List.of(columna("b", "Marca", TipoCampo.TEXTO), columna("c", "¡Marca!", TipoCampo.TEXTO),
                        columna("d", "¿?", TipoCampo.TEXTO)));

        LecturaImagen lectura = LecturaImagen.desde(propuesta, conMarca, true);

        assertThat(lectura.camposNuevos()).extracting(CampoPropuesto::clave)
                .containsExactly("marca_2", "marca_3", "columna");
        assertThat(lectura.filas().getFirst().keySet())
                .containsExactlyInAnyOrder("marca", "marca_2", "marca_3", "columna");
    }

    @Test
    @DisplayName("Un dato de una columna que no es del bloque ni se propuso como nueva se descarta")
    void desdeShouldDropValuesOfUnknownColumns() {
        PropuestaLectura propuesta = givenPropuesta(List.of(), fila(dato("monto", "1", 0.9), dato("otra", "x", 0.9)));

        LecturaImagen lectura = LecturaImagen.desde(propuesta, bloque, true);

        assertThat(lectura.filas().getFirst().keySet()).containsExactly("importe");
    }

    @Test
    @DisplayName("Devuelve los avisos de la lectura")
    void desdeShouldKeepTheReadingWarnings() {
        PropuestaLectura propuesta = new PropuestaLectura(
                new LecturaCuaderno(TipoRegistroCuaderno.OTRO, new BloquePropuesto("Hoja", null, List.of()),
                        List.of(), List.of("Página torcida"), consumo()),
                Map.of(), List.of());

        LecturaImagen lectura = LecturaImagen.desde(propuesta, bloque, true);

        assertThat(lectura.advertencias()).containsExactly("Página torcida");
        assertThat(lectura.filas()).isEmpty();
    }

    // --- arrange ---
    private PropuestaLectura givenPropuesta(List<CampoPropuesto> nuevas, FilaLeida... filas) {
        Map<String, UUID> mapeo = Map.of(
                "monto", bloque.campos().get(0).id(), "cliente", bloque.campos().get(1).id());
        return new PropuestaLectura(lectura(List.of(), filas), mapeo, nuevas);
    }

    private LecturaCuaderno lectura(List<CampoPropuesto> campos, FilaLeida... filas) {
        return new LecturaCuaderno(TipoRegistroCuaderno.OTRO, new BloquePropuesto("Ventas", null, campos),
                List.of(filas), List.of(), consumo());
    }

    private ConsumoIa consumo() {
        return new ConsumoIa("simulado", "demo", "v3", 1, 0, 0, 1);
    }

    private CampoPropuesto columna(String clave, String nombre, TipoCampo tipo) {
        return new CampoPropuesto(clave, nombre, tipo, List.of());
    }

    private Map.Entry<String, DatoLeido> dato(String clave, String valor, double confianza) {
        return Map.entry(clave, new DatoLeido(valor, confianza));
    }

    @SafeVarargs
    private FilaLeida fila(Map.Entry<String, DatoLeido>... datos) {
        Map<String, DatoLeido> mapa = new LinkedHashMap<>();
        for (Map.Entry<String, DatoLeido> dato : datos) {
            mapa.put(dato.getKey(), dato.getValue());
        }
        return new FilaLeida(1, 1, mapa);
    }
}
