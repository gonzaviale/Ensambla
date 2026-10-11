package com.ensambla.backend.domain.useCase.lote;

import com.ensambla.backend.domain.dto.lote.FotoArchivoResponse;
import com.ensambla.backend.domain.dto.lote.ObtenerFotoLoteRequest;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.lote.FotoArchivoMapper;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerFotoLoteUseCaseTest {

    @Mock
    private NegocioService negocioService;

    @Mock
    private LoteSubidaService loteSubidaService;

    private ObtenerFotoLoteUseCase obtenerFotoLoteUseCase;

    @BeforeEach
    void setUp() {
        obtenerFotoLoteUseCase = new ObtenerFotoLoteUseCase(negocioService, loteSubidaService, new FotoArchivoMapper());
    }

    @Test
    @DisplayName("Devuelve la foto del lote del negocio de la sesión")
    void executeShouldReturnFotoOfNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(loteSubidaService.leerFotoDeLote(NEGOCIO_ID, LOTE_ID, FOTO_ID))
                .thenReturn(new ArchivoFoto(JPEG, "image/jpeg"));

        FotoArchivoResponse result = obtenerFotoLoteUseCase.execute(request());

        assertThat(result.contenido()).isEqualTo(JPEG);
        assertThat(result.tipoContenido()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Propaga que la foto no existe sin convertirlo")
    void executeShouldPropagateNotFound() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(loteSubidaService.leerFotoDeLote(NEGOCIO_ID, LOTE_ID, FOTO_ID))
                .thenThrow(new ResourceNotFoundException("Foto no encontrada"));
        ObtenerFotoLoteRequest request = request();

        assertThatThrownBy(() -> obtenerFotoLoteUseCase.execute(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- helpers ---
    private ObtenerFotoLoteRequest request() {
        return new ObtenerFotoLoteRequest(USUARIO_ID, LOTE_ID, FOTO_ID);
    }
}
