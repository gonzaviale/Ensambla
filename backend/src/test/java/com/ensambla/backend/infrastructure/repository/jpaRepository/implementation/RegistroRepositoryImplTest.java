package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.DireccionOrden;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRegistroJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.RegistroRepositoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroRepositoryImplTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final Instant DESDE = Instant.parse("2026-10-01T03:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-11-01T03:00:00Z");

    @Mock
    private IRegistroJpaRepository jpa;

    private RegistroRepositoryImpl repository;

    @BeforeEach
    void dadoUnRepositorioDeRegistros() {
        repository = new RegistroRepositoryImpl(jpa, new RegistroRepositoryMapper());
    }

    private Registro guardado;
    private List<Registro> registros;
    private Optional<Registro> encontrado;
    private Pagina<Registro> pagina;
    private Map<CampoId, BigDecimal> sumas;
    private long cantidad;

    @Test
    @DisplayName("Guarda el registro y devuelve el modelo persistido")
    void guardaElRegistroYDevuelveElModeloPersistido() {
        dadoQueGuardarDevuelveElRegistro();

        cuandoSeGuardaUnRegistroNuevo();

        entoncesSeDevuelveElRegistroPersistido();
    }

    @Test
    @DisplayName("Guarda varios registros juntos y devuelve los modelos persistidos")
    void guardaVariosRegistrosJuntosYDevuelveLosModelosPersistidos() {
        when(jpa.saveAll(anyList())).thenReturn(List.of(unaEntidad()));

        cuandoSeGuardanVariosRegistros();

        entoncesSeListaElRegistro();
    }

    @Test
    @DisplayName("Busca por id dentro del negocio y mapea el resultado")
    void buscaPorIdDentroDelNegocioYMapeaElResultado() {
        when(jpa.findByIdAndNegocioId(REGISTRO_ID.valor(), NEGOCIO_ID.valor())).thenReturn(Optional.of(unaEntidad()));

        cuandoSeBuscaElRegistroPorIdYNegocio();

        entoncesSeEncuentraElRegistro();
    }

    @Test
    @DisplayName("Devuelve vacío cuando el registro no es del negocio")
    void devuelveVacioCuandoElRegistroNoEsDelNegocio() {
        when(jpa.findByIdAndNegocioId(REGISTRO_ID.valor(), NEGOCIO_ID.valor())).thenReturn(Optional.empty());

        cuandoSeBuscaElRegistroPorIdYNegocio();

        entoncesNoSeEncuentraNingunRegistro();
    }

    @Test
    @DisplayName("Elimina por id")
    void eliminaPorId() {
        cuandoSeEliminaElRegistro();

        entoncesSeDelegoLaEliminacionAlJpa();
    }

    @Test
    @DisplayName("Sin texto ni campo consulta en descendente sin patrón y devuelve la página mapeada")
    void sinTextoNiCampoConsultaEnDescendenteSinPatronYDevuelveLaPaginaMapeada() {
        dadoQueLaBusquedaDescendenteDevuelveUnaPaginaDeOnce();

        cuandoSeBuscaSinTextoNiCampo();

        entoncesSeDevuelveLaPaginaMapeada();
    }

    @Test
    @DisplayName("En ascendente envía el patrón con los comodines escapados y el campo de orden")
    void enAscendenteEnviaElPatronConLosComodinesEscapadosYElCampoDeOrden() {
        dadoQueLaBusquedaAscendenteDevuelveUnaPaginaVacia();

        cuandoSeBuscaEnAscendenteConUnPatronConComodines();

        entoncesSeEnvioElPatronEscapadoYElCampoDeOrden();
    }

    @Test
    @DisplayName("Suma con el mismo patrón escapado del listado y devuelve las sumas por campo")
    void sumaConElMismoPatronEscapadoDelListadoYDevuelveLasSumasPorCampo() {
        dadoQueLaSumaDelCampoEsMilQuinientosConCincuenta();

        cuandoSeSumanLosCamposNumericosConUnPatron();

        entoncesSeDevuelveLaSumaDelCampo("1500.50");
    }

    @Test
    @DisplayName("Sin campos numéricos no consulta la base")
    void sinCamposNumericosNoConsultaLaBase() {
        cuandoSeSumanCamposNumericosSinCampos();

        entoncesNoHaySumasNiSeConsultoLaBase();
    }

    @Test
    @DisplayName("Busca los registros pedidos por id dentro del bloque y del negocio")
    void buscaLosRegistrosPedidosPorIdDentroDelBloqueYDelNegocio() {
        when(jpa.findAllByIdInAndBloqueIdAndNegocioIdOrderByCreadoEnDescIdDesc(
                List.of(REGISTRO_ID.valor()), BLOQUE_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(List.of(unaEntidad()));

        cuandoSeBuscanLosRegistrosPorIds();

        entoncesSeListaElRegistro();
    }

    @Test
    @DisplayName("Lista todos los registros del bloque y del negocio")
    void listaTodosLosRegistrosDelBloqueYDelNegocio() {
        when(jpa.findAllByBloqueIdAndNegocioIdOrderByCreadoEnAscIdAsc(BLOQUE_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(List.of(unaEntidad()));

        cuandoSeListanLosRegistrosDelBloque();

        entoncesSeListaElRegistro();
    }

    @Test
    @DisplayName("Sin ids no consulta la base")
    void sinIdsNoConsultaLaBase() {
        cuandoSeBuscanLosRegistrosSinIds();

        entoncesNoSeListaNingunRegistroNiSeConsultoLaBase();
    }

    @Test
    @DisplayName("El reporte busca con el patrón escapado, las fechas y el filtro como texto")
    void elReporteBuscaConElPatronEscapadoLasFechasYElFiltroComoTexto() {
        dadoQueElReporteFiltradoDevuelveUnRegistro();

        cuandoSeBuscanLosRegistrosFiltradosDelReporte();

        entoncesSeListaElRegistro();
    }

    @Test
    @DisplayName("El reporte sin filtros pasa todo nulo")
    void elReporteSinFiltrosPasaTodoNulo() {
        when(jpa.buscarParaReporte(NEGOCIO_ID.valor(), BLOQUE_ID.valor(), null, null, null, null, null, 5))
                .thenReturn(List.of());

        cuandoSeBuscanLosRegistrosDelReporteSinFiltros();

        entoncesNoSeListaNingunRegistro();
    }

    @Test
    @DisplayName("Cuenta las filas del reporte con el mismo filtro")
    void cuentaLasFilasDelReporteConElMismoFiltro() {
        when(jpa.contarParaReporte(NEGOCIO_ID.valor(), BLOQUE_ID.valor(), null, null, null, CAMPO_ID.toString(), "x"))
                .thenReturn(7L);

        cuandoSeCuentanLasFilasDelReporte();

        entoncesSeCuentanSiete();
    }

    @Test
    @DisplayName("Suma las columnas numéricas del reporte y no consulta si no hay ninguna")
    void sumaLasColumnasNumericasDelReporteYNoConsultaSiNoHayNinguna() {
        dadoQueLaSumaDelReporteDelCampoEsDoceYMedio();

        cuandoSeSumanLasColumnasNumericasDelReporte();

        entoncesSeDevuelveLaSumaDelCampo("12.5");
        entoncesSinColumnasNumericasElReporteNoSuma();
    }

    // --- dado ---
    private void dadoQueGuardarDevuelveElRegistro() {
        when(jpa.save(any(RegistroEntity.class))).thenReturn(unaEntidad());
    }

    private void dadoQueLaBusquedaDescendenteDevuelveUnaPaginaDeOnce() {
        Pageable pageable = PageRequest.of(1, 10);
        when(jpa.buscarDescendente(eq(NEGOCIO_ID.valor()), eq(BLOQUE_ID.valor()), eq(null), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(unaEntidad()), pageable, 11));
    }

    private void dadoQueLaBusquedaAscendenteDevuelveUnaPaginaVacia() {
        when(jpa.buscarAscendente(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    }

    private void dadoQueLaSumaDelCampoEsMilQuinientosConCincuenta() {
        when(jpa.sumarNumericos(eq(NEGOCIO_ID.valor()), eq(BLOQUE_ID.valor()), eq("%50\\%%"),
                eq(List.of(CAMPO_ID.toString()))))
                .thenReturn(List.of(unaSuma(CAMPO_ID, "1500.50")));
    }

    private void dadoQueElReporteFiltradoDevuelveUnRegistro() {
        when(jpa.buscarParaReporte(NEGOCIO_ID.valor(), BLOQUE_ID.valor(), "%50\\%%", DESDE.toString(),
                HASTA.toString(), CAMPO_ID.toString(), "true", 10)).thenReturn(List.of(unaEntidad()));
    }

    private void dadoQueLaSumaDelReporteDelCampoEsDoceYMedio() {
        when(jpa.sumarNumericosParaReporte(NEGOCIO_ID.valor(), BLOQUE_ID.valor(), "%a%", null, null, null, null,
                List.of(CAMPO_ID.toString()))).thenReturn(List.of(unaSuma(CAMPO_ID, "12.5")));
    }

    private IRegistroJpaRepository.SumaPorCampo unaSuma(CampoId campoId, String valor) {
        return new IRegistroJpaRepository.SumaPorCampo() {
            @Override
            public String getCampo() {
                return campoId.toString();
            }

            @Override
            public BigDecimal getSuma() {
                return new BigDecimal(valor);
            }
        };
    }

    private ConsultaRegistros unaConsulta(String texto, CampoId campoOrdenId, DireccionOrden direccion, int numero,
                                          int tamanio) {
        return new ConsultaRegistros(NEGOCIO_ID, BLOQUE_ID, texto, campoOrdenId, direccion, numero, tamanio);
    }

    private RegistroEntity unaEntidad() {
        return RegistroEntity.builder()
                .id(REGISTRO_ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .bloqueId(BLOQUE_ID.valor())
                .valores(Map.of("k", "v"))
                .build();
    }

    private FiltroReporte unFiltroCompleto() {
        return new FiltroReporte(NEGOCIO_ID, BLOQUE_ID, "50%", DESDE, HASTA, CAMPO_ID, "true");
    }

    // --- cuando ---
    private void cuandoSeGuardaUnRegistroNuevo() {
        guardado = repository.save(Registro.builder().bloqueId(BLOQUE_ID).valores(Map.of("k", "v")).build());
    }

    private void cuandoSeGuardanVariosRegistros() {
        registros = repository.saveAll(List.of(Registro.builder().bloqueId(BLOQUE_ID).build()));
    }

    private void cuandoSeBuscaElRegistroPorIdYNegocio() {
        encontrado = repository.findByIdAndNegocioId(REGISTRO_ID, NEGOCIO_ID);
    }

    private void cuandoSeEliminaElRegistro() {
        repository.deleteById(REGISTRO_ID);
    }

    private void cuandoSeBuscaSinTextoNiCampo() {
        pagina = repository.buscar(unaConsulta(null, null, DireccionOrden.DESC, 1, 10));
    }

    private void cuandoSeBuscaEnAscendenteConUnPatronConComodines() {
        repository.buscar(unaConsulta("50%_a\\b", CAMPO_ID, DireccionOrden.ASC, 0, 20));
    }

    private void cuandoSeSumanLosCamposNumericosConUnPatron() {
        sumas = repository.sumarNumericos(unaConsulta("50%", null, DireccionOrden.DESC, 0, 20), List.of(CAMPO_ID));
    }

    private void cuandoSeSumanCamposNumericosSinCampos() {
        sumas = repository.sumarNumericos(unaConsulta(null, null, DireccionOrden.DESC, 0, 20), List.of());
    }

    private void cuandoSeBuscanLosRegistrosPorIds() {
        registros = repository.findAllByIdsAndBloque(List.of(REGISTRO_ID), BLOQUE_ID, NEGOCIO_ID);
    }

    private void cuandoSeListanLosRegistrosDelBloque() {
        registros = repository.findAllByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID);
    }

    private void cuandoSeBuscanLosRegistrosSinIds() {
        registros = repository.findAllByIdsAndBloque(List.of(), BLOQUE_ID, NEGOCIO_ID);
    }

    private void cuandoSeBuscanLosRegistrosFiltradosDelReporte() {
        registros = repository.buscarFiltrados(unFiltroCompleto(), 10);
    }

    private void cuandoSeBuscanLosRegistrosDelReporteSinFiltros() {
        registros = repository.buscarFiltrados(
                new FiltroReporte(NEGOCIO_ID, BLOQUE_ID, null, null, null, null, null), 5);
    }

    private void cuandoSeCuentanLasFilasDelReporte() {
        cantidad = repository.contarFiltrados(new FiltroReporte(NEGOCIO_ID, BLOQUE_ID, null, null, null, CAMPO_ID, "x"));
    }

    private void cuandoSeSumanLasColumnasNumericasDelReporte() {
        sumas = repository.sumarNumericos(
                new FiltroReporte(NEGOCIO_ID, BLOQUE_ID, "a", null, null, null, null), List.of(CAMPO_ID));
    }

    // --- entonces ---
    private void entoncesSeDevuelveElRegistroPersistido() {
        assertThat(guardado.getId()).isEqualTo(REGISTRO_ID);
        assertThat(guardado.getValores()).containsEntry("k", "v");
    }

    private void entoncesSeListaElRegistro() {
        assertThat(registros).extracting(Registro::getId).containsExactly(REGISTRO_ID);
    }

    private void entoncesNoSeListaNingunRegistro() {
        assertThat(registros).isEmpty();
    }

    private void entoncesSeEncuentraElRegistro() {
        assertThat(encontrado).map(Registro::getId).contains(REGISTRO_ID);
    }

    private void entoncesNoSeEncuentraNingunRegistro() {
        assertThat(encontrado).isEmpty();
    }

    private void entoncesSeDelegoLaEliminacionAlJpa() {
        verify(jpa).deleteById(REGISTRO_ID.valor());
    }

    private void entoncesSeDevuelveLaPaginaMapeada() {
        assertThat(pagina.contenido()).extracting(Registro::getId).containsExactly(REGISTRO_ID);
        assertThat(pagina.numero()).isEqualTo(1);
        assertThat(pagina.tamanio()).isEqualTo(10);
        assertThat(pagina.totalElementos()).isEqualTo(11);
    }

    private void entoncesSeEnvioElPatronEscapadoYElCampoDeOrden() {
        ArgumentCaptor<String> patron = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> campoOrden = ArgumentCaptor.forClass(String.class);
        verify(jpa).buscarAscendente(eq(NEGOCIO_ID.valor()), eq(BLOQUE_ID.valor()), patron.capture(),
                campoOrden.capture(), eq(PageRequest.of(0, 20)));
        assertThat(patron.getValue()).isEqualTo("%50\\%\\_a\\\\b%");
        assertThat(campoOrden.getValue()).isEqualTo(CAMPO_ID.toString());
    }

    private void entoncesSeDevuelveLaSumaDelCampo(String esperada) {
        assertThat(sumas).containsOnly(entry(CAMPO_ID, new BigDecimal(esperada)));
    }

    private void entoncesNoHaySumasNiSeConsultoLaBase() {
        assertThat(sumas).isEmpty();
        verifyNoInteractions(jpa);
    }

    private void entoncesNoSeListaNingunRegistroNiSeConsultoLaBase() {
        assertThat(registros).isEmpty();
        verifyNoInteractions(jpa);
    }

    private void entoncesSeCuentanSiete() {
        assertThat(cantidad).isEqualTo(7L);
    }

    private void entoncesSinColumnasNumericasElReporteNoSuma() {
        assertThat(repository.sumarNumericos(
                new FiltroReporte(NEGOCIO_ID, BLOQUE_ID, "a", null, null, null, null), List.of())).isEmpty();
    }
}
