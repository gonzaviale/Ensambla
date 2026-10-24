package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.dto.registro.ListarReferenciasRequest;
import com.ensambla.backend.domain.dto.registro.ReferenciaItem;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.registro.ListarReferenciasMapper;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarReferenciasUseCaseTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID NOMBRE_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final List<Campo> CAMPOS = List.of(
            Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).posicion((short) 0).build());

    @Mock
    private NegocioService negocioService;

    @Mock
    private BloqueService bloqueService;

    @Mock
    private CampoService campoService;

    @Mock
    private RegistroDatosService registroDatosService;

    private ListarReferenciasUseCase listarReferenciasUseCase;

    @BeforeEach
    void setUp() {
        listarReferenciasUseCase = new ListarReferenciasUseCase(
                negocioService, bloqueService, campoService, registroDatosService, new ListarReferenciasMapper());
    }

    @Test
    @DisplayName("Devuelve cada fila encontrada con su etiqueta")
    void executeShouldReturnReferenciasWithEtiqueta() {
        givenBloqueOfNegocio();
        givenReferenciables("ana", List.of());

        List<ReferenciaItem> result = execute("ana", List.of());

        assertThat(result).containsExactly(new ReferenciaItem(REGISTRO_ID, "Ana"));
    }

    @Test
    @DisplayName("Pasa al servicio los ids pedidos con el negocio de la sesión y los campos del bloque")
    void executeShouldPassIdsToService() {
        givenBloqueOfNegocio();
        givenReferenciables(null, List.of(REGISTRO_ID));

        execute(null, List.of(REGISTRO_ID));

        verify(registroDatosService).buscarReferenciables(NEGOCIO_ID, BLOQUE_ID, CAMPOS, null, List.of(REGISTRO_ID));
    }

    @Test
    @DisplayName("No busca filas cuando el bloque no es del negocio")
    void executeShouldPropagateNotFoundWhenBloqueIsNotOfTheNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El bloque no existe"));

        assertThatThrownBy(() -> execute("ana", List.of()))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(campoService, registroDatosService);
    }

    // --- arrange ---
    private void givenBloqueOfNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(CAMPOS);
    }

    private void givenReferenciables(String texto, List<UUID> ids) {
        Registro registro = Registro.builder().id(REGISTRO_ID).valores(Map.of(NOMBRE_ID.toString(), "Ana")).build();
        when(registroDatosService.buscarReferenciables(NEGOCIO_ID, BLOQUE_ID, CAMPOS, texto, ids))
                .thenReturn(List.of(registro));
    }

    // --- act ---
    private List<ReferenciaItem> execute(String texto, List<UUID> ids) {
        return listarReferenciasUseCase.execute(new ListarReferenciasRequest(USUARIO_ID, BLOQUE_ID, texto, ids));
    }
}
