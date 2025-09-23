package com.github.vadimmiheev.vectordocs.documentprocessor.ocr;

import java.awt.image.BufferedImage;

public class ImageWithPosition {
    public BufferedImage image;
    public float x, y;

    public ImageWithPosition(BufferedImage image, float x, float y) {
        this.image = image;
        this.x = x;
        this.y = y;
    }
}