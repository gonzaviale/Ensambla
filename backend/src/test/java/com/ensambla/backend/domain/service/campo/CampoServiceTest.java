package com.ensambla.backend.domain.service.campo;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampoServiceTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("d0000000-0000-4000-8000-000000000001");
    private static final CampoId OTRO_CAMPO_ID = CampoId.de("d0000000-0000-4000-8000-000000000002");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("e0000000-0000-4000-8000-000000000001");
    private static final ProyectoId OTRO_PROYECTO_ID = ProyectoId.de("e0000000-0000-4000-8000-000000000002");
    private static final BloqueId RELACIONADO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T15:00:00Z");

    @Mock
    private CampoRepository campoRepository;

    @Mock
    private BloqueRepository bloqueRepository;

    @Spy
    private TransaccionPort transaccionPort = new TransaccionDirecta();

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

    @Test
    @DisplayName("Devuelve el campo del negocio")
    void obtenerShouldReturnCampoOfNegocio() {
        Campo campo = givenCampo(CAMPO_ID.valor(), "Precio", 0);
        when(campoRepository.findByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID)).thenReturn(Optional.of(campo));

        Campo result = campoService.obtener(CAMPO_ID, NEGOCIO_ID);

        assertThat(result).isSameAs(campo);
    }

    @Test
    @DisplayName("Responde 'no existe' cuando el campo es de otro negocio o no existe")
    void obtenerShouldThrowWhenCampoIsNotFound() {
        when(campoRepository.findByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campoService.obtener(CAMPO_ID, NEGOCIO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El campo no existe");
    }

    @Test
    @DisplayName("Crear carga los campos del bloque y guarda el nuevo al final")
    void crearShouldSaveCampoAfterTheLastOne() {
        givenCamposDelBloque(givenCampo(OTRO_CAMPO_ID.valor(), "Producto", 1));
        givenSaveReturnsArgument();

        Campo result = campoService.crear(givenBloque(), configuracion(" Precio "), AHORA);

        assertThat(result.getNombre()).isEqualTo("Precio");
        assertThat(result.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getPosicion()).isEqualTo((short) 2);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Crear con un nombre repetido en el bloque no guarda nada")
    void crearShouldNotSaveWhenNameIsTaken() {
        givenCamposDelBloque(givenCampo(OTRO_CAMPO_ID.valor(), "Producto", 0));
        Bloque bloque = givenBloque();

        assertThatThrownBy(() -> campoService.crear(bloque, configuracion("producto"), AHORA))
                .isInstanceOf(DuplicateResourceException.class);
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear con una configuración inválida no guarda nada")
    void crearShouldNotSaveWhenConfiguracionIsInvalid() {
        givenCamposDelBloque();
        Bloque bloque = givenBloque();
        ConfiguracionCampo invalida = new ConfiguracionCampo("Talle", "OPCION", null, null, null, null, null);

        assertThatThrownBy(() -> campoService.crear(bloque, invalida, AHORA))
                .isInstanceOf(CampoInvalidoException.class);
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear un campo que no es de conexión no consulta otras tablas")
    void crearShouldNotLookUpBloqueWhenCampoIsNotRelacion() {
        givenCamposDelBloque();
        givenSaveReturnsArgument();

        campoService.crear(givenBloque(), configuracion("Precio"), AHORA);

        verifyNoInteractions(bloqueRepository);
    }

    @Test
    @DisplayName("Crear guarda una conexión con una tabla del mismo sistema")
    void crearShouldSaveRelacionWhenBloqueIsOfSameProyecto() {
        givenCamposDelBloque();
        givenBloqueRelacionado(PROYECTO_ID);
        givenSaveReturnsArgument();

        Campo result = campoService.crear(givenBloque(), relacion(), AHORA);

        assertThat(result.getTipo()).isEqualTo(TipoCampo.RELACION);
        assertThat(result.getBloqueRelacionadoId()).isEqualTo(RELACIONADO_ID);
    }

    @Test
    @DisplayName("Crear rechaza una conexión con una tabla inexistente o de otro negocio")
    void crearShouldRejectRelacionWhenBloqueIsMissing() {
        givenCamposDelBloque();
        when(bloqueRepository.findByIdAndNegocioId(RELACIONADO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
        Bloque bloque = givenBloque();

        assertThatThrownBy(() -> campoService.crear(bloque, relacion(), AHORA))
                .isInstanceOf(CampoInvalidoException.class)
                .extracting(error -> ((CampoInvalidoException) error).getErrores())
                .isEqualTo(Map.of("bloqueRelacionadoId", "Elegí una tabla de este sistema"));
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear rechaza una conexión con una tabla de otro sistema")
    void crearShouldRejectRelacionWhenBloqueIsOfOtherProyecto() {
        givenCamposDelBloque();
        givenBloqueRelacionado(OTRO_PROYECTO_ID);
        Bloque bloque = givenBloque();

        assertThatThrownBy(() -> campoService.crear(bloque, relacion(), AHORA))
                .isInstanceOf(CampoInvalidoException.class);
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear guarda una fórmula que usa campos numéricos del bloque")
    void crearShouldSaveFormulaThatUsesNumericFields() {
        givenCamposDelBloque(givenCampo(OTRO_CAMPO_ID.valor(), "Cantidad", 0));
        givenSaveReturnsArgument();

        Campo result = campoService.crear(givenBloque(), formula("{" + OTRO_CAMPO_ID + "} * 2"), AHORA);

        assertThat(result.getTipo()).isEqualTo(TipoCampo.FORMULA);
        assertThat(result.getFormula()).isEqualTo("{" + OTRO_CAMPO_ID + "} * 2");
    }

    @Test
    @DisplayName("Crear rechaza una fórmula que cita un campo que no es del bloque")
    void crearShouldRejectFormulaWithUnknownField() {
        givenCamposDelBloque(givenCampo(OTRO_CAMPO_ID.valor(), "Cantidad", 0));
        Bloque bloque = givenBloque();
        ConfiguracionCampo ajena = formula("{" + CAMPO_ID + "} * 2");

        assertThatThrownBy(() -> campoService.crear(bloque, ajena, AHORA))
                .isInstanceOfSatisfying(CampoInvalidoException.class, e -> assertThat(e.getErrores())
                        .containsEntry("formula", "Usá sólo campos numéricos de esta tabla"));
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear rechaza una fórmula que cita un campo que no es numérico")
    void crearShouldRejectFormulaWithNonNumericField() {
        Campo texto = givenCampo(OTRO_CAMPO_ID.valor(), "Nombre", 0);
        texto.setTipo(TipoCampo.TEXTO);
        givenCamposDelBloque(texto);
        Bloque bloque = givenBloque();
        ConfiguracionCampo conTexto = formula("{" + OTRO_CAMPO_ID + "} * 2");

        assertThatThrownBy(() -> campoService.crear(bloque, conTexto, AHORA))
                .isInstanceOf(CampoInvalidoException.class);
    }

    // --- arrange ---
    private void givenCamposDelBloque(Campo... campos) {
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of(campos));
    }

    private void givenSaveReturnsArgument() {
        when(campoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenBloqueRelacionado(ProyectoId proyectoId) {
        Bloque relacionado = Bloque.builder().id(RELACIONADO_ID).negocioId(NEGOCIO_ID).proyectoId(proyectoId).build();
        when(bloqueRepository.findByIdAndNegocioId(RELACIONADO_ID, NEGOCIO_ID)).thenReturn(Optional.of(relacionado));
    }

    // --- helpers ---
    private Bloque givenBloque() {
        return Bloque.builder().id(BLOQUE_ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).build();
    }

    private Campo givenCampo(UUID id, String nombre, int posicion) {
        return Campo.builder()
                .id(CampoId.de(id))
                .negocioId(NEGOCIO_ID)
                .bloqueId(BLOQUE_ID)
                .nombre(nombre)
                .tipo(TipoCampo.NUMERO)
                .posicion((short) posicion)
                .build();
    }

    private ConfiguracionCampo configuracion(String nombre) {
        return new ConfiguracionCampo(nombre, "TEXTO", null, null, null, null, null);
    }

    private ConfiguracionCampo relacion() {
        return new ConfiguracionCampo("Cliente", "RELACION", null, null, null, RELACIONADO_ID, null);
    }

    private ConfiguracionCampo formula(String expresion) {
        return new ConfiguracionCampo("Total", "FORMULA", null, null, null, null, expresion);
    }

    private Campo givenFormula(UUID id, String nombre, String expresion) {
        return Campo.builder().id(CampoId.de(id)).negocioId(NEGOCIO_ID).bloqueId(BLOQUE_ID).nombre(nombre)
                .tipo(TipoCampo.FORMULA).formula(expresion).posicion((short) 1).build();
    }
}
