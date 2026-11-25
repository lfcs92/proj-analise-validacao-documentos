package br.mentoria.docvalidator.entity;

import br.mentoria.docvalidator.model.StatusValidacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "documentos_processados")
public class DocumentoProcessado {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private String nomeArquivo;

    @Enumerated(EnumType.STRING)
    private StatusValidacao status;

    private String nome;
    private String cpf;
    private LocalDate dataValidade;

    @Column(length = 20000)
    private String textoOcr;

    /** Lista de {@code RegraResultado} serializada em JSON. */
    @Column(length = 20000)
    private String regrasJson;

    private Instant criadoEm = Instant.now();

    // ---- getters / setters ----
    public UUID getId() { return id; }
    public String getNomeArquivo() { return nomeArquivo; }
    public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }
    public StatusValidacao getStatus() { return status; }
    public void setStatus(StatusValidacao status) { this.status = status; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public LocalDate getDataValidade() { return dataValidade; }
    public void setDataValidade(LocalDate dataValidade) { this.dataValidade = dataValidade; }
    public String getTextoOcr() { return textoOcr; }
    public void setTextoOcr(String textoOcr) { this.textoOcr = textoOcr; }
    public String getRegrasJson() { return regrasJson; }
    public void setRegrasJson(String regrasJson) { this.regrasJson = regrasJson; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
