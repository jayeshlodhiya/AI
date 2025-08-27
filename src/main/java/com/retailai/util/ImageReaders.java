package com.retailai.util;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.awt.image.ColorConvertOp;
import java.io.*;
import java.util.Iterator;

public final class ImageReaders {
    static {
        // ensure plugins are discovered (esp. in shaded/fat jars)
        ImageIO.scanForPlugins();
    }

    /** Reads any common image (incl. CMYK/YCCK JPEG) and returns sRGB BufferedImage. */
    public static BufferedImage readAsSRGB(InputStream in) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(in)) {
            if (iis == null) throw new IOException("Cannot create ImageInputStream");
            Iterator<ImageReader> it = ImageIO.getImageReaders(iis);
            if (!it.hasNext()) throw new IOException("No ImageReader found for stream (unsupported type)");
            ImageReader reader = it.next();
            try {
                reader.setInput(iis, true, true);
                BufferedImage img = reader.read(0);
                return toSRGB(img);
            } finally {
                reader.dispose();
            }
        }
    }

    /** Convert any BufferedImage to 24-bit sRGB (TYPE_INT_RGB). */
    public static BufferedImage toSRGB(BufferedImage src) {
        ColorSpace cs = src.getColorModel().getColorSpace();
        boolean isSRGB = cs != null && cs.isCS_sRGB();
        if (isSRGB && src.getType() == BufferedImage.TYPE_INT_RGB) return src;

        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        ColorConvertOp op = new ColorConvertOp(null);
        op.filter(src, dst);
        return dst;
    }
}
