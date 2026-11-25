package br.mentoria.docvalidator.service;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_photo.fastNlMeansDenoising;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.stereotype.Service;

/**
 * ETAPA 2 - OpenCV: trata e melhora a imagem para facilitar a leitura do OCR.
 * Escala de cinza -> ampliação (se pequena) -> remoção de ruído -> binarização adaptativa.
 */
@Service
public class ImagemService {

    private static final int LARGURA_MINIMA = 1600;

    public BufferedImage preprocessar(BufferedImage original) {
        Mat colorida = null, cinza = null, ampliada = null, semRuido = null, binaria = null;
        BytePointer buffer = new BytePointer();
        try {
            colorida = imdecode(new Mat(paraPng(original)), IMREAD_COLOR);
            if (colorida == null || colorida.empty()) {
                throw new IllegalArgumentException("Não foi possível decodificar a imagem.");
            }

            cinza = new Mat();
            cvtColor(colorida, cinza, COLOR_BGR2GRAY);

            // Imagens pequenas rendem OCR ruim: amplia até ~1600px de largura.
            Mat base = cinza;
            if (cinza.cols() < LARGURA_MINIMA) {
                double fator = (double) LARGURA_MINIMA / cinza.cols();
                ampliada = new Mat();
                resize(cinza, ampliada, new Size(), fator, fator, INTER_CUBIC);
                base = ampliada;
            }

            semRuido = new Mat();
            fastNlMeansDenoising(base, semRuido, 10f, 7, 21);

            binaria = new Mat();
            adaptiveThreshold(semRuido, binaria, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 31, 15);

            imencode(".png", binaria, buffer);
            byte[] saida = new byte[(int) buffer.limit()];
            buffer.get(saida);
            return ImageIO.read(new ByteArrayInputStream(saida));
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao tratar a imagem", e);
        } finally {
            for (Mat m : new Mat[] {colorida, cinza, ampliada, semRuido, binaria}) {
                if (m != null) m.release();
            }
            buffer.deallocate();
        }
    }

    private static byte[] paraPng(BufferedImage img) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }
}
