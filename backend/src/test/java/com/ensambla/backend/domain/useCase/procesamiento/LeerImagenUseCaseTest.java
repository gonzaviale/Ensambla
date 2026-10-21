package com.ensambla.backend.domain.useCase.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.LeerImagenItem;
import com.ensambla.backend.domain.dto.procesamiento.LeerImagenRequest;
import com.ensambla.backend.domain.dto.procesamiento.LeerImagenRequest.CampoItem;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.mapper.procesamiento.LeerImagenMapper;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.lectura.ImagenCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen.CampoDeImagen;
import com.ensambla.backend.domain.model.procesamiento.LecturaImagen;
import com.ensambla.backend.domain.service.procesamiento.LecturaImagenService;
import com.ensambla.backend.support.ImagenTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LeerImagenUseCaseTest {

    private final LecturaImagenService lecturaImagenService = mock(LecturaImagenService.class);
    private final LeerImagenMapper leerImagenMapper = mock(LeerImagenMapper.class);
    private final LeerImagenUseCase useCase = new LeerImagenUseCase(lecturaImagenService, leerImagenMapper);

    private final LeerImagenRequest request = new LeerImagenRequest(ImagenTestData.jpeg(), "Ventas",
            List.of(new CampoItem("importe", "Monto", "NUMERO", null)), true);

    @Test
    @DisplayName("Lee la imagen contra el bloque pedido y devuelve lo leído")
    void executeShouldReadTheImageAgainstTheRequestedBlock() {
        ImagenCuaderno imagen = ImagenCuaderno.de(request.contenido());
        BloqueDeImagen bloque = bloque();
        LecturaImagen lectura = new LecturaImagen(List.of(), List.of(), List.of());
        LeerImagenItem esperado = new LeerImagenItem(List.of(), List.of(), List.of());
        givenMapeo(imagen, bloque);
        when(lecturaImagenService.leer(imagen, bloque, true)).thenReturn(lectura);
        when(leerImagenMapper.toResponse(lectura)).thenReturn(esperado);

        LeerImagenItem item = useCase.execute(request);

        assertThat(item).isSameAs(esperado);
    }

    @Test
    @DisplayName("Con una imagen inválida no llega a leer")
    void executeShouldNotReadAnInvalidImage() {
        when(leerImagenMapper.toImagen(request)).thenThrow(new BusinessRuleException("La imagen está vacía"));

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(lecturaImagenService);
    }

    @Test
    @DisplayName("Con un bloque sin datos no llega a leer")
    void executeShouldNotReadABlockWithoutFields() {
        when(leerImagenMapper.toImagen(request)).thenReturn(ImagenCuaderno.de(request.contenido()));
        when(leerImagenMapper.toBloque(request)).thenThrow(new BusinessRuleException("El bloque no tiene datos"));

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(BusinessRuleException.class);

        verify(lecturaImagenService, never()).leer(any(), any(), anyBoolean());
    }

    // --- arrange ---
    private void givenMapeo(ImagenCuaderno imagen, BloqueDeImagen bloque) {
        when(leerImagenMapper.toImagen(request)).thenReturn(imagen);
        when(leerImagenMapper.toBloque(request)).thenReturn(bloque);
    }

    private BloqueDeImagen bloque() {
        return BloqueDeImagen.de("Ventas", List.of(
                CampoDeImagen.nuevo("importe", "Monto", TipoCampo.NUMERO, List.of())));
    }
}
