package br.mentoria.docvalidator.service;

import br.mentoria.docvalidator.dto.DocumentoResponse;
import br.mentoria.docvalidator.entity.DocumentoProcessado;
import br.mentoria.docvalidator.model.DadosDocumento;
import br.mentoria.docvalidator.model.RegraResultado;
import br.mentoria.docvalidator.model.ResultadoValidacao;
import br.mentoria.docvalidator.model.StatusValidacao;
import br.mentoria.docvalidator.repository.DocumentoRepository;
import br.mentoria.docvalidator.service.extracao.ExtratorDados;
import br.mentoria.docvalidator.service.extracao.ExtratorLlm;
import br.mentoria.docvalidator.service.extracao.RegexExtrator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orquestra o fluxo completo:
 * Upload -> (PDF->imagem) -> OpenCV -> Tesseract -> IA/Regex -> Regras -> Banco -> Resultado.
 */
@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    private final ImagemService imagemService;
    private final OcrService ocrService;
    private final ValidacaoService validacaoService;
    private final DocumentoRepository repository;
    private final ObjectMapper mapper;
    private final RegexExtrator regexExtrator;
    private final ExtratorDados extratorPrincipal;

    public DocumentoService(ImagemService imagemService, OcrService ocrService,
                            ValidacaoService validacaoService, DocumentoRepository repository,
                            ObjectMapper mapper, RegexExtrator regexExtrator,
                            ObjectProvider<ExtratorLlm> llm) {
        this.imagemService = imagemService;
        this.ocrService = ocrService;
        this.validacaoService = validacaoService;
        this.repository = repository;
        this.mapper = mapper;
        this.regexExtrator = regexExtrator;
        // Se um provedor de IA estiver configurado, ele é o principal; senão usa Regex.
        ExtratorLlm ia = llm.getIfAvailable();
        this.extratorPrincipal = ia != null ? ia : regexExtrator;
    }

    public DocumentoResponse processar(MultipartFile arquivo) {
        byte[] bytes = lerBytes(arquivo);
        String nomeArquivo = arquivo.getOriginalFilename() == null ? "documento" : arquivo.getOriginalFilename();

        // 1) Upload -> lista de imagens (um PDF pode ter várias páginas)
        List<BufferedImage> paginas = carregarImagens(bytes, nomeArquivo, arquivo.getContentType());

        // 2) OpenCV + 3) Tesseract
        StringBuilder texto = new StringBuilder();
        for (BufferedImage pagina : paginas) {
            texto.append(ocrService.extrairTexto(imagemService.preprocessar(pagina))).append('\n');
        }
        String textoOcr = texto.toString();
        log.info("OCR concluído para '{}': {} caracteres", nomeArquivo, textoOcr.length());

        // 4) IA (ou Regex)
        DadosDocumento dados = extrair(textoOcr);

        // 5) Regras de negócio
        ResultadoValidacao resultado = validacaoService.validar(dados);

        // 6) Persistência
        DocumentoProcessado salvo = repository.save(paraEntidade(nomeArquivo, textoOcr, dados, resultado));
        return paraResponse(salvo);
    }

    public DocumentoResponse buscar(UUID id) {
        return repository.findById(id).map(this::paraResponse)
                .orElseThrow(() -> new NoSuchElementException("Documento não encontrado: " + id));
    }

    public List<DocumentoResponse> listar(StatusValidacao status) {
        List<DocumentoProcessado> docs = status == null
                ? repository.findAllByOrderByCriadoEmDesc()
                : repository.findByStatusOrderByCriadoEmDesc(status);
        return docs.stream().map(this::paraResponse).toList();
    }

    // ------------------------------------------------------------------

    private DadosDocumento extrair(String textoOcr) {
        try {
            return extratorPrincipal.extrair(textoOcr);
        } catch (RuntimeException e) {
            if (extratorPrincipal == regexExtrator) throw e;
            log.warn("Falha na IA ({}). Usando extração por Regex como plano B.", e.getMessage());
            return regexExtrator.extrair(textoOcr);
        }
    }

    private List<BufferedImage> carregarImagens(byte[] bytes, String nome, String contentType) {
        boolean pdf = "application/pdf".equalsIgnoreCase(contentType) || nome.toLowerCase().endsWith(".pdf");
        if (pdf) return ocrService.pdfParaImagens(bytes);
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) throw new IllegalArgumentException("Formato não suportado. Envie PDF, PNG ou JPG.");
            return List.of(img);
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler a imagem enviada.", e);
        }
    }

    private static byte[] lerBytes(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) throw new IllegalArgumentException("Arquivo vazio.");
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Falha ao ler o arquivo.", e);
        }
    }

    private DocumentoProcessado paraEntidade(String arquivo, String textoOcr, DadosDocumento d, ResultadoValidacao r) {
        DocumentoProcessado e = new DocumentoProcessado();
        e.setNomeArquivo(arquivo);
        e.setTextoOcr(textoOcr.length() > 19000 ? textoOcr.substring(0, 19000) : textoOcr);
        e.setStatus(r.status());
        e.setNome(d.nome());
        e.setCpf(d.cpf());
        e.setDataValidade(d.dataValidade());
        try {
            e.setRegrasJson(mapper.writeValueAsString(r.regras()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
        return e;
    }

    private DocumentoResponse paraResponse(DocumentoProcessado e) {
        List<RegraResultado> regras = new ArrayList<>();
        try {
            if (e.getRegrasJson() != null) {
                regras = mapper.readValue(e.getRegrasJson(), new TypeReference<List<RegraResultado>>() {});
            }
        } catch (JsonProcessingException ex) {
            log.warn("Não foi possível ler as regras salvas do documento {}", e.getId());
        }
        DadosDocumento dados = new DadosDocumento(e.getNome(), e.getCpf(), e.getDataValidade());
        return new DocumentoResponse(e.getId(), e.getNomeArquivo(), e.getStatus(), dados, regras, e.getCriadoEm());
    }
}
