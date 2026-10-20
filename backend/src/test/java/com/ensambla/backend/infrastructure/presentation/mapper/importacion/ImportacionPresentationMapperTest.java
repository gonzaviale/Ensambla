package com.ensambla.backend.infrastructure.presentation.mapper.importacion;

import com.ensambla.backend.domain.dto.lectura.LeerImagenItem;
import com.ensambla.backend.domain.dto.lectura.LeerImagenRequest;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.infrastructure.ia.IaProperties;
import com.ensambla.backend.infrastructure.presentation.dto.importacion.LecturaImagenResponse;
import com.ensambla.backend.infrastructure.presentation.dto.importacion.LecturaImagenResponse.CampoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.importacion.PedidoLecturaRequest;
import com.ensambla.backend.infrastructure.presentation.dto.importacion.PedidoLecturaRequest.CampoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportacionPresentationMapperTest {

    private final ImportacionPresentationMapper mapper = new ImportacionPresentationMapper(
            new IaProperties("simulado", "", "", Duration.ofSeconds(60), 10, 0.8));

    @Test
    @DisplayName("Arma el pedido de lectura con los tipos del dominio y sin los campos que apuntan a otro bloque")
    void toRequestShouldTranslateFieldTypesAndSkipRelations() {
        PedidoLecturaRequest pedido = pedido(true,
                campo("nombre", "Nombre", "text"), campo("nota", "Nota", "textarea"), campo("mail", "Mail", "email"),
                campo("cel", "Celular", "tel"), campo("hora", "Hora", "time"), campo("cant", "Cantidad", "number"),
                campo("monto", "Monto", "currency"), campo("dia", "Día", "date"), campo("estado", "Estado", "select", "A", "B"),
                campo("pago", "Pagó", "checkbox"), campo("cliente", "Cliente", "relation"));

        LeerImagenRequest request = mapper.toRequest(
                new MockMultipartFile("imagen", "foto.jpg", "image/jpeg", new byte[]{1, 2}), pedido);

        assertThat(request.contenido()).containsExactly(1, 2);
        assertThat(request.tipoContenido()).isEqualTo("image/jpeg");
        assertThat(request.nombreBloque()).isEqualTo("Ventas");
        assertThat(request.destinoExistente()).isTrue();
        assertThat(request.campos()).extracting("clave", "tipo").containsExactly(
                tuple("nombre", TipoCampo.TEXTO),
                tuple("nota", TipoCampo.TEXTO),
                tuple("mail", TipoCampo.TEXTO),
                tuple("cel", TipoCampo.TEXTO),
                tuple("hora", TipoCampo.TEXTO),
                tuple("cant", TipoCampo.NUMERO),
                tuple("monto", TipoCampo.NUMERO),
                tuple("dia", TipoCampo.FECHA),
                tuple("estado", TipoCampo.OPCION),
                tuple("pago", TipoCampo.SI_NO));
        assertThat(request.campos().get(8).opciones()).containsExactly("A", "B");
    }

    @Test
    @DisplayName("No se puede leer la imagen recibida")
    void toRequestShouldFailWhenTheImageCannotBeRead() throws IOException {
        MultipartFile imagen = mock(MultipartFile.class);
        when(imagen.getBytes()).thenThrow(new IOException("disco"));

        assertThatThrownBy(() -> mapper.toRequest(imagen, pedido(false, campo("a", "A", "text"))))
                .isInstanceOf(UncheckedIOException.class).hasMessageContaining("No se pudo leer la imagen");
    }

    @Test
    @DisplayName("Convierte los valores leídos a lo que espera cada campo y completa los que faltan")
    void toResponseShouldConvertValuesAndFillMissingOnes() {
        PedidoLecturaRequest pedido = pedido(true,
                campo("nombre", "Nombre", "text"), campo("cant", "Cantidad", "number"),
                campo("monto", "Monto", "currency"), campo("dia", "Día", "date"),
                campo("pago", "Pagó", "checkbox"), campo("cliente", "Cliente", "relation"));
        Map<String, DatoLeido> primera = new LinkedHashMap<>();
        primera.put("nombre", new DatoLeido("Ana", 0.9));
        primera.put("cant", new DatoLeido("3", 0.9));
        primera.put("monto", new DatoLeido("1500.50", 0.9));
        primera.put("dia", new DatoLeido("2026-10-19", 0.9));
        primera.put("pago", new DatoLeido("true", 0.9));
        Map<String, DatoLeido> segunda = new LinkedHashMap<>();
        segunda.put("nombre", new DatoLeido(null, 0.9));
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
    @DisplayName("Marca como duda el dato con poca confianza, con el número de fila desde 0")
    void toResponseShouldFlagLowConfidenceValues() {
        PedidoLecturaRequest pedido = pedido(false, campo("nombre", "Nombre", "text"), campo("cant", "Cantidad", "number"));
        Map<String, DatoLeido> primera = Map.of("nombre", new DatoLeido("Ana", 0.95), "cant", new DatoLeido("3", 0.8));
        Map<String, DatoLeido> segunda = Map.of("nombre", new DatoLeido("Beto", 0.4), "cant", new DatoLeido("4", 0.9));

        LecturaImagenResponse response = mapper.toResponse(
                new LeerImagenItem(List.of(primera, segunda), List.of(), List.of()), pedido);

        assertThat(response.dudas()).hasSize(1);
        assertThat(response.dudas().getFirst().fila()).isEqualTo(1);
        assertThat(response.dudas().getFirst().campo()).isEqualTo("nombre");
        assertThat(response.dudas().getFirst().motivo()).isEqualTo(ImportacionPresentationMapper.MOTIVO_DUDA);
    }

    @Test
    @DisplayName("Las columnas nuevas llegan con su valor en cada fila, convertido según su tipo")
    void toResponseShouldIncludeNewColumnsWithTheirValues() {
        PedidoLecturaRequest pedido = pedido(true, campo("nombre", "Nombre", "text"));
        CampoPropuesto nueva = new CampoPropuesto("edad", "Edad", TipoCampo.NUMERO, List.of());
        CampoPropuesto pago = new CampoPropuesto("pago", "Pagó", TipoCampo.SI_NO, List.of());
        Map<String, DatoLeido> fila = Map.of("nombre", new DatoLeido("Ana", 0.9), "edad", new DatoLeido("30", 0.9),
                "pago", new DatoLeido("false", 0.9));

        LecturaImagenResponse response = mapper.toResponse(
                new LeerImagenItem(List.of(fila), List.of(nueva, pago), List.of()), pedido);

        assertThat(response.camposNuevos()).extracting(CampoResponse::key, CampoResponse::label, CampoResponse::type)
                .containsExactly(tuple("edad", "Edad", "number"),
                        tuple("pago", "Pagó", "checkbox"));
        assertThat(response.camposNuevos()).allMatch(campo -> !campo.required() && campo.options() == null);
        assertThat(response.filas().getFirst()).containsEntry("edad", 30.0).containsEntry("pago", false);
    }

    @ParameterizedTest
    @CsvSource({"TEXTO,text", "NUMERO,number", "FECHA,date", "OPCION,select", "SI_NO,checkbox", "RELACION,relation",
            "FORMULA,text",
            "TEXTO_LARGO,textarea", "MONEDA,currency", "EMAIL,email", "TELEFONO,tel", "HORA,time"})
    @DisplayName("Traduce el tipo de la columna nueva al de la pantalla y sólo las opciones llevan lista")
    void toResponseShouldTranslateNewColumnTypes(TipoCampo tipo, String esperado) {
        CampoPropuesto nueva = new CampoPropuesto("x", "X", tipo, List.of("A", "B"));

        LecturaImagenResponse response = mapper.toResponse(
                new LeerImagenItem(List.of(), List.of(nueva), List.of()), pedido(true, campo("a", "A", "text")));

        assertThat(response.camposNuevos().getFirst().type()).isEqualTo(esperado);
        assertThat(response.camposNuevos().getFirst().options())
                .isEqualTo(tipo == TipoCampo.OPCION ? List.of("A", "B") : null);
    }

    private PedidoLecturaRequest pedido(boolean destinoExistente, CampoPedido... campos) {
        return new PedidoLecturaRequest("Ventas", List.of(campos), destinoExistente);
    }

    private CampoPedido campo(String key, String label, String type, String... opciones) {
        return new CampoPedido(key, label, type, opciones.length == 0 ? null : List.of(opciones));
    }
}
