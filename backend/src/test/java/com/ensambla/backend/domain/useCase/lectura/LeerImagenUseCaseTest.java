package com.ensambla.backend.domain.useCase.lectura;

import com.ensambla.backend.domain.dto.lectura.LeerImagenItem;
import com.ensambla.backend.domain.dto.lectura.LeerImagenRequest;
import com.ensambla.backend.domain.dto.lectura.LeerImagenRequest.CampoImagen;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.enums.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.service.procesamiento.PropuestaLecturaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LeerImagenUseCaseTest {

    private static final byte[] FOTO = {1, 2, 3};

    private final LectorCuadernoPort lectorCuadernoPort = mock(LectorCuadernoPort.class);
    private final LeerImagenUseCase useCase = new LeerImagenUseCase(lectorCuadernoPort, new PropuestaLecturaService());

    @Test
    @DisplayName("Los datos leídos van por la clave del campo del bloque y la columna que falta se propone como nueva")
    void executeShouldKeyValuesByBlockFieldAndProposeNewColumn() {
        givenLectura(
                List.of(columna("monto", "Monto", TipoCampo.NUMERO), columna("cliente", "Cliente", TipoCampo.TEXTO),
                        columna("telefono", "Teléfono", TipoCampo.TEXTO)),
                fila(dato("monto", "1500", 0.95), dato("cliente", "Ana", 0.9), dato("telefono", "1155", 0.5)));

        LeerImagenItem item = useCase.execute(request(true,
                campo("importe", "Monto", TipoCampo.NUMERO), campo("cliente", "Cliente", TipoCampo.TEXTO)));

        assertThat(item.camposNuevos()).extracting(CampoPropuesto::clave, CampoPropuesto::nombre, CampoPropuesto::tipo)
                .containsExactly(tuple("telefono", "Teléfono", TipoCampo.TEXTO));
        assertThat(item.filas()).hasSize(1);
        Map<String, DatoLeido> datos = item.filas().getFirst();
        assertThat(datos.keySet()).containsExactlyInAnyOrder("importe", "cliente", "telefono");
        assertThat(datos.get("importe").valor()).isEqualTo("1500");
        assertThat(datos.get("telefono").confianza()).isEqualTo(0.5);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("La IA ve sólo el bloque elegido, una página y el bloque como destino")
    void executeShouldAskTheReaderForOnlyTheChosenBlock() {
        givenLectura(List.of(columna("monto", "Monto", TipoCampo.NUMERO)), fila(dato("monto", "1", 0.9)));

        useCase.execute(request(true, campo("importe", "Monto", TipoCampo.NUMERO)));

        ArgumentCaptor<List<PaginaCuaderno>> paginas = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<BloqueConocido>> bloques = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<UUID> destino = ArgumentCaptor.forClass(UUID.class);
        verify(lectorCuadernoPort).leer(paginas.capture(), bloques.capture(), destino.capture());
        assertThat(paginas.getValue()).hasSize(1);
        assertThat(paginas.getValue().getFirst().contenido()).isEqualTo(FOTO);
        assertThat(paginas.getValue().getFirst().tipoContenido()).isEqualTo("image/jpeg");
        assertThat(bloques.getValue()).hasSize(1);
        assertThat(bloques.getValue().getFirst().id()).isEqualTo(destino.getValue());
        assertThat(bloques.getValue().getFirst().nombre()).isEqualTo("Ventas");
        assertThat(bloques.getValue().getFirst().campos()).extracting("nombre").containsExactly("Monto");
    }

    @Test
    @DisplayName("Si el bloque todavía no existe no se proponen columnas nuevas ni se devuelven sus valores")
    void executeShouldIgnoreNewColumnsWhenTheBlockDoesNotExist() {
        givenLectura(List.of(columna("monto", "Monto", TipoCampo.NUMERO), columna("telefono", "Teléfono", TipoCampo.TEXTO)),
                fila(dato("monto", "1500", 0.9), dato("telefono", "1155", 0.9)));

        LeerImagenItem item = useCase.execute(request(false, campo("importe", "Monto", TipoCampo.NUMERO)));

        assertThat(item.camposNuevos()).isEmpty();
        assertThat(item.filas().getFirst().keySet()).containsExactly("importe");
    }

    @Test
    @DisplayName("La clave de una columna nueva no pisa la de un campo del bloque ni la de otra columna nueva")
    void executeShouldGiveNewColumnsUniqueKeys() {
        givenLectura(List.of(columna("a", "Producto", TipoCampo.TEXTO), columna("b", "Marca", TipoCampo.TEXTO),
                        columna("c", "¡Marca!", TipoCampo.TEXTO), columna("d", "¿?", TipoCampo.TEXTO)),
                fila(dato("a", "Yerba", 0.9), dato("b", "Taragüí", 0.9), dato("c", "Otra", 0.9), dato("d", "x", 0.9)));

        LeerImagenItem item = useCase.execute(request(true, campo("marca", "Producto", TipoCampo.TEXTO)));

        assertThat(item.camposNuevos()).extracting(CampoPropuesto::clave).containsExactly("marca_2", "marca_3", "columna");
        assertThat(item.filas().getFirst().keySet()).containsExactlyInAnyOrder("marca", "marca_2", "marca_3", "columna");
    }

    @Test
    @DisplayName("Devuelve los avisos de la lectura")
    void executeShouldReturnReadingWarnings() {
        LecturaCuaderno lectura = new LecturaCuaderno(TipoRegistroCuaderno.OTRO,
                new BloquePropuesto("Hoja", null, List.of(columna("monto", "Monto", TipoCampo.NUMERO))),
                List.of(fila(dato("monto", "1", 0.9))), List.of("Página torcida"), consumo());
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura);

        LeerImagenItem item = useCase.execute(request(true, campo("importe", "Monto", TipoCampo.NUMERO)));

        assertThat(item.advertencias()).containsExactly("Página torcida");
    }

    @Test
    @DisplayName("Rechaza la foto cuando ninguna columna leída es del bloque")
    void executeShouldRejectPhotoThatIsNotFromTheBlock() {
        givenLectura(List.of(columna("color", "Color", TipoCampo.TEXTO)), fila(dato("color", "Rojo", 0.9)));

        assertThatThrownBy(() -> useCase.execute(request(true, campo("importe", "Monto", TipoCampo.NUMERO))))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("compatible");
    }

    @Test
    @DisplayName("Propaga el error de la IA")
    void executeShouldPropagateReaderFailure() {
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Falla de la IA"));

        assertThatThrownBy(() -> useCase.execute(request(true, campo("importe", "Monto", TipoCampo.NUMERO))))
                .isInstanceOf(LecturaCuadernoException.class).hasMessage("Falla de la IA");
    }

    @Test
    @DisplayName("No llama a la IA con una imagen vacía, enorme, de otro tipo o sin datos que leer")
    void executeShouldRejectInvalidRequestsWithoutCallingTheReader() {
        CampoImagen monto = campo("importe", "Monto", TipoCampo.NUMERO);

        assertThatThrownBy(() -> useCase.execute(new LeerImagenRequest(null, "image/jpeg", "Ventas", List.of(monto), true)))
                .isInstanceOf(BusinessRuleException.class).hasMessage("La imagen está vacía");
        assertThatThrownBy(() -> useCase.execute(new LeerImagenRequest(new byte[0], "image/jpeg", "Ventas", List.of(monto), true)))
                .isInstanceOf(BusinessRuleException.class).hasMessage("La imagen está vacía");
        assertThatThrownBy(() -> useCase.execute(new LeerImagenRequest(
                new byte[(int) LeerImagenUseCase.MAXIMO_BYTES + 1], "image/jpeg", "Ventas", List.of(monto), true)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("5 MB");
        assertThatThrownBy(() -> useCase.execute(new LeerImagenRequest(FOTO, "application/pdf", "Ventas", List.of(monto), true)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("JPG, PNG o WEBP");
        assertThatThrownBy(() -> useCase.execute(new LeerImagenRequest(FOTO, null, "Ventas", List.of(monto), true)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("JPG, PNG o WEBP");
        assertThatThrownBy(() -> useCase.execute(new LeerImagenRequest(FOTO, "image/png", "Ventas", List.of(), true)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("no tiene datos");
        verifyNoInteractions(lectorCuadernoPort);
    }

    @Test
    @DisplayName("Un campo sin opciones informadas no tiene opciones")
    void campoImagenShouldDefaultToNoOptions() {
        assertThat(new CampoImagen("a", "A", TipoCampo.TEXTO, null).opciones()).isEmpty();
    }

    private void givenLectura(List<CampoPropuesto> campos, FilaLeida... filas) {
        LecturaCuaderno lectura = new LecturaCuaderno(TipoRegistroCuaderno.OTRO,
                new BloquePropuesto("Hoja", null, campos), List.of(filas), List.of(), consumo());
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura);
    }

    private ConsumoIa consumo() {
        return new ConsumoIa("simulado", "demo", "v3", 1, 0, 0, 1);
    }

    private LeerImagenRequest request(boolean destinoExistente, CampoImagen... campos) {
        return new LeerImagenRequest(FOTO, "image/jpeg", "Ventas", List.of(campos), destinoExistente);
    }

    private CampoImagen campo(String clave, String nombre, TipoCampo tipo) {
        return new CampoImagen(clave, nombre, tipo, List.of());
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
