package com.ensambla.backend.domain.useCase.campo.listarCampos;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.campo.CampoItem;
import com.ensambla.backend.infrastructure.config.useCase.CampoUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.ensambla.backend.support.CamposDePrueba.unCampoDeTexto;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarCamposTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final ListarCamposRequest PEDIDO = new ListarCamposRequest(USUARIO_ID, BLOQUE_ID);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final ListarCampos listarCampos = new CampoUseCaseConfig().listarCampos(
            negocioRepository,
            bloqueRepository,
            campoRepository);

    private List<CampoItem> campos;
    private Throwable rechazo;

    @Test
    @DisplayName("Se listan los campos del bloque del negocio de la sesión en el orden del repositorio")
    void seListanLosCamposDelBloqueDelNegocioDeLaSesionEnElOrdenDelRepositorio() {
        dadoUnBloqueConLosCampos("Producto", "Precio");

        cuandoSeListanLosCampos();

        entoncesSeListanLosCampos("Producto", "Precio");
    }

    @Test
    @DisplayName("Si el bloque es ajeno se informa que no existe, sin consultar los campos")
    void siElBloqueEsAjenoSePropagaElNoEncontradoSinConsultarLosCampos() {
        dadoUnBloqueAjeno();

        cuandoSeIntentaListarLosCampos();

        entoncesSeRechazaPorqueElBloqueNoExiste();
        entoncesNoSeConsultanLosCampos();
    }

    private void dadoUnBloqueConLosCampos(String... nombres) {
        dadoUnNegocio();
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(camposLlamados(nombres));
    }

    private void dadoUnBloqueAjeno() {
        dadoUnNegocio();
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El bloque no existe"));
    }

    private void dadoUnNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private List<Campo> camposLlamados(String... nombres) {
        return Arrays.stream(nombres).map(nombre -> unCampoDeTexto(nombre)).toList();
    }

    private void cuandoSeListanLosCampos() {
        campos = listarCampos.execute(PEDIDO);
    }

    private void cuandoSeIntentaListarLosCampos() {
        rechazo = catchThrowable(() -> listarCampos.execute(PEDIDO));
    }

    private void entoncesSeListanLosCampos(String... nombres) {
        assertThat(campos).extracting(CampoItem::nombre).containsExactly(nombres);
    }

    private void entoncesSeRechazaPorqueElBloqueNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeConsultanLosCampos() {
        verifyNoInteractions(campoRepository);
    }
}
