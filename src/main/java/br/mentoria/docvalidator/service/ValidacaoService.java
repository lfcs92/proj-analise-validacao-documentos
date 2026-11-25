package br.mentoria.docvalidator.service;

import br.mentoria.docvalidator.model.DadosDocumento;
import br.mentoria.docvalidator.model.RegraResultado;
import br.mentoria.docvalidator.model.ResultadoValidacao;
import br.mentoria.docvalidator.model.StatusValidacao;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * ETAPA 5 - Regras de negócio.
 * <ul>
 *   <li>Nome completo (nome e sobrenome) identificado;</li>
 *   <li>CPF válido (dígitos verificadores);</li>
 *   <li>Data de validade: só é verificada SE o documento tiver uma. Sem data de validade,
 *       a aprovação depende apenas do CPF e do nome completo.</li>
 * </ul>
 * Aprovado somente se TODAS as regras aplicáveis forem atendidas.
 */
@Service
public class ValidacaoService {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Clock clock;

    public ValidacaoService(Clock clock) {
        this.clock = clock;
    }

    public ResultadoValidacao validar(DadosDocumento d) {
        List<RegraResultado> regras = new ArrayList<>();

        boolean nome = d.nome() != null && d.nome().strip().split("\\s+").length >= 2;
        regras.add(new RegraResultado("Nome completo identificado", nome,
                nome ? "Nome: " + d.nome() : "Nome completo (nome e sobrenome) não encontrado"));

        boolean cpf = CpfValidator.valido(d.cpf());
        regras.add(new RegraResultado("CPF válido", cpf,
                cpf ? "CPF com dígitos verificadores corretos"
                    : d.cpf() == null ? "CPF não encontrado" : "CPF inválido: " + d.cpf()));

        LocalDate validade = d.dataValidade();
        if (validade == null) {
            regras.add(new RegraResultado("Data de validade", true,
                    "Documento sem data de validade: aprovado apenas por CPF e nome"));
        } else {
            LocalDate hoje = LocalDate.now(clock);
            boolean noPrazo = !validade.isBefore(hoje); // vale até o próprio dia da validade
            regras.add(new RegraResultado("Documento dentro da validade", noPrazo,
                    noPrazo ? "Válido até " + validade.format(BR) : "Documento vencido em " + validade.format(BR)));
        }

        boolean aprovado = regras.stream().allMatch(RegraResultado::atendida);
        return new ResultadoValidacao(aprovado ? StatusValidacao.APROVADO : StatusValidacao.REPROVADO, regras);
    }
}
