package com.ensambla.backend.infrastructure.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Claude con la API de Anthropic ({@code ensambla.ia.proveedor=anthropic}). Necesita la clave
 * ({@code ENSAMBLA_IA_API_KEY}). Reusa un cliente por clave y timeout, y los cierra al bajar la aplicación.
 */
public class ProveedorIaAnthropic implements ProveedorIa, AutoCloseable {

    static final String NOMBRE = "anthropic";
    static final String FALTA_CLAVE = "Para usar Claude de Anthropic falta la clave: ENSAMBLA_IA_API_KEY.";

    private record Conexion(String apiKey, Duration timeout) {
    }

    private final Function<AjustesIa, AnthropicClient> conectar;
    private final Map<Conexion, AnthropicClient> clientes = new ConcurrentHashMap<>();

    public ProveedorIaAnthropic() {
        this(ProveedorIaAnthropic::conectar);
    }

    /** Para probar sin red: {@code conectar} arma el cliente con los ajustes. */
    ProveedorIaAnthropic(Function<AjustesIa, AnthropicClient> conectar) {
        this.conectar = conectar;
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public ClienteModeloVision vision(AjustesIa ajustes) {
        return new ClienteModeloVisionAnthropic(cliente(ajustes), ajustes.modeloVision());
    }

    @Override
    public ClienteModeloTexto texto(AjustesIa ajustes, String modelo, int maxTokens) {
        return new ClienteModeloTextoAnthropic(cliente(ajustes), modelo, maxTokens);
    }

    @Override
    public ClienteModeloConHerramientas conHerramientas(AjustesIa ajustes, int maxTokens,
                                                        List<DefinicionHerramienta> herramientas) {
        return new ClienteModeloConHerramientasAnthropic(
                cliente(ajustes), ajustes.modeloRapido(), maxTokens, herramientas);
    }

    @Override
    public void close() {
        clientes.values().forEach(AnthropicClient::close);
        clientes.clear();
    }

    private AnthropicClient cliente(AjustesIa ajustes) {
        if (AjustesIa.vacio(ajustes.apiKey())) {
            throw new IllegalStateException(FALTA_CLAVE);
        }
        return clientes.computeIfAbsent(
                new Conexion(ajustes.apiKey(), ajustes.timeout()), conexion -> conectar.apply(ajustes));
    }

    static AnthropicClient conectar(AjustesIa ajustes) {
        return AnthropicOkHttpClient.builder()
                .apiKey(ajustes.apiKey())
                .timeout(ajustes.timeout())
                .build();
    }
}
