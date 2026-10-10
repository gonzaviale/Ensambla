package com.ensambla.backend.domain.service.campo;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampoServiceTest {

    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");

    @Mock
    private CampoRepository campoRepository;

    @InjectMocks
    private CampoService campoService;

    @Test
    @DisplayName("Devuelve los campos del bloque que entrega el repositorio")
    void listarDeBloqueShouldReturnCamposFromRepository() {
        List<Campo> campos = List.of(Campo.builder().nombre("Producto").build());
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(campos);

        List<Campo> result = campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID);

        assertThat(result).isSameAs(campos);
    }
}
