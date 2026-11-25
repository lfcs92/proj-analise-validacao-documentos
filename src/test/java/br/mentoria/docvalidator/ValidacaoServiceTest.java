package br.mentoria.docvalidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.mentoria.docvalidator.model.DadosDocumento;
import br.mentoria.docvalidator.model.StatusValidacao;
import br.mentoria.docvalidator.service.CpfValidator;
import br.mentoria.docvalidator.service.ValidacaoService;
import br.mentoria.docvalidator.service.extracao.RegexExtrator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ValidacaoServiceTest {

    // "Hoje" fixo para o teste não quebrar com o passar do tempo.
    private static final Clock HOJE = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);
    private static final String CPF_OK = "529.982.247-25";

    private final ValidacaoService service = new ValidacaoService(HOJE);

    @Test
    void aprovaSemDataDeValidadeQuandoCpfENomeSaoValidos() {
        var r = service.validar(new DadosDocumento("João da Silva", CPF_OK, null));
        assertEquals(StatusValidacao.APROVADO, r.status());
    }

    @Test
    void aprovaComValidadeNoFuturo() {
        var r = service.validar(new DadosDocumento("João da Silva", CPF_OK, LocalDate.of(2030, 5, 10)));
        assertEquals(StatusValidacao.APROVADO, r.status());
    }

    @Test
    void aprovaNoProprioDiaDaValidade() {
        var r = service.validar(new DadosDocumento("João da Silva", CPF_OK, LocalDate.of(2026, 9, 1)));
        assertEquals(StatusValidacao.APROVADO, r.status());
    }

    @Test
    void reprovaDocumentoVencido() {
        var r = service.validar(new DadosDocumento("João da Silva", CPF_OK, LocalDate.of(2026, 8, 31)));
        assertEquals(StatusValidacao.REPROVADO, r.status());
    }

    @Test
    void reprovaCpfInvalido() {
        var r = service.validar(new DadosDocumento("João da Silva", "123.456.789-00", null));
        assertEquals(StatusValidacao.REPROVADO, r.status());
    }

    @Test
    void reprovaNomeSemSobrenome() {
        var r = service.validar(new DadosDocumento("João", CPF_OK, null));
        assertEquals(StatusValidacao.REPROVADO, r.status());
    }

    @Test
    void reprovaDocumentoVazio() {
        assertEquals(StatusValidacao.REPROVADO, service.validar(DadosDocumento.vazio()).status());
    }

    @Test
    void cpfValidator() {
        assertTrue(CpfValidator.valido(CPF_OK));
        assertFalse(CpfValidator.valido("123.456.789-00"));
        assertFalse(CpfValidator.valido("111.111.111-11"));
        assertFalse(CpfValidator.valido(null));
    }

    @Test
    void regexExtraiNomeCpfEValidade() {
        String ocr = """
                CARTEIRA NACIONAL DE HABILITAÇÃO
                Nome: João da Silva
                CPF: 529.982.247-25
                Validade: 10/05/2030
                """;
        DadosDocumento d = new RegexExtrator().extrair(ocr);
        assertEquals("João da Silva", d.nome());
        assertEquals(CPF_OK, d.cpf());
        assertEquals(LocalDate.of(2030, 5, 10), d.dataValidade());
    }

    @Test
    void regexLeNomeNaLinhaSeguinteESemValidade() {
        String ocr = """
                REGISTRO GERAL
                NOME
                MARIA APARECIDA SOUZA
                CPF 529.982.247-25
                """;
        DadosDocumento d = new RegexExtrator().extrair(ocr);
        assertEquals("MARIA APARECIDA SOUZA", d.nome());
        assertEquals(CPF_OK, d.cpf());
        assertNull(d.dataValidade());
    }
}
