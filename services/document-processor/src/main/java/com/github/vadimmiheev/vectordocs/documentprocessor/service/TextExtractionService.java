package com.github.vadimmiheev.vectordocs.documentprocessor.service;

import com.github.vadimmiheev.vectordocs.documentprocessor.ocr.ImageExtractor;
import com.github.vadimmiheev.vectordocs.documentprocessor.ocr.ImageWithPosition;
import com.github.vadimmiheev.vectordocs.documentprocessor.ocr.PageElement;
import com.github.vadimmiheev.vectordocs.documentprocessor.ocr.PositionTextStripper;
import com.github.vadimmiheev.vectordocs.documentprocessor.util.TextExtractor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class TextExtractionService {

    @Value("${app.ocr.enabled:true}")
    private boolean ocrEnabled;

    @Value("${app.ocr.lang:rus+eng}")
    private String ocrLang;

    @Value("${app.ocr.datapath:}")
    private String ocrDataPath;

    @Value("${app.ocr.dpi:300}")
    private int ocrDpi;

    public String extractText(byte[] data, String contentType, String fileName) throws IOException, TesseractException {
        String type = contentType != null ? contentType.toLowerCase() : null;
        String lowerName = fileName != null ? fileName.toLowerCase() : "";

        if ((type != null && type.contains("pdf")) || lowerName.endsWith(".pdf")) {
            return extractFromPdf(data);
        }
        if ((type != null && (type.contains("text/plain") || type.startsWith("text/"))) || lowerName.endsWith(".txt")) {
            String text = new String(data, StandardCharsets.UTF_8);
            // CRLF & CR → LF
            text = text.replace("\r\n", "\n").replace("\r", "\n");
            return text;
        }
        throw new IOException("Unsupported content type: " + contentType + " for file " + fileName);
    }

    /**
     * Extracts textual and image-based content from a PDF file represented as a byte array.
     * The method combines text extracted directly from the PDF with OCR-processed text from embedded images,
     * assembling the results in reading order based on positional coordinates.
     *
     * @param data the byte array representation of the PDF file
     * @return a string containing all extracted content from the PDF, including text and OCR-processed data
     * @throws IOException if an error occurs while loading or processing the PDF file
     * @throws TesseractException if an error occurs during OCR processing
     */
    private String extractFromPdf(byte[] data) throws IOException, TesseractException {
        StringBuilder result = new StringBuilder();

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(data))) {
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(ocrDataPath);
            tesseract.setLanguage(ocrLang);

            // Ensure DPI is known to tesseract to improve accuracy
            tesseract.setVariable("user_defined_dpi", Integer.toString(Math.max(72, ocrDpi)));

            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                PDPage page = document.getPage(pageIndex);

                // Extract text objects
                PositionTextStripper stripper = new PositionTextStripper();
                stripper.setStartPage(pageIndex + 1);
                stripper.setEndPage(pageIndex + 1);
                stripper.getText(document); // It is necessary to call for launch processTextPosition()
                List<PageElement> elements = new ArrayList<>(stripper.getElements());

                // Extract images
                ImageExtractor extractor = new ImageExtractor();
                extractor.processPage(page);

                for (ImageWithPosition img : extractor.getImages()) {
                    String ocrResult = tesseract.doOCR(img.image);
                    elements.add(new PageElement(ocrResult, img.x, img.y));
                }

                // Sort by coordinates (from top to bottom, left to right)
                elements.sort(Comparator
                        .comparingDouble((PageElement e) -> e.y)    // from top to bottom
                        .thenComparingDouble(e -> e.x)); // from left to right

                result.append(TextExtractor.mergeElements(elements));
            }
        }

        return result.toString();
    }

    private static boolean isPunctuation(char c) {
        // Basic check for signs, before which the gap is usually not needed
        return ",.;:!?)]}%»".indexOf(c) >= 0;
    }
}
