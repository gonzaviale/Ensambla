package com.ensambla.backend.domain.useCase.registro.listarRegistros;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.DireccionOrden;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarRegistrosTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private BloqueRepository bloqueRepository;

    @Mock
    private CampoRepository campoRepository;

    @Mock
    private RegistroRepository registroRepository;

    private ListarRegistros listarRegistros;
    private ListarRegistrosResponse listado;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        listarRegistros = new ListarRegistros(
                negocioRepository, bloqueRepository, campoRepository, registroRepository);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Arma la consulta con el negocio de la sesión y devuelve la página mapeada")
    void armaLaConsultaConElNegocioDeLaSesionYDevuelveLaPaginaMapeada() {
        dadoUnBloqueConUnCampoNumerico();
        dadoUnaPaginaConUnRegistroYTreintaDeSuma();

        cuandoSeListaConTextoOrdenYPagina();

        entoncesSeConsultoConLosFiltrosPedidos();
        entoncesSeDevuelveLaPaginaConSusTotales();
    }

    @Test
    @DisplayName("No consulta registros cuando el bloque no es del negocio")
    void noConsultaRegistrosCuandoElBloqueNoEsDelNegocio() {
        dadoQueElBloqueNoEsDelNegocio();

        cuandoSeIntentaListarSinFiltros();

        entoncesSeRechazaPorBloqueInexistenteSinConsultar();
    }

    @Test
    @DisplayName("Sin campos numéricos los totales traen sólo la cantidad")
    void sinCamposNumericosLosTotalesTraenSoloLaCantidad() {
        dadoUnBloqueConUnCampoDeTexto();
        dadoUnaPaginaVaciaConTresRegistrosEnTotal();

        cuandoSeListaSinFiltros();

        entoncesLosTotalesTraenSoloLaCantidadDeTres();
    }

    // --- dado ---
    private void dadoUnBloqueConUnCampoNumerico() {
        dadoUnBloqueConCampos(List.of(Campo.builder().id(CAMPO_ID).tipo(TipoCampo.NUMERO).build()));
    }

    private void dadoUnBloqueConUnCampoDeTexto() {
        dadoUnBloqueConCampos(List.of(Campo.builder().id(CAMPO_ID).tipo(TipoCampo.TEXTO).build()));
    }

    private void dadoUnBloqueConCampos(List<Campo> campos) {
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(campos);
    }

    private void dadoQueElBloqueNoEsDelNegocio() {
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El bloque no existe"));
    }

    private void dadoUnaPaginaConUnRegistroYTreintaDeSuma() {
        Registro registro = Registro.builder().id(REGISTRO_ID).valores(Map.of()).build();
        when(registroRepository.buscar(any(ConsultaRegistros.class))).thenReturn(new Pagina<>(List.of(registro), 1, 5, 6));
        when(registroRepository.sumarNumericos(any(ConsultaRegistros.class), eq(List.of(CAMPO_ID))))
                .thenReturn(Map.of(CAMPO_ID, new BigDecimal("30")));
    }

    private void dadoUnaPaginaVaciaConTresRegistrosEnTotal() {
        when(registroRepository.buscar(any(ConsultaRegistros.class))).thenReturn(new Pagina<>(List.of(), 0, 20, 3));
        when(registroRepository.sumarNumericos(any(ConsultaRegistros.class), eq(List.of()))).thenReturn(Map.of());
    }

    // --- cuando ---
    private void cuandoSeListaConTextoOrdenYPagina() {
        listado = listarRegistros.execute(new ListarRegistrosRequest(
                USUARIO_ID, BLOQUE_ID, " harina ", CAMPO_ID.toString(), "asc", 1, 5));
    }

    private void cuandoSeListaSinFiltros() {
        listado = listarRegistros.execute(
                new ListarRegistrosRequest(USUARIO_ID, BLOQUE_ID, null, null, null, null, null));
    }

    private void cuandoSeIntentaListarSinFiltros() {
        rechazo = catchThrowable(this::cuandoSeListaSinFiltros);
    }

    // --- entonces ---
    private void entoncesSeConsultoConLosFiltrosPedidos() {
        ArgumentCaptor<ConsultaRegistros> consulta = ArgumentCaptor.forClass(ConsultaRegistros.class);
        verify(registroRepository).buscar(consulta.capture());
        assertThat(consulta.getValue().negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(consulta.getValue().bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(consulta.getValue().texto()).isEqualTo("harina");
        assertThat(consulta.getValue().campoOrdenId()).isEqualTo(CAMPO_ID);
        assertThat(consulta.getValue().direccion()).isEqualTo(DireccionOrden.ASC);
        assertThat(consulta.getValue().pagina()).isEqualTo(1);
        assertThat(consulta.getValue().tamanio()).isEqualTo(5);
    }

    private void entoncesSeDevuelveLaPaginaConSusTotales() {
        assertThat(listado.contenido()).extracting("id").containsExactly(REGISTRO_ID);
        assertThat(listado.totalElementos()).isEqualTo(6);
        assertThat(listado.totalPaginas()).isEqualTo(2);
        assertThat(listado.totales().cantidad()).isEqualTo(6);
        assertThat(listado.totales().sumas()).containsOnly(entry(CAMPO_ID, new BigDecimal("30")));
    }

    private void entoncesSeRechazaPorBloqueInexistenteSinConsultar() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(campoRepository, registroRepository);
    }

    private void entoncesLosTotalesTraenSoloLaCantidadDeTres() {
        assertThat(listado.totales().cantidad()).isEqualTo(3);
        assertThat(listado.totales().sumas()).isEmpty();
    }
}
