package com.ensambla.backend.infrastructure.presentation.mapper.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.LeerImagenItem;
import com.ensambla.backend.domain.dto.procesamiento.LeerImagenItem.CampoItem;
import com.ensambla.backend.domain.dto.procesamiento.LeerImagenItem.DatoItem;
import com.ensambla.backend.domain.dto.procesamiento.LeerImagenRequest;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.LecturaImagenResponse;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.LecturaImagenResponse.CampoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.PedidoLecturaRequest;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.PedidoLecturaRequest.CampoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LecturaPresentationMapperTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");

    private final LecturaPresentationMapper mapper = new LecturaPresentationMapper();

    @Test
    @DisplayName("Arma el pedido de lectura con los tipos del dominio y sin los campos que apuntan a otro bloque")
    void toRequestShouldTranslateFieldTypesAndSkipRelations() {
        PedidoLecturaRequest pedido = pedido(true,
                campo("nombre", "Nombre", "text"), campo("nota", "Nota", "textarea"), campo("mail", "Mail", "email"),
                campo("cel", "Celular", "tel"), campo("hora", "Hora", "time"), campo("cant", "Cantidad", "number"),
                campo("monto", "Monto", "currency"), campo("dia", "Día", "date"),
                campo("estado", "Estado", "select", "A", "B"), campo("pago", "Pagó", "checkbox"),
                campo("cliente", "Cliente", "relation"));

        LeerImagenRequest request = mapper.toRequest(USUARIO_ID,
                new MockMultipartFile("imagen", "foto.jpg", "image/jpeg", new byte[]{1, 2}), pedido);

        assertThat(request.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(request.contenido()).containsExactly(1, 2);
        assertThat(request.nombreBloque()).isEqualTo("Ventas");
        assertThat(request.destinoExistente()).isTrue();
        assertThat(request.campos()).extracting("clave", "tipo").containsExactly(
                tuple("nombre", "TEXTO"), tuple("nota", "TEXTO"), tuple("mail", "TEXTO"), tuple("cel", "TEXTO"),
                tuple("hora", "TEXTO"), tuple("cant", "NUMERO"), tuple("monto", "NUMERO"), tuple("dia", "FECHA"),
                tuple("estado", "OPCION"), tuple("pago", "SI_NO"));
        assertThat(request.campos().get(8).opciones()).containsExactly("A", "B");
    }

    @Test
    @DisplayName("No se puede leer la imagen recibida")
    void toRequestShouldFailWhenTheImageCannotBeRead() throws IOException {
        MultipartFile imagen = mock(MultipartFile.class);
        when(imagen.getBytes()).thenThrow(new IOException("disco"));

        assertThatThrownBy(() -> mapper.toRequest(USUARIO_ID, imagen, pedido(false, campo("a", "A", "text"))))
                .isInstanceOf(UncheckedIOException.class).hasMessageContaining("No se pudo leer la imagen");
    }

    @Test
    @DisplayName("Convierte los valores leídos a lo que espera cada campo y completa los que faltan")
    void toResponseShouldConvertValuesAndFillMissingOnes() {
        PedidoLecturaRequest pedido = pedido(true,
                campo("nombre", "Nombre", "text"), campo("cant", "Cantidad", "number"),
                campo("monto", "Monto", "currency"), campo("dia", "Día", "date"),
                campo("pago", "Pagó", "checkbox"), campo("cliente", "Cliente", "relation"));
        Map<String, DatoItem> primera = new LinkedHashMap<>();
        primera.put("nombre", new DatoItem("Ana", false));
        primera.put("cant", new DatoItem("3", false));
        primera.put("monto", new DatoItem("1500.50", false));
        primera.put("dia", new DatoItem("2026-10-19", false));
        primera.put("pago", new DatoItem("true", false));
        Map<String, DatoItem> segunda = new LinkedHashMap<>();
        segunda.put("nombre", new DatoItem(null, false));
        LeerImagenItem item = new LeerImagenItem(List.of(primera, segunda), List.of(), List.of());

        LecturaImagenResponse response = mapper.toResponse(item, pedido);

        assertThat(response.descripcion()).isEmpty();
        assertThat(response.camposNuevos()).isEmpty();
        assertThat(response.filas().get(0)).containsEntry("nombre", "Ana").containsEntry("cant", 3.0)
                .containsEntry("monto", 1500.5).containsEntry("dia", "2026-10-19").containsEntry("pago", true)
                .containsEntry("cliente", "");
        assertThat(response.filas().get(1)).containsEntry("nombre", "").containsEntry("cant", "")
                .containsEntry("monto", "").containsEntry("dia", "").containsEntry("pago", false)
                .containsEntry("cliente", "");
    }

    @Test
    @DisplayName("Un número mal formado queda en blanco en vez de romper la respuesta")
    void toResponseShouldLeaveMalformedNumbersBlank() {
        PedidoLecturaRequest pedido = pedido(false, campo("cant", "Cantidad", "number"));
        LeerImagenItem item = new LeerImagenItem(
                List.of(Map.of("cant", new DatoItem("doce", false))), List.of(), List.of());

        LecturaImagenResponse response = mapper.toResponse(item, pedido);

        assertThat(response.filas().getFirst()).containsEntry("cant", "");
    }

    @Test
    @DisplayName("Marca como duda el dato dudoso, con el número de fila desde 0")
    void toResponseShouldFlagDoubtfulValues() {
        PedidoLecturaRequest pedido = pedido(false,
                campo("nombre", "Nombre", "text"), campo("cant", "Cantidad", "number"));
        Map<String, DatoItem> primera = Map.of("nombre", new DatoItem("Ana", false), "cant", new DatoItem("3", false));
        Map<String, DatoItem> segunda = Map.of("nombre", new DatoItem("Beto", true), "cant", new DatoItem("4", false));

        LecturaImagenResponse response = mapper.toResponse(
                new LeerImagenItem(List.of(primera, segunda), List.of(), List.of()), pedido);

        assertThat(response.dudas()).hasSize(1);
        assertThat(response.dudas().getFirst().fila()).isEqualTo(1);
        assertThat(response.dudas().getFirst().campo()).isEqualTo("nombre");
        assertThat(response.dudas().getFirst().motivo()).isEqualTo(LecturaPresentationMapper.MOTIVO_DUDA);
    }

    @Test
    @DisplayName("Las columnas nuevas llegan con su valor en cada fila, convertido según su tipo")
    void toResponseShouldIncludeNewColumnsWithTheirValues() {
        PedidoLecturaRequest pedido = pedido(true, campo("nombre", "Nombre", "text"));
        CampoItem edad = new CampoItem("edad", "Edad", "NUMERO", List.of());
        CampoItem pago = new CampoItem("pago", "Pagó", "SI_NO", List.of());
        Map<String, DatoItem> fila = Map.of("nombre", new DatoItem("Ana", false), "edad", new DatoItem("30", false),
                "pago", new DatoItem("false", false));

        LecturaImagenResponse response = mapper.toResponse(
                new LeerImagenItem(List.of(fila), List.of(edad, pago), List.of()), pedido);

        assertThat(response.camposNuevos()).extracting(CampoResponse::key, CampoResponse::label, CampoResponse::type)
                .containsExactly(tuple("edad", "Edad", "number"), tuple("pago", "Pagó", "checkbox"));
        assertThat(response.camposNuevos()).allMatch(campo -> !campo.required() && campo.options() == null);
        assertThat(response.filas().getFirst()).containsEntry("edad", 30.0).containsEntry("pago", false);
    }

    @ParameterizedTest
    @CsvSource({"TEXTO,text", "NUMERO,number", "FECHA,date", "OPCION,select", "SI_NO,checkbox", "RELACION,relation",
            "FORMULA,text", "TEXTO_LARGO,textarea", "MONEDA,currency", "EMAIL,email", "TELEFONO,tel", "HORA,time"})
    @DisplayName("Traduce el tipo de la columna nueva al de la pantalla y sólo las opciones llevan lista")
    void toResponseShouldTranslateNewColumnTypes(String tipo, String esperado) {
        CampoItem nueva = new CampoItem("x", "X", tipo, List.of("A", "B"));

        LecturaImagenResponse response = mapper.toResponse(
                new LeerImagenItem(List.of(), List.of(nueva), List.of()), pedido(true, campo("a", "A", "text")));

        assertThat(response.camposNuevos().getFirst().type()).isEqualTo(esperado);
        assertThat(response.camposNuevos().getFirst().options())
                .isEqualTo("OPCION".equals(tipo) ? List.of("A", "B") : null);
    }

    // --- arrange ---
    private PedidoLecturaRequest pedido(boolean destinoExistente, CampoPedido... campos) {
        return new PedidoLecturaRequest("Ventas", List.of(campos), destinoExistente);
    }

    private CampoPedido campo(String key, String label, String type, String... opciones) {
        return new CampoPedido(key, label, type, opciones.length == 0 ? null : List.of(opciones));
    }
}
