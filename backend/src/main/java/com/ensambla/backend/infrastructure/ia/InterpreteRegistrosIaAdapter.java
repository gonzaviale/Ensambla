package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.whatsapp.ConsultaInterpretacion;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;
import com.ensambla.backend.domain.port.InterpreteRegistrosIaPort;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pasa un texto a filas con un modelo de texto. Una respuesta ilegible, con una tabla que no existe o con columnas de
 * otra tabla no lanza error: vuelve como interpretación vacía y con su consumo, para que se registre lo gastado. Sólo
 * falla (con la excepción del cliente) si el modelo no respondió.
 */
@Slf4j
public class InterpreteRegistrosIaAdapter implements InterpreteRegistrosIaPort {

    /** Forma del JSON que devuelve el modelo ({@code ia/esquema-whatsapp-registros.json}), antes de validarlo. */
    record RespuestaJson(String tabla, Double confianza, List<Map<String, String>> filas) {
    }

    private final ClienteModeloTexto modelo;
    private final PromptWhatsappRegistros prompt;
    private final ObjectMapper objectMapper;

    public InterpreteRegistrosIaAdapter(ClienteModeloTexto modelo, PromptWhatsappRegistros prompt,
                                        ObjectMapper objectMapper) {
        this.modelo = modelo;
        this.prompt = prompt;
        this.objectMapper = objectMapper;
    }

    @Override
    public InterpretacionRegistros interpretar(ConsultaInterpretacion consulta) {
        PromptWhatsappRegistros.Pedido pedido = prompt.pedido(consulta);
        RespuestaModeloTexto respuesta = modelo.completar(pedido.peticion());
        ConsumoIaSami consumo = new ConsumoIaSami(respuesta.proveedor(), respuesta.modelo(),
                respuesta.tokensEntrada(), respuesta.tokensSalida());
        try {
            RespuestaJson leida = objectMapper.readerFor(RespuestaJson.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(respuesta.json());
            return traducir(leida, pedido, consumo);
        } catch (JacksonException | IllegalArgumentException e) {
            log.warn("Sami recibió un JSON ilegible de la IA al interpretar un mensaje de WhatsApp: {}", e.getMessage());
            return InterpretacionRegistros.sinResultado(consumo);
        }
    }

    private InterpretacionRegistros traducir(RespuestaJson leida, PromptWhatsappRegistros.Pedido pedido,
                                             ConsumoIaSami consumo) {
        BloqueId bloqueId = leida.tabla() == null ? null : pedido.tablas().get(leida.tabla());
        if (bloqueId == null || leida.filas() == null) {
            return InterpretacionRegistros.sinResultado(consumo);
        }
        List<Map<CampoId, String>> filas = new ArrayList<>();
        for (Map<String, String> fila : leida.filas()) {
            Map<CampoId, String> valores = new LinkedHashMap<>();
            fila.forEach((codigo, valor) -> {
                CampoId campoId = pedido.campos().get(codigo);
                if (campoId != null && bloqueId.equals(pedido.tablaDeCadaCampo().get(campoId))) {
                    valores.put(campoId, valor);
                }
            });
            filas.add(valores);
        }
        double confianza = leida.confianza() == null ? 0.0 : leida.confianza();
        return new InterpretacionRegistros(bloqueId, filas, confianza, consumo);
    }
}
