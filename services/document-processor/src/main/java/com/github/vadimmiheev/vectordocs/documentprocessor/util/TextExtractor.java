package com.github.vadimmiheev.vectordocs.documentprocessor.util;

import com.github.vadimmiheev.vectordocs.documentprocessor.ocr.PageElement;
import java.util.List;

public class TextExtractor {

    private static final float LINE_THRESHOLD = 2.0f; // new line sensitivity
    private static final float GAP_THRESHOLD = 1.5f;  // horizontal gap for space

    public static String mergeElements(List<PageElement> elements) {
        StringBuilder merged = new StringBuilder();
        float lastY = Float.NaN;
        float lastEndX = Float.NaN;

        for (PageElement el : elements) {
            String nextText = el.text == null ? "" : el.text;

            boolean isNewLineByY = !Float.isNaN(lastY) && Math.abs(el.y - lastY) > LINE_THRESHOLD;
            boolean lastEndsWithNL = !merged.isEmpty() && (merged.charAt(merged.length() - 1) == '\n');

            if (isNewLineByY && !lastEndsWithNL) {
                merged.append('\n');
            } else if (!Float.isNaN(lastEndX)) {
                float gap = el.x - lastEndX;
                boolean needSpaceByGap = gap > GAP_THRESHOLD;
                boolean mergedEndsWithSpace = !merged.isEmpty() && Character.isWhitespace(merged.charAt(merged.length() - 1));
                boolean nextStartsWithPunct = !nextText.isEmpty() && isPunctuation(nextText.charAt(0));

                if (needSpaceByGap && !mergedEndsWithSpace && !nextStartsWithPunct) {
                    merged.append(' ');
                }
            }

            merged.append(nextText);
            lastEndX = Math.max(el.endX, el.x);
            lastY = el.y;
        }
        return merged.toString();
    }

    private static boolean isPunctuation(char c) {
        return ",.;:!?)]}%»".indexOf(c) >= 0;
    }
}