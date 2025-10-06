package com.github.vadimmiheev.vectordocs.documentprocessor.ocr;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Slf4j
public class ImageExtractor extends PDFStreamEngine {
    private final List<ImageWithPosition> images = new ArrayList<>();
    private Matrix currentMatrix;
    private float pageHeight;

    @Override
    public void processPage(org.apache.pdfbox.pdmodel.PDPage page) throws IOException {
        // Store the page height before processing
        this.pageHeight = page.getMediaBox().getHeight();
        super.processPage(page);
    }

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        String operation = operator.getName();
        if ("cm".equals(operation)) {
            // keep the last matrix
            float a = ((COSNumber) operands.get(0)).floatValue();
            float b = ((COSNumber) operands.get(1)).floatValue();
            float c = ((COSNumber) operands.get(2)).floatValue();
            float d = ((COSNumber) operands.get(3)).floatValue();
            float e = ((COSNumber) operands.get(4)).floatValue();
            float f = ((COSNumber) operands.get(5)).floatValue();

            currentMatrix = new Matrix(a, b, c, d, e, f);
        }
        else if ("Do".equals(operation)) {
            COSName objectName = (COSName) operands.getFirst();
            PDXObject xobject = getResources().getXObject(objectName);
            if (xobject instanceof PDImageXObject image) {

                try {
                    if (currentMatrix != null) {
                        float x = currentMatrix.getTranslateX();
                        float y = currentMatrix.getTranslateY();
                        // Inverted y
                        float invertedY = pageHeight - y;

                        images.add(new ImageWithPosition(image.getImage(), x, invertedY));
                    } else {
                        // fallback
                        images.add(new ImageWithPosition(image.getImage(), 0, 0));
                    }
                } catch (IllegalArgumentException ex) {
                    log.warn("Failed to process PDF image. {}", ex.getMessage());
                }
            }
        } else {
            super.processOperator(operator, operands);
        }
    }
}

