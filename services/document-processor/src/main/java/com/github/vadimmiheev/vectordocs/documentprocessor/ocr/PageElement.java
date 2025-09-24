package com.github.vadimmiheev.vectordocs.documentprocessor.ocr;

public class PageElement {
    public String text;
    public float x, y;
    // The right boundary of the text block for evaluating horizontal clearances
    public float endX;

    public PageElement(String text, float x, float y) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.endX = x; // Compatibility: if we do not know the width
    }

    public PageElement(String text, float x, float y, float endX) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.endX = endX;
    }
}