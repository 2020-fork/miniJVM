/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.mini.gui.GToolkit;
import org.mini.nk.NK;
import static org.mini.nk.NK.stbtt_GetCodepointBitmapBox;
import static org.mini.nk.NK.stbtt_GetCodepointHMetrics;
import static org.mini.nk.NK.stbtt_GetCodepointKernAdvance;
import static org.mini.nk.NK.stbtt_InitFont;
import static org.mini.nk.NK.stbtt_ScaleForPixelHeight;

/**
 *
 * @author gust
 */
public class TrueTypeFont {

    static {
        System.setProperty("java.library.path", "../../jni_gui/cmake-build-debug/");
        System.loadLibrary("gui");
    }

    public static void main(String[] args) {
        TrueTypeFont gt = new TrueTypeFont();
        gt.t1();

    }

    void t1() {
        /* load font file */
        int size;
        byte[] fontBuffer;

        File fontFile = new File("./wqymhei.ttc");
        //    FILE* fontFile = fopen("../font/cmunrm.ttf", "rb");
        size = (int) fontFile.length();
        /* how long is the file ? */

        fontBuffer = new byte[size];
        FileInputStream fis;
        try {
            fis = new FileInputStream(fontFile);
            fis.read(fontBuffer, 0, size);
            fis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        /* prepare font */
        byte[] info = NK.stbtt_MakeFontInfo();
        long infoPtr = GToolkit.getArrayDataPtr(info);
        if (stbtt_InitFont(infoPtr, fontBuffer, 0) != 0) {
            System.out.println("failed\n");
        }

        int b_w = 2560;
        /* bitmap width */
        int b_h = 128;
        /* bitmap height */
        int l_h = 64;
        /* line height */
 /* create a bitmap for the phrase */
        byte[] bitmap = new byte[b_w * b_h];
        long bitmapPtr = GToolkit.getArrayDataPtr(bitmap);
        /* calculate font scaling */
        float scale = stbtt_ScaleForPixelHeight(infoPtr, l_h);
        byte[] word = "hello how are you?".getBytes();

        int x = 0;

        int[] ascent = {0}, descent = {0}, lineGap = {0};
        NK.stbtt_GetFontVMetrics(infoPtr, ascent, descent, lineGap);

        ascent[0] *= scale;
        descent[0] *= scale;

        int i;
        for (i = 0; i < word.length; ++i) {
            int ch = 24352;//word[i];
            int nch = 24352;//word[i + 1];
            /* get bounding box for character (may be offset to account for chars that dip above or below the line */
            int[] c_x1 = {0}, c_y1 = {0}, c_x2 = {0}, c_y2 = {0};
            stbtt_GetCodepointBitmapBox(infoPtr, ch, scale, scale, c_x1, c_y1, c_x2, c_y2);

            /* compute y (different characters have different heights */
            int y = ascent[0] + c_y1[0];

            /* render character (stride and offset is important here) */
            int byteOffset = x + (y * b_w);
            NK.stbtt_MakeCodepointBitmap(infoPtr, bitmap, c_x2[0] - c_x1[0], c_y2[0] - c_y1[0], b_w, scale, scale, ch);

            /* how wide is this character */
            int[] ax = {0}, bx = {0};
            stbtt_GetCodepointHMetrics(infoPtr, ch, ax, bx);
            x += ax[0] * scale;

            /* add kerning */
            int kern;
            kern = stbtt_GetCodepointKernAdvance(infoPtr, ch, nch);
            x += kern * scale;
        }

        /* save out a 1 channel image */
        NK.stbi_write_png("./out.png\000".getBytes(), b_w, b_h, 1, bitmapPtr, b_w);

    }
}
