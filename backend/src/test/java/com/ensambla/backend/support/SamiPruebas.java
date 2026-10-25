package com.ensambla.backend.support;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.BloquePlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.CampoPlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.RespuestaSami;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.SamiIaPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSami;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiRequest;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.PropuestaSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.RespuestaSamiItem;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

/**
 * Datos compartidos por los tests de Sami: el diccionario, el catálogo de componentes y los kits reales, y las cinco
 * plantillas sembradas. También interpreta un texto con el diccionario a través de {@link ConversarConSami}.
 */
public final class SamiPruebas {

    private SamiPruebas() {
    }

    public static DiccionarioSami diccionarioReal() {
        try (InputStream json = SamiPruebas.class.getResourceAsStream("/ia/sami-diccionario.json")) {
            return JsonMapper.builder().build().readValue(json, DiccionarioSami.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** El catálogo de componentes que genera el frontend. */
    public static CatalogoComponentes catalogoReal() {
        return leer("/ia/catalogo-componentes.json", CatalogoComponentes.class);
    }

    /** Los kits por rubro que usa producción. */
    public static KitsRubro kitsReales() {
        return new KitsRubro(List.of(leer("/ia/kits-rubro.json", KitRubro[].class)));
    }

    private static <T> T leer(String recurso, Class<T> tipo) {
        try (InputStream json = SamiPruebas.class.getResourceAsStream(recurso)) {
            return JsonMapper.builder().build().readValue(json, tipo);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<PlantillaProyecto> plantillas() {
        return List.of(
                plantilla("Inventario", campo("Nombre", TipoCampo.TEXTO), campo("Stock", TipoCampo.NUMERO)),
                plantilla("Pedidos", campo("Cliente", TipoCampo.TEXTO), campo("Entrega", TipoCampo.FECHA)),
                plantilla("Clientes", campo("Nombre", TipoCampo.TEXTO), campo("Teléfono", TipoCampo.TEXTO)),
                plantilla("Ventas", campo("Producto", TipoCampo.TEXTO), campo("Monto", TipoCampo.NUMERO)),
                plantilla("Gastos", campo("Detalle", TipoCampo.TEXTO), campo("Monto", TipoCampo.NUMERO)));
    }

    public static PlantillaProyecto plantilla(String nombre, CampoPlantilla... campos) {
        return new PlantillaProyecto(PlantillaId.nuevo(), nombre, "",
                new EstructuraPlantilla(List.of(new BloquePlantilla(nombre, List.of(campos)))));
    }

    public static CampoPlantilla campo(String nombre, TipoCampo tipo) {
        return new CampoPlantilla(nombre, tipo, false, null, null);
    }

    /**
     * La propuesta del diccionario para {@code texto}, sin preguntar ni consultar a la IA: sin umbral de confianza,
     * cualquier plantilla reconocida se propone con su confianza. Vacía si no reconoce ninguna plantilla.
     */
    public static Optional<PropuestaSamiItem> interpretar(DiccionarioSami diccionario, String texto,
                                                          List<PlantillaProyecto> disponibles,
                                                          RespuestaSami... respuestas) {
        PlantillaRepository plantillaRepository = mock(PlantillaRepository.class);
        ConsumoSamiRepository consumoSamiRepository = mock(ConsumoSamiRepository.class);
        when(plantillaRepository.findAllParaProyectoActivas()).thenReturn(disponibles);
        ConversarConSami conversacion = new ConversarConSami(mock(NegocioRepository.class), plantillaRepository,
                diccionario, mock(SamiIaPort.class), consumoSamiRepository, Clock.systemUTC(), 0.0, 0, 0);
        List<RespuestaSamiItem> items = Arrays.stream(respuestas)
                .map(respuesta -> new RespuestaSamiItem(respuesta.preguntaId(), respuesta.opcionId()))
                .toList();
        try {
            return Optional.of(conversacion.execute(new ConversarConSamiRequest(UsuarioId.nuevo(), texto, items))
                    .propuesta());
        } catch (BusinessRuleException e) {
            if (mockingDetails(consumoSamiRepository).getInvocations().isEmpty()) {
                throw e;
            }
            return Optional.empty();
        }
    }
}
