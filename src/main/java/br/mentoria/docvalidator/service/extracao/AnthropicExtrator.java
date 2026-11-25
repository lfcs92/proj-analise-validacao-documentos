package br.mentoria.docvalidator.service.extracao;

import br.mentoria.docvalidator.model.DadosDocumento;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** ETAPA 4 (opção B) - IA: Claude (Anthropic Messages API). */
@Component
@ConditionalOnProperty(name = "app.ia.provider", havingValue = "anthropic")
public class AnthropicExtrator implements ExtratorLlm {

    private final RestClient http;
    private final ObjectMapper mapper;
    private final String model;

    public AnthropicExtrator(ObjectMapper mapper,
                             @Value("${app.ia.anthropic.api-key}") String apiKey,
                             @Value("${app.ia.anthropic.model}") String model) {
        this.mapper = mapper;
        this.model = model;
        this.http = RestClient.builder()
                .baseUrl("https://api.anthropic.com/v1")
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    @Override
    public DadosDocumento extrair(String textoOcr) {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 1024,
                "temperature", 0,
                "system", LlmSupport.SYSTEM,
                "messages", List.of(Map.of("role", "user", "content", LlmSupport.prompt(textoOcr))));

        JsonNode resposta = http.post().uri("/messages").body(body).retrieve().body(JsonNode.class);
        String conteudo = resposta.path("content").path(0).path("text").asText();
        return LlmSupport.parse(mapper, conteudo);
    }
}
