package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.dto.lote.FotoArchivoResponse;
import com.ensambla.backend.domain.dto.registro.ObtenerFotoOrigenRequest;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.lote.FotoArchivoMapper;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerFotoOrigenUseCaseTest {

    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");

    @Mock
    private NegocioService negocioService;

    @Mock
    private RegistroDatosService registroDatosService;

    @Mock
    private LoteSubidaService loteSubidaService;

    private ObtenerFotoOrigenUseCase obtenerFotoOrigenUseCase;

    @BeforeEach
    void setUp() {
        obtenerFotoOrigenUseCase = new ObtenerFotoOrigenUseCase(
                negocioService, registroDatosService, loteSubidaService, new FotoArchivoMapper());
    }

    @Test
    @DisplayName("Devuelve la foto de la que salió el registro del negocio")
    void executeShouldReturnFotoOfRegistro() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID))
                .thenReturn(Registro.desdeCuaderno(NEGOCIO_ID, BLOQUE_ID, Map.of(), FOTO_ID, AHORA));
        when(loteSubidaService.leerFoto(NEGOCIO_ID, FOTO_ID)).thenReturn(new ArchivoFoto(JPEG, "image/jpeg"));

        FotoArchivoResponse result = obtenerFotoOrigenUseCase.execute(request());

        assertThat(result.contenido()).isEqualTo(JPEG);
        assertThat(result.tipoContenido()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Un registro cargado a mano no tiene foto y no se lee ningún archivo")
    void executeShouldThrowWhenRegistroHasNoFoto() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID))
                .thenReturn(Registro.nuevo(NEGOCIO_ID, BLOQUE_ID, Map.of(), AHORA));
        ObtenerFotoOrigenRequest request = request();

        assertThatThrownBy(() -> obtenerFotoOrigenUseCase.execute(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(loteSubidaService);
    }

    // --- helpers ---
    private ObtenerFotoOrigenRequest request() {
        return new ObtenerFotoOrigenRequest(USUARIO_ID, REGISTRO_ID);
    }
}
