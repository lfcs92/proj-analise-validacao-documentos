package br.mentoria.docvalidator.service.extracao;

import br.mentoria.docvalidator.model.DadosDocumento;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Prompt e interpretação do JSON, compartilhados por todos os provedores de IA. */
final class LlmSupport {

    static final String SYSTEM = "Você extrai dados de documentos brasileiros a partir de texto de OCR. "
            + "Responda SOMENTE com um objeto JSON válido, sem markdown e sem explicações.";

    private static final String PROMPT = """
            Extraia as informações do documento abaixo.
            Retorne apenas os campos: nome (nome completo do titular), cpf e dataValidade.
            dataValidade é a data de VALIDADE do documento (não confunda com data de emissão ou de nascimento),
            no formato yyyy-MM-dd. O texto vem de OCR e pode conter pequenos erros de leitura.
            Se o documento não tiver um campo (por exemplo, não tem data de validade), retorne null.
            Não invente informações.

            TEXTO:
            %s
            """;

    private LlmSupport() {}

    static String prompt(String textoOcr) {
        return PROMPT.formatted(textoOcr);
    }

    static DadosDocumento parse(ObjectMapper mapper, String resposta) {
        try {
            String json = resposta.trim();
            // Alguns modelos devolvem ```json ... ``` mesmo quando pedimos para não fazer isso.
            int ini = json.indexOf('{');
            int fim = json.lastIndexOf('}');
            if (ini < 0 || fim < ini) throw new IllegalStateException("Resposta da IA sem JSON: " + resposta);
            JsonNode n = mapper.readTree(json.substring(ini, fim + 1));
            return new DadosDocumento(
                    texto(n, "nome"), texto(n, "cpf"),
                    DataParser.parse(texto(n, "dataValidade")));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("JSON inválido retornado pela IA", e);
        }
    }

    private static String texto(JsonNode n, String campo) {
        JsonNode v = n.get(campo);
        return (v == null || v.isNull() || v.asText().isBlank()) ? null : v.asText().trim();
    }
}
