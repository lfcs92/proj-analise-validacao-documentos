package br.mentoria.docvalidator.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** ETAPA 3 - Tesseract: transforma imagem em texto (não entende o significado, só lê caracteres). */
@Service
public class OcrService {

    private static final int MAX_PAGINAS_PDF = 3;

    private final String tessdataPath;
    private final String idioma;

    public OcrService(@Value("${app.ocr.tessdata-path}") String tessdataPath,
                      @Value("${app.ocr.language:por}") String idioma) {
        this.tessdataPath = tessdataPath;
        this.idioma = idioma;
    }

    public String extrairTexto(BufferedImage imagem) {
        // A instância do Tesseract NÃO é thread-safe: criamos uma por chamada.
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(idioma);
        tesseract.setPageSegMode(6); // bloco único de texto uniforme: bom p/ comprovantes
        try {
            return tesseract.doOCR(imagem);
        } catch (TesseractException e) {
            throw new IllegalStateException("Falha no OCR: " + e.getMessage(), e);
        }
    }

    /** Converte as primeiras páginas de um PDF em imagens (300 DPI). */
    public List<BufferedImage> pdfParaImagens(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int paginas = Math.min(doc.getNumberOfPages(), MAX_PAGINAS_PDF);
            List<BufferedImage> imagens = new ArrayList<>();
            for (int i = 0; i < paginas; i++) {
                imagens.add(renderer.renderImageWithDPI(i, 300, ImageType.RGB));
            }
            return imagens;
        } catch (IOException e) {
            throw new IllegalArgumentException("PDF inválido ou corrompido.", e);
        }
    }
}
