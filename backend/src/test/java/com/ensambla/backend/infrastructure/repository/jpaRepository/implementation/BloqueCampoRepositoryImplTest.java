package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.BloqueRepositoryMapper;
import com.ensambla.backend.infrastructure.repository.mapper.CampoRepositoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueCampoRepositoryImplTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");

    @Mock
    private IBloqueJpaRepository bloqueJpa;

    @Mock
    private ICampoJpaRepository campoJpa;

    private BloqueRepositoryImpl bloqueRepository;
    private CampoRepositoryImpl campoRepository;
    private Optional<Bloque> encontrado;
    private Bloque guardado;
    private List<Campo> campos;

    @BeforeEach
    void dadoLosRepositoriosDeBloquesYCampos() {
        bloqueRepository = new BloqueRepositoryImpl(bloqueJpa, new BloqueRepositoryMapper());
        campoRepository = new CampoRepositoryImpl(campoJpa, new CampoRepositoryMapper());
    }

    @Test
    @DisplayName("El bloque se busca por id dentro del negocio y se mapea")
    void elBloqueSeBuscaPorIdDentroDelNegocioYSeMapea() {
        dadoQueExisteElBloqueProductos();

        cuandoSeBuscaElBloquePorId();

        entoncesSeEncuentraElBloqueProductos();
    }

    @Test
    @DisplayName("Guardar un bloque persiste la entidad y devuelve lo que guardó la base")
    void guardarUnBloquePersisteLaEntidadYDevuelveLoQueGuardoLaBase() {
        dadoQueGuardarDevuelveElBloqueClientes();

        cuandoSeGuardaUnBloqueNuevo();

        entoncesSeDevuelveElBloqueClientesGuardado();
    }

    @Test
    @DisplayName("Eliminar un bloque delega el borrado con el filtro de negocio")
    void eliminarUnBloqueDelegaElBorradoConElFiltroDeNegocio() {
        cuandoSeEliminaElBloque();

        entoncesSeDelegoElBorradoDelBloqueConElFiltroDeNegocio();
    }

    @Test
    @DisplayName("Los campos del bloque se devuelven mapeados y en el orden de la consulta")
    void losCamposDelBloqueSeDevuelvenMapeadosYEnElOrdenDeLaConsulta() {
        dadoQueElBloqueTieneUnCampoPrimeroYUnoSegundo();

        cuandoSeListanLosCamposDelBloque();

        entoncesLosCamposVienenEnOrdenPrimeroYSegundo();
    }

    // --- dado ---
    private void dadoQueExisteElBloqueProductos() {
        when(bloqueJpa.findByIdAndNegocioId(BLOQUE_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(BloqueEntity.builder().id(BLOQUE_ID.valor()).nombre("Productos").build()));
    }

    private void dadoQueGuardarDevuelveElBloqueClientes() {
        when(bloqueJpa.save(any())).thenReturn(BloqueEntity.builder().id(BLOQUE_ID.valor()).nombre("Clientes").build());
    }

    private void dadoQueElBloqueTieneUnCampoPrimeroYUnoSegundo() {
        when(campoJpa.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(BLOQUE_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(List.of(
                        CampoEntity.builder().nombre("Primero").tipo(TipoCampo.TEXTO).build(),
                        CampoEntity.builder().nombre("Segundo").tipo(TipoCampo.TEXTO).build()));
    }

    // --- cuando ---
    private void cuandoSeBuscaElBloquePorId() {
        encontrado = bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID);
    }

    private void cuandoSeGuardaUnBloqueNuevo() {
        guardado = bloqueRepository.save(Bloque.builder().negocioId(NEGOCIO_ID).nombre("Clientes").build());
    }

    private void cuandoSeEliminaElBloque() {
        bloqueRepository.deleteByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID);
    }

    private void cuandoSeListanLosCamposDelBloque() {
        campos = campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID);
    }

    // --- entonces ---
    private void entoncesSeEncuentraElBloqueProductos() {
        assertThat(encontrado).map(Bloque::getNombre).contains("Productos");
    }

    private void entoncesSeDevuelveElBloqueClientesGuardado() {
        assertThat(guardado.getId()).isEqualTo(BLOQUE_ID);
        assertThat(guardado.getNombre()).isEqualTo("Clientes");
    }

    private void entoncesSeDelegoElBorradoDelBloqueConElFiltroDeNegocio() {
        verify(bloqueJpa).deleteByIdAndNegocioId(BLOQUE_ID.valor(), NEGOCIO_ID.valor());
    }

    private void entoncesLosCamposVienenEnOrdenPrimeroYSegundo() {
        assertThat(campos).extracting(Campo::getNombre).containsExactly("Primero", "Segundo");
    }
}
