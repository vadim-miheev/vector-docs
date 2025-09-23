package com.github.vadimmiheev.vectordocs.documentprocessor.ocr;

import lombok.Getter;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
public class PositionTextStripper extends PDFTextStripper {
    private final List<PageElement> elements = new ArrayList<>();

    public PositionTextStripper() throws IOException {
        super();
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        elements.add(new PageElement(text.getUnicode(), text.getXDirAdj(), text.getYDirAdj()));
    }

}

