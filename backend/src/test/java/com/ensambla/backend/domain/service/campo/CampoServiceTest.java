package com.ensambla.backend.domain.service.campo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID CAMPO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID OTRO_CAMPO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID PROYECTO_ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final UUID OTRO_PROYECTO_ID = UUID.fromString("e0000000-0000-4000-8000-000000000002");
    private static final UUID RELACIONADO_ID = UUID.fromString("b0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T15:00:00Z");

    @Mock
    private CampoRepository campoRepository;

    @Mock
    private BloqueRepository bloqueRepository;

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
        Campo campo = givenCampo(CAMPO_ID, "Precio", 0);
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
        givenCamposDelBloque(givenCampo(OTRO_CAMPO_ID, "Producto", 1));
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
        givenCamposDelBloque(givenCampo(OTRO_CAMPO_ID, "Producto", 0));
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
    @DisplayName("Editar reemplaza la configuración del campo y lo guarda")
    void editarShouldReconfigureAndSave() {
        Campo campo = givenCampo(CAMPO_ID, "Precio", 0);
        givenCamposDelBloque(givenCampo(CAMPO_ID, "Precio", 0));
        givenSaveReturnsArgument();

        Campo result = campoService.editar(givenBloque(), campo, configuracion("Costo"), AHORA);

        assertThat(result.getNombre()).isEqualTo("Costo");
        assertThat(result.getTipo()).isEqualTo(TipoCampo.TEXTO);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
        verify(campoRepository).save(campo);
    }

    @Test
    @DisplayName("Editar con el nombre de otro campo del bloque no guarda nada")
    void editarShouldNotSaveWhenNameIsTaken() {
        Campo campo = givenCampo(CAMPO_ID, "Precio", 0);
        givenCamposDelBloque(campo, givenCampo(OTRO_CAMPO_ID, "Producto", 1));
        Bloque bloque = givenBloque();

        assertThatThrownBy(() -> campoService.editar(bloque, campo, configuracion("Producto"), AHORA))
                .isInstanceOf(DuplicateResourceException.class);
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
    @DisplayName("Editar rechaza convertir el campo en una conexión con una tabla de otro sistema")
    void editarShouldRejectRelacionWhenBloqueIsOfOtherProyecto() {
        Campo campo = givenCampo(CAMPO_ID, "Precio", 0);
        givenCamposDelBloque(campo);
        givenBloqueRelacionado(OTRO_PROYECTO_ID);
        Bloque bloque = givenBloque();

        assertThatThrownBy(() -> campoService.editar(bloque, campo, relacion(), AHORA))
                .isInstanceOf(CampoInvalidoException.class);
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar guarda una conexión con una tabla del mismo sistema")
    void editarShouldSaveRelacionWhenBloqueIsOfSameProyecto() {
        Campo campo = givenCampo(CAMPO_ID, "Precio", 0);
        givenCamposDelBloque(campo);
        givenBloqueRelacionado(PROYECTO_ID);
        givenSaveReturnsArgument();

        Campo result = campoService.editar(givenBloque(), campo, relacion(), AHORA);

        assertThat(result.getBloqueRelacionadoId()).isEqualTo(RELACIONADO_ID);
    }

    @Test
    @DisplayName("Eliminar borra el campo filtrando por su negocio")
    void eliminarShouldDeleteFilteringByNegocio() {
        Campo campo = givenCampo(CAMPO_ID, "Precio", 0);

        campoService.eliminar(campo);

        verify(campoRepository).deleteByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Crear guarda una fórmula que usa campos numéricos del bloque")
    void crearShouldSaveFormulaThatUsesNumericFields() {
        givenCamposDelBloque(givenCampo(OTRO_CAMPO_ID, "Cantidad", 0));
        givenSaveReturnsArgument();

        Campo result = campoService.crear(givenBloque(), formula("{" + OTRO_CAMPO_ID + "} * 2"), AHORA);

        assertThat(result.getTipo()).isEqualTo(TipoCampo.FORMULA);
        assertThat(result.getFormula()).isEqualTo("{" + OTRO_CAMPO_ID + "} * 2");
    }

    @Test
    @DisplayName("Crear rechaza una fórmula que cita un campo que no es del bloque")
    void crearShouldRejectFormulaWithUnknownField() {
        givenCamposDelBloque(givenCampo(OTRO_CAMPO_ID, "Cantidad", 0));
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
        Campo texto = givenCampo(OTRO_CAMPO_ID, "Nombre", 0);
        texto.setTipo(TipoCampo.TEXTO);
        givenCamposDelBloque(texto);
        Bloque bloque = givenBloque();
        ConfiguracionCampo conTexto = formula("{" + OTRO_CAMPO_ID + "} * 2");

        assertThatThrownBy(() -> campoService.crear(bloque, conTexto, AHORA))
                .isInstanceOf(CampoInvalidoException.class);
    }

    @Test
    @DisplayName("Editar un campo para que sea una fórmula que se cita a sí misma no guarda nada")
    void editarShouldRejectFormulaThatUsesItself() {
        Campo campo = givenCampo(CAMPO_ID, "Total", 0);
        givenCamposDelBloque(givenCampo(CAMPO_ID, "Total", 0));
        Bloque bloque = givenBloque();
        ConfiguracionCampo propia = formula("{" + CAMPO_ID + "} * 2");

        assertThatThrownBy(() -> campoService.editar(bloque, campo, propia, AHORA))
                .isInstanceOf(CampoInvalidoException.class);
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar no deja cambiar el tipo de un campo que usa una fórmula")
    void editarShouldRejectChangingTypeOfFieldUsedByFormula() {
        Campo cantidad = givenCampo(CAMPO_ID, "Cantidad", 0);
        givenCamposDelBloque(cantidad, givenFormula(OTRO_CAMPO_ID, "Doble", "{" + CAMPO_ID + "} * 2"));
        Bloque bloque = givenBloque();
        ConfiguracionCampo texto = configuracion("Cantidad");

        assertThatThrownBy(() -> campoService.editar(bloque, cantidad, texto, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede cambiar el tipo de «Cantidad»: la fórmula «Doble» lo usa");
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar deja cambiar el tipo de un campo que ninguna fórmula usa")
    void editarShouldAllowChangingTypeOfFieldNotUsedByFormula() {
        Campo cantidad = givenCampo(CAMPO_ID, "Cantidad", 0);
        givenCamposDelBloque(cantidad, givenFormula(OTRO_CAMPO_ID, "Doble", "2 * 2"));
        givenSaveReturnsArgument();

        Campo result = campoService.editar(givenBloque(), cantidad, configuracion("Cantidad"), AHORA);

        assertThat(result.getTipo()).isEqualTo(TipoCampo.TEXTO);
    }

    @Test
    @DisplayName("Editar un campo numérico que usa una fórmula no revisa las fórmulas")
    void editarShouldKeepNumericFieldUsedByFormula() {
        Campo cantidad = givenCampo(CAMPO_ID, "Cantidad", 0);
        givenCamposDelBloque(cantidad, givenFormula(OTRO_CAMPO_ID, "Doble", "{" + CAMPO_ID + "} * 2"));
        givenSaveReturnsArgument();

        Campo result = campoService.editar(givenBloque(), cantidad, numero("Cantidad"), AHORA);

        assertThat(result.getTipo()).isEqualTo(TipoCampo.NUMERO);
    }

    @Test
    @DisplayName("Eliminar rechaza borrar un campo que usa una fórmula")
    void eliminarShouldRejectFieldUsedByFormula() {
        Campo cantidad = givenCampo(CAMPO_ID, "Cantidad", 0);
        givenCamposDelBloque(cantidad, givenFormula(OTRO_CAMPO_ID, "Doble", "{" + CAMPO_ID + "} * 2"));

        assertThatThrownBy(() -> campoService.eliminar(cantidad))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede eliminar «Cantidad»: la fórmula «Doble» lo usa");
        verify(campoRepository, never()).deleteByIdAndNegocioId(any(), any());
    }

    // --- arrange ---
    private void givenCamposDelBloque(Campo... campos) {
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of(campos));
    }

    private void givenSaveReturnsArgument() {
        when(campoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenBloqueRelacionado(UUID proyectoId) {
        Bloque relacionado = Bloque.builder().id(RELACIONADO_ID).negocioId(NEGOCIO_ID).proyectoId(proyectoId).build();
        when(bloqueRepository.findByIdAndNegocioId(RELACIONADO_ID, NEGOCIO_ID)).thenReturn(Optional.of(relacionado));
    }

    // --- helpers ---
    private Bloque givenBloque() {
        return Bloque.builder().id(BLOQUE_ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).build();
    }

    private Campo givenCampo(UUID id, String nombre, int posicion) {
        return Campo.builder()
                .id(id)
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

    private ConfiguracionCampo numero(String nombre) {
        return new ConfiguracionCampo(nombre, "NUMERO", null, null, null, null, null);
    }

    private Campo givenFormula(UUID id, String nombre, String expresion) {
        return Campo.builder().id(id).negocioId(NEGOCIO_ID).bloqueId(BLOQUE_ID).nombre(nombre)
                .tipo(TipoCampo.FORMULA).formula(expresion).posicion((short) 1).build();
    }
}
