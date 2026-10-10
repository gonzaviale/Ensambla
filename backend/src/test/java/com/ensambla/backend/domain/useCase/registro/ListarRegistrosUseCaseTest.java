package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.dto.registro.ListarRegistrosRequest;
import com.ensambla.backend.domain.dto.registro.ListarRegistrosResponse;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.registro.ListarRegistrosMapper;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.enums.DireccionOrden;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarRegistrosUseCaseTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID CAMPO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    @Mock
    private NegocioService negocioService;

    @Mock
    private BloqueService bloqueService;

    @Mock
    private CampoService campoService;

    @Mock
    private RegistroService registroService;

    private ListarRegistrosUseCase listarRegistrosUseCase;

    @BeforeEach
    void setUp() {
        listarRegistrosUseCase = new ListarRegistrosUseCase(
                negocioService, bloqueService, campoService, registroService, new ListarRegistrosMapper());
    }

    @Test
    @DisplayName("Arma la consulta con el negocio de la sesión y devuelve la página mapeada")
    void executeShouldQueryWithSessionNegocioAndReturnMappedPage() {
        givenBloqueOfNegocioWithCampo();
        when(registroService.listar(any(ConsultaRegistros.class)))
                .thenReturn(new Pagina<>(List.of(Registro.builder().id(REGISTRO_ID).build()), 1, 5, 6));

        ListarRegistrosResponse response = listarRegistrosUseCase.execute(new ListarRegistrosRequest(
                USUARIO_ID, BLOQUE_ID, " harina ", CAMPO_ID.toString(), "asc", 1, 5));

        ArgumentCaptor<ConsultaRegistros> consulta = ArgumentCaptor.forClass(ConsultaRegistros.class);
        verify(registroService).listar(consulta.capture());
        assertThat(consulta.getValue().negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(consulta.getValue().bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(consulta.getValue().texto()).isEqualTo("harina");
        assertThat(consulta.getValue().campoOrdenId()).isEqualTo(CAMPO_ID);
        assertThat(consulta.getValue().direccion()).isEqualTo(DireccionOrden.ASC);
        assertThat(consulta.getValue().pagina()).isEqualTo(1);
        assertThat(consulta.getValue().tamanio()).isEqualTo(5);
        assertThat(response.contenido()).extracting("id").containsExactly(REGISTRO_ID);
        assertThat(response.totalElementos()).isEqualTo(6);
        assertThat(response.totalPaginas()).isEqualTo(2);
    }

    @Test
    @DisplayName("No consulta registros cuando el bloque no es del negocio")
    void executeShouldPropagateNotFoundWhenBloqueIsNotOfTheNegocio() {
        when(negocioService.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenThrow(new ResourceNotFoundException("El bloque no existe"));
        ListarRegistrosRequest request = new ListarRegistrosRequest(USUARIO_ID, BLOQUE_ID, null, null, null, null, null);

        assertThatThrownBy(() -> listarRegistrosUseCase.execute(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(campoService, registroService);
    }

    // --- arrange ---
    private void givenBloqueOfNegocioWithCampo() {
        when(negocioService.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(List.of(Campo.builder().id(CAMPO_ID).build()));
    }
}
