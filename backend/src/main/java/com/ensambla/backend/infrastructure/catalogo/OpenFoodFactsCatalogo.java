package com.ensambla.backend.infrastructure.catalogo;

import com.ensambla.backend.domain.exception.CatalogoNoDisponibleException;
import com.ensambla.backend.domain.model.catalogo.CodigoBarras;
import com.ensambla.backend.domain.model.catalogo.ProductoCatalogo;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class OpenFoodFactsCatalogo {

    private final RestClient restClient;
    private final Duration vigencia;
    private final int limitePorMinuto;
    private final Map<String, Entrada> cache = new LinkedHashMap<>();
    private final Deque<Instant> consultas = new ArrayDeque<>();

    public OpenFoodFactsCatalogo(RestClient restClient, Duration vigencia, int limitePorMinuto) {
        this.restClient = restClient;
        this.vigencia = vigencia;
        this.limitePorMinuto = limitePorMinuto;
    }

    public synchronized Optional<ProductoCatalogo> buscar(CodigoBarras codigo, Instant instante) {
        Entrada anterior = cache.get(codigo.identidad());
        if (anterior != null && instante.isBefore(anterior.vence())) {
            return anterior.producto();
        }
        while (!consultas.isEmpty() && !consultas.peekFirst().isAfter(instante.minusSeconds(60))) {
            consultas.removeFirst();
        }
        if (consultas.size() >= limitePorMinuto) {
            throw new CatalogoNoDisponibleException();
        }
        consultas.addLast(instante);
        Optional<ProductoCatalogo> producto = consultar(codigo);
        cache.remove(codigo.identidad());
        if (cache.size() >= 1000) {
            cache.remove(cache.keySet().iterator().next());
        }
        cache.put(codigo.identidad(), new Entrada(producto, instante.plus(vigencia)));
        return producto;
    }

    private Optional<ProductoCatalogo> consultar(CodigoBarras codigo) {
        try {
            String cuerpo = restClient.get()
                    .uri("/api/v3/product/{codigo}?product_type=all&lc=es&fields="
                            + "code,product_name,product_name_es,brands,quantity,image_front_url", codigo.valor())
                    .retrieve().body(String.class);
            JsonNode respuesta = JsonMapper.builder().build().readTree(cuerpo);
            JsonNode producto = respuesta.path("product");
            if (!producto.isObject()) {
                throw new CatalogoNoDisponibleException();
            }
            if (!new CodigoBarras(producto.path("code").asString()).identidad().equals(codigo.identidad())) {
                throw new CatalogoNoDisponibleException();
            }
            String nombre = producto.path("product_name_es").asString("").strip();
            if (nombre.isEmpty()) {
                nombre = producto.path("product_name").asString("").strip();
            }
            String imagen = producto.path("image_front_url").asString("");
            return Optional.of(new ProductoCatalogo(codigo.valor(), nombre, producto.path("brands").asString(""),
                    producto.path("quantity").asString(""), imagen.matches(
                            "https://images\\.open(food|beauty|products)facts\\.org/[^\\s]+") ? imagen : null));
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (RuntimeException exception) {
            throw new CatalogoNoDisponibleException();
        }
    }

    private record Entrada(Optional<ProductoCatalogo> producto, Instant vence) {
    }
}
