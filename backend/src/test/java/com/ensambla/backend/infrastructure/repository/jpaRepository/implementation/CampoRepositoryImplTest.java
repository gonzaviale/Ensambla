package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.CampoRepositoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampoRepositoryImplTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("d0000000-0000-4000-8000-000000000001");

    @Mock
    private ICampoJpaRepository campoJpa;

    private CampoRepositoryImpl campoRepositoryImpl;

    @BeforeEach
    void dadoUnRepositorioDeCampos() {
        campoRepositoryImpl = new CampoRepositoryImpl(campoJpa, new CampoRepositoryMapper());
    }

    private Optional<Campo> encontrado;
    private Campo guardado;
    private List<Campo> relaciones;
    private ArgumentCaptor<CampoEntity> entidadGuardada;

    @Test
    @DisplayName("El campo se busca por id dentro del negocio y se mapea")
    void elCampoSeBuscaPorIdDentroDelNegocioYSeMapea() {
        dadoQueExisteElCampoPrecio();

        cuandoSeBuscaElCampoPorId();

        entoncesSeEncuentraElCampoPrecio();
    }

    @Test
    @DisplayName("Un campo de otro negocio o inexistente vuelve vacío")
    void unCampoDeOtroNegocioOInexistenteVuelveVacio() {
        dadoQueElCampoNoExiste();

        cuandoSeBuscaElCampoPorId();

        entoncesNoSeEncuentraNingunCampo();
    }

    @Test
    @DisplayName("Guardar un campo persiste la entidad completa y devuelve lo que guardó la base")
    void guardarUnCampoPersisteLaEntidadCompletaYDevuelveLoQueGuardoLaBase() {
        dadoQueGuardarDevuelveElCampoTalle();

        cuandoSeGuardaUnCampoNuevo();

        entoncesSeDevuelveElCampoGuardadoYSePersistioLaEntidadCompleta();
    }

    @Test
    @DisplayName("Las conexiones hacia un bloque se buscan dentro del negocio y se mapean")
    void lasConexionesHaciaUnBloqueSeBuscanDentroDelNegocioYSeMapean() {
        dadoQueUnCampoClienteApuntaAlBloque();

        cuandoSeBuscanLasConexionesHaciaElBloque();

        entoncesSeMapeaLaConexionDeCliente();
    }

    @Test
    @DisplayName("Eliminar un campo delega el borrado con el filtro de negocio")
    void eliminarUnCampoDelegaElBorradoConElFiltroDeNegocio() {
        cuandoSeEliminaElCampo();

        entoncesSeDelegoElBorradoConElFiltroDeNegocio();
    }

    // --- dado ---
    private void dadoQueExisteElCampoPrecio() {
        when(campoJpa.findByIdAndNegocioId(CAMPO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(CampoEntity.builder()
                        .id(CAMPO_ID.valor())
                        .nombre("Precio")
                        .tipo(TipoCampo.TEXTO)
                        .build()));
    }

    private void dadoQueElCampoNoExiste() {
        when(campoJpa.findByIdAndNegocioId(CAMPO_ID.valor(), NEGOCIO_ID.valor())).thenReturn(Optional.empty());
    }

    private void dadoQueGuardarDevuelveElCampoTalle() {
        entidadGuardada = ArgumentCaptor.forClass(CampoEntity.class);
        when(campoJpa.save(entidadGuardada.capture()))
                .thenReturn(CampoEntity.builder().id(CAMPO_ID.valor()).nombre("Talle").tipo(TipoCampo.OPCION).build());
    }

    private void dadoQueUnCampoClienteApuntaAlBloque() {
        when(campoJpa.findRelacionesHacia(BLOQUE_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(List.of(CampoEntity.builder().id(CAMPO_ID.valor()).nombre("Cliente")
                        .tipo(TipoCampo.RELACION)
                        .bloqueRelacionadoId(BLOQUE_ID.valor()).build()));
    }

    private Campo unCampoNuevo() {
        return Campo.builder()
                .negocioId(NEGOCIO_ID)
                .bloqueId(BLOQUE_ID)
                .nombre("Talle")
                .tipo(TipoCampo.OPCION)
                .opciones(List.of("Chico", "Grande"))
                .build();
    }

    // --- cuando ---
    private void cuandoSeBuscaElCampoPorId() {
        encontrado = campoRepositoryImpl.findByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID);
    }

    private void cuandoSeGuardaUnCampoNuevo() {
        guardado = campoRepositoryImpl.save(unCampoNuevo());
    }

    private void cuandoSeBuscanLasConexionesHaciaElBloque() {
        relaciones = campoRepositoryImpl.findRelacionesHacia(BLOQUE_ID, NEGOCIO_ID);
    }

    private void cuandoSeEliminaElCampo() {
        campoRepositoryImpl.deleteByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID);
    }

    // --- entonces ---
    private void entoncesSeEncuentraElCampoPrecio() {
        assertThat(encontrado).map(Campo::getNombre).contains("Precio");
    }

    private void entoncesNoSeEncuentraNingunCampo() {
        assertThat(encontrado).isEmpty();
    }

    private void entoncesSeDevuelveElCampoGuardadoYSePersistioLaEntidadCompleta() {
        assertThat(guardado.getId()).isEqualTo(CAMPO_ID);
        assertThat(guardado.getTipo()).isEqualTo(TipoCampo.OPCION);
        assertThat(entidadGuardada.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entidadGuardada.getValue().getOpciones()).containsExactly("Chico", "Grande");
    }

    private void entoncesSeMapeaLaConexionDeCliente() {
        assertThat(relaciones).extracting(Campo::getNombre).containsExactly("Cliente");
        assertThat(relaciones).extracting(Campo::getBloqueRelacionadoId).containsExactly(BLOQUE_ID);
    }

    private void entoncesSeDelegoElBorradoConElFiltroDeNegocio() {
        verify(campoJpa).deleteByIdAndNegocioId(CAMPO_ID.valor(), NEGOCIO_ID.valor());
    }
}
