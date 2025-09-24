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
    private float lastLineY = Float.NaN;

    public PositionTextStripper() throws IOException {
        super();
        // Important: let PDFBOX sort symbols and restore gaps
        setSortByPosition(true);

        setWordSeparator(" ");
        setLineSeparator("\n");
        setAverageCharTolerance(0.30f);
        setSpacingTolerance(0.50f);
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) {
        if (textPositions == null || textPositions.isEmpty()) {
            return;
        }
        // coordinates of the first and last symbol as the boundaries of the block
        TextPosition first = textPositions.getFirst();
        TextPosition last = textPositions.getLast();

        float x = first.getXDirAdj();
        float y = first.getYDirAdj();
        float endX = last.getXDirAdj() + last.getWidthDirAdj();

        elements.add(new PageElement(text, x, y, endX));
        lastLineY = y;
    }

    @Override
    protected void writeLineSeparator() throws IOException {
        // Obviously add the transfer of the line as a separate element
        float y = Float.isNaN(lastLineY) ? 0f : lastLineY;
        elements.add(new PageElement("\n", 0f, y, 0f));
    }
}

