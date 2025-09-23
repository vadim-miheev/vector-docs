package com.github.vadimmiheev.vectordocs.documentprocessor.ocr;

public class PageElement {
    public String text;
    public float x, y;

    public PageElement(String text, float x, float y) {
        this.text = text;
        this.x = x;
        this.y = y;
    }
}