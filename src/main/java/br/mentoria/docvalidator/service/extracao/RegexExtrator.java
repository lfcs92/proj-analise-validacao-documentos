package br.mentoria.docvalidator.service.extracao;

import br.mentoria.docvalidator.model.DadosDocumento;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Extração SEM IA: palavras-chave + Regex.
 * Funciona bem para poucos layouts previsíveis; serve também de plano B quando a IA falha.
 */
@Component
public class RegexExtrator implements ExtratorDados {

    private static final Pattern CPF = Pattern.compile("\\b(\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2})\\b");

    /** "Nome: João da Silva" (mesma linha). */
    private static final Pattern NOME_MESMA_LINHA = Pattern.compile(
            "(?im)^\\s*nome(?:\\s+(?:completo|do\\s+titular|do\\s+cliente))?\\s*[:\\-]\\s*(.+?)\\s*$");

    /** Rótulo "NOME" sozinho numa linha e o valor na linha seguinte (comum em RG/CNH). */
    private static final Pattern NOME_LINHA_SEGUINTE = Pattern.compile(
            "(?im)^\\s*nome(?:\\s+completo)?\\s*\\R\\s*(\\p{L}[\\p{L} .'’]+?)\\s*$");

    /** "Validade: 10/05/2030", "Data de validade 2030-05-10", "Válido até 10/05/2030". */
    private static final Pattern VALIDADE = Pattern.compile(
            "(?i)(?:data\\s+de\\s+)?(?:validade|v[aá]lid[oa]\\s+at[eé])\\s*[:\\-]?\\s*(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})");

    @Override
    public DadosDocumento extrair(String texto) {
        if (texto == null || texto.isBlank()) return DadosDocumento.vazio();

        String nome = primeiro(NOME_MESMA_LINHA, texto);
        if (nome == null) nome = primeiro(NOME_LINHA_SEGUINTE, texto);

        return new DadosDocumento(
                nome,
                primeiro(CPF, texto),
                DataParser.parse(primeiro(VALIDADE, texto)));
    }

    private static String primeiro(Pattern p, String texto) {
        Matcher m = p.matcher(texto);
        return m.find() ? m.group(1).trim() : null;
    }
}
