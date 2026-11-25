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

/** ETAPA 4 (opção A) - IA: OpenAI Chat Completions com saída em JSON. */
@Component
@ConditionalOnProperty(name = "app.ia.provider", havingValue = "openai")
public class OpenAiExtrator implements ExtratorLlm {

    private final RestClient http;
    private final ObjectMapper mapper;
    private final String model;

    public OpenAiExtrator(ObjectMapper mapper,
                          @Value("${app.ia.openai.api-key}") String apiKey,
                          @Value("${app.ia.openai.model}") String model) {
        this.mapper = mapper;
        this.model = model;
        this.http = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public DadosDocumento extrair(String textoOcr) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", LlmSupport.SYSTEM),
                        Map.of("role", "user", "content", LlmSupport.prompt(textoOcr))));

        JsonNode resposta = http.post().uri("/chat/completions").body(body).retrieve().body(JsonNode.class);
        String conteudo = resposta.path("choices").path(0).path("message").path("content").asText();
        return LlmSupport.parse(mapper, conteudo);
    }
}
