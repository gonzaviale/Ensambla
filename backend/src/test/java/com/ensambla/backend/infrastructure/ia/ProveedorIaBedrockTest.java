package com.ensambla.backend.infrastructure.ia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProveedorIaBedrockTest {

    private static final AjustesIa AJUSTES = new AjustesIa("bedrock", null, "us-east-1", null, "rapido", "avanzado",
            "vision", Duration.ofSeconds(30));
    private static final List<DefinicionHerramienta> HERRAMIENTAS = List.of(new DefinicionHerramienta(
            "renombrar_sistema", "Renombra el sistema.", Map.of("type", "object", "properties", Map.of())));

    private final List<AjustesIa> conexiones = new ArrayList<>();
    private BedrockRuntimeClient cliente;
    private ProveedorIaBedrock proveedor;

    @BeforeEach
    void setUp() {
        cliente = mock(BedrockRuntimeClient.class);
        proveedor = new ProveedorIaBedrock(ajustes -> {
            conexiones.add(ajustes);
            return cliente;
        });
    }

    @Test
    @DisplayName("Arma las tres puertas con los clientes de Bedrock")
    void proveedorShouldBuildTheThreeBedrockClients() {
        ClienteModeloVision vision = proveedor.vision(AJUSTES);
        ClienteModeloTexto texto = proveedor.texto(AJUSTES, "avanzado", 700);
        ClienteModeloConHerramientas conHerramientas = proveedor.conHerramientas(AJUSTES, 900, HERRAMIENTAS);

        assertThat(proveedor.nombre()).isEqualTo("bedrock");
        assertThat(vision).isInstanceOf(ClienteModeloVisionBedrock.class);
        assertThat(texto).isInstanceOf(ClienteModeloTextoBedrock.class);
        assertThat(conHerramientas).isInstanceOf(ClienteModeloConHerramientasBedrock.class);
    }

    @Test
    @DisplayName("Reusa un cliente por región, perfil y timeout")
    void proveedorShouldReuseClientForTheSameConnection() {
        proveedor.vision(AJUSTES);
        proveedor.texto(AJUSTES, "rapido", 700);
        proveedor.texto(new AjustesIa("bedrock", null, "us-west-2", null, "rapido", null, null,
                Duration.ofSeconds(30)), "rapido", 700);

        assertThat(conexiones).extracting(AjustesIa::region).containsExactly("us-east-1", "us-west-2");
    }

    @Test
    @DisplayName("Al cerrar cierra los clientes que abrió")
    void closeShouldCloseOpenClients() {
        proveedor.vision(AJUSTES);

        proveedor.close();

        verify(cliente).close();
    }

    @Test
    @DisplayName("Conecta con el perfil de AWS indicado o con las credenciales por defecto, y la región por defecto")
    void conectarShouldBuildWithProfileOrDefaultCredentials() {
        try (BedrockRuntimeClient conPerfil = ProveedorIaBedrock.conectar(ajustes("us-east-1", "perfil-de-prueba"));
             BedrockRuntimeClient sinPerfil = ProveedorIaBedrock.conectar(ajustes(null, null));
             BedrockRuntimeClient perfilEnBlanco = ProveedorIaBedrock.conectar(ajustes("  ", "  "))) {
            assertThat(conPerfil.serviceClientConfiguration().region().id()).isEqualTo("us-east-1");
            assertThat(sinPerfil.serviceClientConfiguration().region().id()).isEqualTo("us-east-1");
            assertThat(perfilEnBlanco.serviceClientConfiguration().region().id()).isEqualTo("us-east-1");
        }
    }

    @Test
    @DisplayName("Sin la función de prueba, conecta de verdad (sin llamar todavía a la red)")
    void defaultConstructorShouldBuildRealClients() {
        try (ProveedorIaBedrock real = new ProveedorIaBedrock()) {
            assertThat(real.texto(AJUSTES, "rapido", 700)).isInstanceOf(ClienteModeloTextoBedrock.class);
        }
    }

    // --- helpers ---
    private AjustesIa ajustes(String region, String perfil) {
        return new AjustesIa("bedrock", null, region, perfil, "rapido", null, null, Duration.ofSeconds(5));
    }
}
