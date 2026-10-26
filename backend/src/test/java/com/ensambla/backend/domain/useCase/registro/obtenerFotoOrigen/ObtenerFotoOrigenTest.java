package com.ensambla.backend.domain.useCase.registro.obtenerFotoOrigen;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.useCase.lote.FotoArchivoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

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
class ObtenerFotoOrigenTest {

    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private RegistroRepository registroRepository;

    @Mock
    private ArchivoCuadernoPort archivoCuadernoPort;

    @Mock
    private LoteSubidaRepository loteSubidaRepository;

    private ObtenerFotoOrigen obtenerFotoOrigen;

    @BeforeEach
    void setUp() {
        obtenerFotoOrigen = new ObtenerFotoOrigen(negocioRepository, registroRepository, archivoCuadernoPort,
                loteSubidaRepository);
    }

    @Test
    @DisplayName("Devuelve la foto de la que salió el registro del negocio")
    void executeShouldReturnFotoOfRegistro() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroRepository.obtener(REGISTRO_ID, NEGOCIO_ID))
                .thenReturn(Registro.desdeCuaderno(NEGOCIO_ID, BLOQUE_ID, Map.of(), FOTO_ID, AHORA));
        FotoCuaderno foto = FotoCuaderno.builder().id(FOTO_ID).build();
        when(loteSubidaRepository.findFotoByIdAndNegocioId(FOTO_ID, NEGOCIO_ID)).thenReturn(Optional.of(foto));
        when(archivoCuadernoPort.leerFoto(NEGOCIO_ID, foto)).thenReturn(new ArchivoFoto(JPEG, "image/jpeg"));

        FotoArchivoResponse result = obtenerFotoOrigen.execute(request());

        assertThat(result.contenido()).isEqualTo(JPEG);
        assertThat(result.tipoContenido()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Un registro cargado a mano no tiene foto y no se lee ningún archivo")
    void executeShouldThrowWhenRegistroHasNoFoto() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroRepository.obtener(REGISTRO_ID, NEGOCIO_ID))
                .thenReturn(Registro.nuevo(NEGOCIO_ID, BLOQUE_ID, Map.of(), AHORA));
        ObtenerFotoOrigenRequest request = request();

        assertThatThrownBy(() -> obtenerFotoOrigen.execute(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(archivoCuadernoPort, loteSubidaRepository);
    }

    @Test
    @DisplayName("Una foto que no es del negocio responde como inexistente sin leer ningún archivo")
    void executeShouldThrowWhenFotoIsNotOfTheNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroRepository.obtener(REGISTRO_ID, NEGOCIO_ID))
                .thenReturn(Registro.desdeCuaderno(NEGOCIO_ID, BLOQUE_ID, Map.of(), FOTO_ID, AHORA));
        when(loteSubidaRepository.findFotoByIdAndNegocioId(FOTO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
        ObtenerFotoOrigenRequest request = request();

        assertThatThrownBy(() -> obtenerFotoOrigen.execute(request))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Foto no encontrada");
        verifyNoInteractions(archivoCuadernoPort);
    }

    // --- helpers ---
    private ObtenerFotoOrigenRequest request() {
        return new ObtenerFotoOrigenRequest(USUARIO_ID, REGISTRO_ID);
    }
}
