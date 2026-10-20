package com.ensambla.backend.infrastructure.ia;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Claude en Amazon Bedrock con la API Converse ({@code ensambla.ia.proveedor=bedrock}). Usa el perfil de AWS
 * indicado o, si no hay, la cadena de credenciales por defecto (variables {@code AWS_*}, rol de la instancia…). Reusa
 * un cliente por región, perfil y timeout, y los cierra al bajar la aplicación.
 */
public class ProveedorIaBedrock implements ProveedorIa, AutoCloseable {

    static final String NOMBRE = "bedrock";
    static final String REGION_POR_DEFECTO = "us-east-1";

    private record Conexion(String region, String perfilAws, Duration timeout) {
    }

    private final Function<AjustesIa, BedrockRuntimeClient> conectar;
    private final Map<Conexion, BedrockRuntimeClient> clientes = new ConcurrentHashMap<>();

    public ProveedorIaBedrock() {
        this(ProveedorIaBedrock::conectar);
    }

    /** Para probar sin red: {@code conectar} arma el cliente con los ajustes. */
    ProveedorIaBedrock(Function<AjustesIa, BedrockRuntimeClient> conectar) {
        this.conectar = conectar;
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public ClienteModeloVision vision(AjustesIa ajustes) {
        return new ClienteModeloVisionBedrock(cliente(ajustes), ajustes.modeloVision());
    }

    @Override
    public ClienteModeloTexto texto(AjustesIa ajustes, String modelo, int maxTokens) {
        return new ClienteModeloTextoBedrock(cliente(ajustes), modelo, maxTokens);
    }

    @Override
    public ClienteModeloConHerramientas conHerramientas(AjustesIa ajustes, int maxTokens,
                                                        List<DefinicionHerramienta> herramientas) {
        return new ClienteModeloConHerramientasBedrock(
                cliente(ajustes), ajustes.modeloRapido(), maxTokens, herramientas);
    }

    @Override
    public void close() {
        clientes.values().forEach(BedrockRuntimeClient::close);
        clientes.clear();
    }

    private BedrockRuntimeClient cliente(AjustesIa ajustes) {
        Conexion conexion = new Conexion(ajustes.region(), ajustes.perfilAws(), ajustes.timeout());
        return clientes.computeIfAbsent(conexion, clave -> conectar.apply(ajustes));
    }

    static BedrockRuntimeClient conectar(AjustesIa ajustes) {
        AwsCredentialsProvider credenciales = AjustesIa.vacio(ajustes.perfilAws())
                ? DefaultCredentialsProvider.builder().build()
                : ProfileCredentialsProvider.create(ajustes.perfilAws());
        return BedrockRuntimeClient.builder()
                .region(Region.of(AjustesIa.elegir(ajustes.region(), REGION_POR_DEFECTO)))
                .credentialsProvider(credenciales)
                .overrideConfiguration(config -> config.apiCallTimeout(ajustes.timeout()))
                .build();
    }
}
