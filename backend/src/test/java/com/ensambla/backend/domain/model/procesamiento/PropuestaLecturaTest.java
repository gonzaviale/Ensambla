package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropuestaLecturaTest {

    @Test
    @DisplayName("Una propuesta sin lista de campos nuevos (lectura guardada antes) la lee como vacía")
    void constructorShouldTreatNullNewFieldsAsEmpty() {
        PropuestaLectura propuesta = new PropuestaLectura(ProcesamientoTestData.lectura(), Map.of(), null);

        assertThat(propuesta.camposNuevos()).isEmpty();
    }

    @Test
    @DisplayName("Conserva los campos nuevos que se le pasan")
    void constructorShouldKeepNewFields() {
        CampoPropuesto telefono = new CampoPropuesto("telefono", "Teléfono", TipoCampo.TEXTO, List.of());

        PropuestaLectura propuesta = new PropuestaLectura(
                ProcesamientoTestData.lectura(), Map.of(), List.of(telefono));

        assertThat(propuesta.camposNuevos()).containsExactly(telefono);
    }

    @Test
    @DisplayName("El constructor corto deja los campos nuevos vacíos")
    void constructorShouldDefaultToNoNewFields() {
        PropuestaLectura propuesta = new PropuestaLectura(ProcesamientoTestData.lectura(), Map.of());

        assertThat(propuesta.camposNuevos()).isEmpty();
    }

    @Test
    @DisplayName("Se guarda y se lee como JSON, y una lectura guardada sin campos nuevos se sigue pudiendo leer")
    void jsonShouldRoundTripAndAcceptStoredProposalWithoutNewFields() {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        CampoPropuesto telefono = new CampoPropuesto("telefono", "Teléfono", TipoCampo.TEXTO, List.of());
        PropuestaLectura propuesta = new PropuestaLectura(ProcesamientoTestData.lectura(),
                Map.of("monto", ProcesamientoTestData.CAMPO_ID), List.of(telefono));

        String json = objectMapper.writeValueAsString(propuesta);
        JsonNode guardadaAntes = objectMapper.readTree(json);
        ((ObjectNode) guardadaAntes).remove("camposNuevos");

        assertThat(objectMapper.readValue(json, PropuestaLectura.class)).isEqualTo(propuesta);
        assertThat(objectMapper.readValue(guardadaAntes.toString(), PropuestaLectura.class).camposNuevos()).isEmpty();
    }

    @Test
    @DisplayName("Propone un bloque nuevo cuando el negocio no tiene bloques")
    void proponerShouldProposeNewBlockWhenEmpty() {
        PropuestaLectura propuesta = PropuestaLectura.proponer(ProcesamientoTestData.lectura(), List.of(), null);

        assertThat(propuesta.lectura().bloque().bloqueExistenteId()).isNull();
        assertThat(propuesta.mapeoCampos()).isEmpty();
    }

    @Test
    @DisplayName("Reutiliza el mismo bloque y mapea columnas compatibles ignorando acentos")
    void proponerShouldReuseCompatibleBlock() {
        BloqueLectura bloque = givenBlock(" Véntas ", "Mónto", TipoCampo.NUMERO, List.of());

        PropuestaLectura propuesta = PropuestaLectura.proponer(
                ProcesamientoTestData.lectura(), List.of(bloque), null);

        assertThat(propuesta.lectura().bloque().bloqueExistenteId()).isEqualTo(bloque.id());
        assertThat(propuesta.mapeoCampos()).containsEntry("monto", ProcesamientoTestData.CAMPO_ID);
        assertThat(propuesta.lectura().filas()).isEqualTo(ProcesamientoTestData.lectura().filas());
    }

    @Test
    @DisplayName("Deja una propuesta nueva si el nombre del bloque no coincide")
    void proponerShouldRejectDifferentBlockName() {
        BloqueLectura bloque = givenBlock("Gastos", "Monto", TipoCampo.NUMERO, List.of());

        PropuestaLectura propuesta = PropuestaLectura.proponer(
                ProcesamientoTestData.lectura(), List.of(bloque), null);

        assertThat(propuesta.mapeoCampos()).isEmpty();
    }

    @Test
    @DisplayName("No reutiliza columnas con nombres distintos")
    void proponerShouldRejectDifferentFieldName() {
        BloqueLectura bloque = givenBlock("Ventas", "Cantidad", TipoCampo.NUMERO, List.of());

        PropuestaLectura propuesta = PropuestaLectura.proponer(
                ProcesamientoTestData.lectura(), List.of(bloque), null);

        assertThat(propuesta.mapeoCampos()).isEmpty();
    }

    @Test
    @DisplayName("No reutiliza columnas con tipos incompatibles")
    void proponerShouldRejectDifferentFieldType() {
        BloqueLectura bloque = givenBlock("Ventas", "Monto", TipoCampo.TEXTO, List.of());

        PropuestaLectura propuesta = PropuestaLectura.proponer(
                ProcesamientoTestData.lectura(), List.of(bloque), null);

        assertThat(propuesta.mapeoCampos()).isEmpty();
    }

    @Test
    @DisplayName("No reutiliza columnas cuyas opciones no incluyen las leídas")
    void proponerShouldRejectDifferentOptions() {
        BloqueLectura bloque = givenBlock("Ventas", "Monto", TipoCampo.OPCION, List.of("A"));

        PropuestaLectura propuesta = PropuestaLectura.proponer(
                ProcesamientoTestData.lectura(TipoCampo.OPCION, "B", List.of("B")), List.of(bloque), null);

        assertThat(propuesta.mapeoCampos()).isEmpty();
    }

    @Test
    @DisplayName("No decide automáticamente entre dos bloques compatibles")
    void proponerShouldKeepAmbiguousProposal() {
        BloqueLectura primero = givenBlock("Ventas", "Monto", TipoCampo.NUMERO, List.of());
        BloqueLectura segundo = givenBlock("Ventas", "Monto", TipoCampo.NUMERO, List.of());

        PropuestaLectura propuesta = PropuestaLectura.proponer(
                ProcesamientoTestData.lectura(), List.of(primero, segundo), null);

        assertThat(propuesta.lectura().bloque().bloqueExistenteId()).isNull();
    }

    @Test
    @DisplayName("Respeta un destino explícito entre varios compatibles")
    void proponerShouldUseExplicitDestination() {
        BloqueLectura primero = givenBlock("Ventas", "Monto", TipoCampo.NUMERO, List.of());
        BloqueLectura segundo = givenBlock("Ventas", "Monto", TipoCampo.NUMERO, List.of());

        PropuestaLectura propuesta = PropuestaLectura.proponer(
                ProcesamientoTestData.lectura(), List.of(primero, segundo), segundo.id());

        assertThat(propuesta.lectura().bloque().bloqueExistenteId()).isEqualTo(segundo.id());
    }

    @Test
    @DisplayName("Rechaza un destino inexistente o incompatible")
    void proponerShouldFailWhenDestinationMissing() {
        assertThatThrownBy(() -> PropuestaLectura.proponer(
                ProcesamientoTestData.lectura(), List.of(), BloqueId.nuevo()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("compatible");
    }

    @Test
    @DisplayName("Con destino propone como campo nuevo la columna que el bloque no tiene, sin duplicar las que tiene")
    void proponerShouldDetectNewColumnWhenDestinationLacksIt() {
        BloqueLectura bloque = givenBlock("Ventas", "Monto", TipoCampo.NUMERO, List.of());
        LecturaCuaderno lectura = givenLectura(
                List.of(columna("monto", "Monto", TipoCampo.NUMERO), columna("telefono", "Teléfono", TipoCampo.TEXTO)),
                givenFila(1, "monto", "100", "telefono", "11 4444-5555"));

        PropuestaLectura propuesta = PropuestaLectura.proponer(lectura, List.of(bloque), bloque.id());

        thenNuevosAre(propuesta, "telefono");
        assertThat(propuesta.camposNuevos().getFirst().tipo()).isEqualTo(TipoCampo.TEXTO);
        assertThat(propuesta.mapeoCampos()).containsOnlyKeys("monto");
        assertThat(propuesta.lectura().bloque().campos()).extracting(CampoPropuesto::clave)
                .containsExactly("monto", "telefono");
        assertThat(propuesta.lectura().bloque().bloqueExistenteId()).isEqualTo(bloque.id());
        assertThat(propuesta.lectura().filas()).isEqualTo(lectura.filas());
    }

    @Test
    @DisplayName("Con destino, una columna que existe pasa a llamarse y tipificarse como el campo del bloque")
    void proponerShouldAdoptExistingFieldDefinition() {
        BloqueLectura bloque = givenBlock("Ventas", "Monto", TipoCampo.NUMERO, List.of());
        LecturaCuaderno lectura = givenLectura(
                List.of(columna("monto", " MÓNTO ", TipoCampo.TEXTO)), givenFila(1, "monto", "100"));

        PropuestaLectura propuesta = PropuestaLectura.proponer(lectura, List.of(bloque), bloque.id());

        CampoPropuesto campo = propuesta.lectura().bloque().campos().getFirst();
        assertThat(campo.nombre()).isEqualTo("Monto");
        assertThat(campo.tipo()).isEqualTo(TipoCampo.NUMERO);
        assertThat(propuesta.camposNuevos()).isEmpty();
    }

    @Test
    @DisplayName("Con destino, el nombre del bloque leído no importa: se usa el del bloque elegido")
    void proponerShouldUseDestinationName() {
        BloqueLectura bloque = givenBlock("Mis ventas", "Monto", TipoCampo.NUMERO, List.of());

        PropuestaLectura propuesta = PropuestaLectura.proponer(
                ProcesamientoTestData.lectura(), List.of(bloque), bloque.id());

        assertThat(propuesta.lectura().bloque().nombre()).isEqualTo("Mis ventas");
    }

    @Test
    @DisplayName("Con destino, un sinónimo del campo existente no se propone como campo nuevo")
    void proponerShouldMapSynonymToExistingField() {
        BloqueLectura bloque = givenBlock("Clientes", "Celular", TipoCampo.TEXTO, List.of());
        LecturaCuaderno lectura = givenLectura(
                List.of(columna("telefono", "Teléfono", TipoCampo.TEXTO)), givenFila(1, "telefono", "11 4444-5555"));

        PropuestaLectura propuesta = PropuestaLectura.proponer(lectura, List.of(bloque), bloque.id());

        assertThat(propuesta.mapeoCampos()).containsEntry("telefono", ProcesamientoTestData.CAMPO_ID);
        assertThat(propuesta.lectura().bloque().campos().getFirst().nombre()).isEqualTo("Celular");
        assertThat(propuesta.camposNuevos()).isEmpty();
    }

    @Test
    @DisplayName("Con destino, el nombre igual gana al sinónimo y cada campo se usa una sola vez")
    void proponerShouldPreferExactNameOverSynonym() {
        UUID telefonoId = UUID.randomUUID();
        UUID celularId = UUID.randomUUID();
        BloqueLectura bloque = new BloqueLectura(BloqueId.nuevo(), "Clientes", List.of(
                new CampoConocido(CampoId.de(celularId), "Celular", TipoCampo.TEXTO, List.of()),
                new CampoConocido(CampoId.de(telefonoId), "Teléfono", TipoCampo.TEXTO, List.of())));
        LecturaCuaderno lectura = givenLectura(
                List.of(columna("cel", "Cel", TipoCampo.TEXTO), columna("tel", "Teléfono", TipoCampo.TEXTO)),
                givenFila(1, "cel", "1", "tel", "2"));

        PropuestaLectura propuesta = PropuestaLectura.proponer(lectura, List.of(bloque), bloque.id());

        assertThat(propuesta.mapeoCampos()).containsEntry("tel", CampoId.de(telefonoId))
                .containsEntry("cel", CampoId.de(celularId));
        assertThat(propuesta.camposNuevos()).isEmpty();
    }

    @Test
    @DisplayName("Con destino, una columna que podría ser dos campos se propone como nueva y se avisa")
    void proponerShouldWarnWhenSynonymIsAmbiguous() {
        BloqueLectura bloque = new BloqueLectura(BloqueId.nuevo(), "Clientes", List.of(
                new CampoConocido(CampoId.nuevo(), "Celular", TipoCampo.TEXTO, List.of()),
                new CampoConocido(ProcesamientoTestData.CAMPO_ID, "Teléfono", TipoCampo.TEXTO, List.of()),
                new CampoConocido(CampoId.nuevo(), "Nombre", TipoCampo.TEXTO, List.of())));
        LecturaCuaderno lectura = givenLectura(
                List.of(columna("nombre", "Nombre", TipoCampo.TEXTO), columna("tel", "Tel", TipoCampo.TEXTO)),
                givenFila(1, "nombre", "Ana", "tel", "11 4444-5555"));

        PropuestaLectura propuesta = PropuestaLectura.proponer(lectura, List.of(bloque), bloque.id());

        thenNuevosAre(propuesta, "tel");
        assertThat(propuesta.lectura().advertencias()).anyMatch(aviso -> aviso.contains("«Tel»")
                && aviso.contains("«Clientes»"));
    }

    @Test
    @DisplayName("Con destino, una columna nueva sin datos no se propone y sus celdas se descartan")
    void proponerShouldIgnoreNewColumnsWithoutData() {
        BloqueLectura bloque = givenBlock("Ventas", "Monto", TipoCampo.NUMERO, List.of());
        LecturaCuaderno lectura = givenLectura(
                List.of(columna("monto", "Monto", TipoCampo.NUMERO), columna("nota", "Nota", TipoCampo.TEXTO),
                        columna("color", "Color", TipoCampo.TEXTO), columna("zona", "Zona", TipoCampo.TEXTO)),
                givenFila(1, "monto", "100", "nota", null, "color", "  "),
                givenFila(2, "monto", "200", "nota", null));

        PropuestaLectura propuesta = PropuestaLectura.proponer(lectura, List.of(bloque), bloque.id());

        assertThat(propuesta.camposNuevos()).isEmpty();
        assertThat(propuesta.lectura().bloque().campos()).extracting(CampoPropuesto::clave).containsExactly("monto");
        assertThat(propuesta.lectura().filas()).allSatisfy(fila -> assertThat(fila.datos()).containsOnlyKeys("monto"));
        assertThat(propuesta.lectura().advertencias()).contains(
                "La columna «Nota» no tiene datos en la foto, así que no se propone.",
                "La columna «Color» no tiene datos en la foto, así que no se propone.",
                "La columna «Zona» no tiene datos en la foto, así que no se propone.");
    }

    @Test
    @DisplayName("Con destino, si ninguna columna coincide con el bloque la foto no es de ese bloque")
    void proponerShouldRejectWhenNoColumnMatchesDestination() {
        BloqueLectura bloque = givenBlock("Ventas", "Monto", TipoCampo.NUMERO, List.of());
        LecturaCuaderno lectura = givenLectura(
                List.of(columna("color", "Color", TipoCampo.TEXTO)), givenFila(1, "color", "rojo"));

        assertThatThrownBy(() -> PropuestaLectura.proponer(lectura, List.of(bloque), bloque.id()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("compatible");
    }

    private void thenNuevosAre(PropuestaLectura propuesta, String... claves) {
        assertThat(propuesta.camposNuevos()).extracting(CampoPropuesto::clave).containsExactly(claves);
        assertThat(propuesta.mapeoCampos()).doesNotContainKeys(claves);
    }

    private CampoPropuesto columna(String clave, String nombre, TipoCampo tipo) {
        return new CampoPropuesto(clave, nombre, tipo, List.of());
    }

    /** {@code datos}: pares clave, valor. */
    private FilaLeida givenFila(int renglon, String... datos) {
        Map<String, DatoLeido> mapa = new LinkedHashMap<>();
        for (int i = 0; i < datos.length; i += 2) {
            mapa.put(datos[i], new DatoLeido(datos[i + 1], 0.9));
        }
        return new FilaLeida(1, renglon, mapa);
    }

    private LecturaCuaderno givenLectura(List<CampoPropuesto> campos, FilaLeida... filas) {
        return new LecturaCuaderno(TipoRegistroCuaderno.OTRO, new BloquePropuesto("Ventas", null, campos),
                List.of(filas), List.of("Aviso"), new ConsumoIa("simulado", "demo", "v3", 1, 10, 20, 30));
    }

    private BloqueLectura givenBlock(String nombre, String campo, TipoCampo tipo, List<String> opciones) {
        return new BloqueLectura(BloqueId.nuevo(), nombre,
                List.of(new CampoConocido(ProcesamientoTestData.CAMPO_ID, campo, tipo, opciones)));
    }
}
