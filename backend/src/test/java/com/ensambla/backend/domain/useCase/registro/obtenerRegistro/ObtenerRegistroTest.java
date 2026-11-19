package com.ensambla.backend.domain.useCase.registro.obtenerRegistro;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerRegistroTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("c0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final UUID CANTIDAD_ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final UUID DOBLE_ID = UUID.fromString("e0000000-0000-4000-8000-000000000002");

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private CampoRepository campoRepository;

    @Mock
    private RegistroRepository registroRepository;

    private ObtenerRegistro obtenerRegistro;
    private ObtenerRegistroResponse obtenido;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        obtenerRegistro = new ObtenerRegistro(negocioRepository, campoRepository, registroRepository);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Devuelve el registro del negocio de la sesión mapeado")
    void devuelveElRegistroDelNegocioDeLaSesionMapeado() {
        dadoUnRegistroConElDato("dato");

        cuandoSeObtieneElRegistro();

        entoncesSeDevuelveElRegistroConElDato("dato");
    }

    @Test
    @DisplayName("Incluye el valor calculado de los campos fórmula")
    void incluyeElValorCalculadoDeLosCamposFormula() {
        dadoUnRegistroConUnaCantidadDeCuatroYUnaFormulaQueLaDuplica();

        cuandoSeObtieneElRegistro();

        entoncesLaFormulaDuplicaLaCantidad("8");
    }

    @Test
    @DisplayName("Propaga 'no existe' cuando el registro es de otro negocio")
    void propagaNoExisteCuandoElRegistroEsDeOtroNegocio() {
        dadoQueElRegistroEsDeOtroNegocio();

        cuandoSeIntentaObtenerElRegistro();

        entoncesSeRechazaPorRegistroInexistente();
    }

    // --- dado ---
    private void dadoUnRegistroConElDato(String dato) {
        when(registroRepository.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(
                Registro.builder().id(REGISTRO_ID).bloqueId(BLOQUE_ID).valores(Map.of("campo", dato)).build());
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());
    }

    private void dadoUnRegistroConUnaCantidadDeCuatroYUnaFormulaQueLaDuplica() {
        Campo doble = Campo.builder().id(CampoId.de(DOBLE_ID)).tipo(TipoCampo.FORMULA)
                .formula("{" + CANTIDAD_ID + "} * 2").build();
        when(registroRepository.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(Registro.builder()
                .id(REGISTRO_ID).bloqueId(BLOQUE_ID).valores(Map.of(CANTIDAD_ID.toString(), 4)).build());
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of(doble));
    }

    private void dadoQueElRegistroEsDeOtroNegocio() {
        when(registroRepository.obtener(REGISTRO_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El registro no existe"));
    }

    // --- cuando ---
    private void cuandoSeObtieneElRegistro() {
        obtenido = obtenerRegistro.execute(new ObtenerRegistroRequest(USUARIO_ID, REGISTRO_ID));
    }

    private void cuandoSeIntentaObtenerElRegistro() {
        rechazo = catchThrowable(this::cuandoSeObtieneElRegistro);
    }

    // --- entonces ---
    private void entoncesSeDevuelveElRegistroConElDato(String dato) {
        assertThat(obtenido.registro().id()).isEqualTo(REGISTRO_ID);
        assertThat(obtenido.registro().valores()).containsEntry("campo", dato);
    }

    private void entoncesLaFormulaDuplicaLaCantidad(String esperado) {
        assertThat(obtenido.registro().valores()).containsEntry(DOBLE_ID.toString(), new BigDecimal(esperado));
    }

    private void entoncesSeRechazaPorRegistroInexistente() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }
}
