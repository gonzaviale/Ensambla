package com.ensambla.backend.domain.useCase.registro.listarReferencias;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarReferenciasTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final List<Campo> CAMPOS = List.of(
            Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).posicion((short) 0).build());

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private BloqueRepository bloqueRepository;

    @Mock
    private CampoRepository campoRepository;

    @Mock
    private RegistroDatosService registroDatosService;

    private ListarReferencias listarReferencias;
    private List<ReferenciaItem> referencias;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        listarReferencias = new ListarReferencias(
                negocioRepository, bloqueRepository, campoRepository, registroDatosService);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Devuelve cada fila encontrada con su etiqueta")
    void devuelveCadaFilaEncontradaConSuEtiqueta() {
        dadoUnBloqueDelNegocio();
        dadoQueElServicioEncuentraAAna("ana", List.of());

        cuandoSeListanLasReferencias("ana", List.of());

        entoncesSeDevuelveLaReferenciaDeAna();
    }

    @Test
    @DisplayName("Pasa al servicio los ids pedidos con el negocio de la sesión y los campos del bloque")
    void pasaAlServicioLosIdsPedidosConElNegocioDeLaSesionYLosCamposDelBloque() {
        dadoUnBloqueDelNegocio();
        dadoQueElServicioEncuentraAAna(null, List.of(REGISTRO_ID));

        cuandoSeListanLasReferencias(null, List.of(REGISTRO_ID));

        entoncesElServicioRecibioLosIds(List.of(REGISTRO_ID));
    }

    @Test
    @DisplayName("No busca filas cuando el bloque no es del negocio")
    void noBuscaFilasCuandoElBloqueNoEsDelNegocio() {
        dadoQueElBloqueNoEsDelNegocio();

        cuandoSeIntentaListarLasReferencias("ana", List.of());

        entoncesSeRechazaPorBloqueInexistenteSinBuscarFilas();
    }

    // --- dado ---
    private void dadoUnBloqueDelNegocio() {
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(CAMPOS);
    }

    private void dadoQueElBloqueNoEsDelNegocio() {
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El bloque no existe"));
    }

    private void dadoQueElServicioEncuentraAAna(String texto, List<RegistroId> ids) {
        Registro ana = Registro.builder().id(REGISTRO_ID).valores(Map.of(NOMBRE_ID.toString(), "Ana")).build();
        when(registroDatosService.buscarReferenciables(NEGOCIO_ID, BLOQUE_ID, CAMPOS, texto, ids))
                .thenReturn(List.of(ana));
    }

    // --- cuando ---
    private void cuandoSeListanLasReferencias(String texto, List<RegistroId> ids) {
        referencias = listarReferencias.execute(new ListarReferenciasRequest(USUARIO_ID, BLOQUE_ID, texto, ids));
    }

    private void cuandoSeIntentaListarLasReferencias(String texto, List<RegistroId> ids) {
        rechazo = catchThrowable(() -> cuandoSeListanLasReferencias(texto, ids));
    }

    // --- entonces ---
    private void entoncesSeDevuelveLaReferenciaDeAna() {
        assertThat(referencias).containsExactly(new ReferenciaItem(REGISTRO_ID, "Ana"));
    }

    private void entoncesElServicioRecibioLosIds(List<RegistroId> ids) {
        verify(registroDatosService).buscarReferenciables(NEGOCIO_ID, BLOQUE_ID, CAMPOS, null, ids);
    }

    private void entoncesSeRechazaPorBloqueInexistenteSinBuscarFilas() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(campoRepository, registroDatosService);
    }
}
