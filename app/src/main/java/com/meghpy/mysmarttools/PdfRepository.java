package com.meghpy.mysmarttools;

import android.graphics.Bitmap;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.rendering.ImageType;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.util.ArrayList;
import java.util.List;

public class PdfRepository {

    private float editorDpi = 160f; // Can still be used for rendering if needed

    public void setEditorDpi(float dpi) {
        this.editorDpi = dpi;
    }

    // Renders all pages of a PDF into Bitmaps
    public List<Bitmap> renderAllPages(PDDocument document) {
        List<Bitmap> bitmaps = new ArrayList<>();
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                Bitmap bmp = renderer.renderImageWithDPI(i, editorDpi, ImageType.ARGB);
                if (bmp != null) {
                    bitmaps.add(bmp.copy(Bitmap.Config.ARGB_8888, true));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmaps;
    }

    // Merge a drawing Bitmap onto a PDF page (full page)
    public void mergeDrawingOnPage(PDDocument document, int pageIndex, Bitmap drawing) throws Exception {
        PDPage page = document.getPage(pageIndex);
        PDRectangle pageSize = page.getMediaBox();
        PDImageXObject pdImage = LosslessFactory.createFromImage(document, drawing);

        try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            contentStream.drawImage(pdImage, 0, 0, pageSize.getWidth(), pageSize.getHeight());
        }
    }

    // Add text using **ALREADY CONVERTED** PDF units!
    public void addTextToPage(PDDocument document, int pageIndex,
                              String text, float x, float y, float size, float scale) throws Exception {

        PDPage page = document.getPage(pageIndex);
        PDRectangle pageSize = page.getMediaBox();

        float pdfX = x;
        float pdfY = y;

        float pdfSize = size * scale;

        try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true)) {
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, pdfSize);

            // Get font ascent for vertical offset
            float ascent = PDType1Font.HELVETICA_BOLD.getFontDescriptor().getAscent() / 1000f * pdfSize;

            contentStream.newLineAtOffset(pdfX, pdfY - ascent);
            contentStream.showText(text);
            contentStream.endText();
        }
    }




    // Add image using **ALREADY CONVERTED** PDF units!
    public void addImageToPage(PDDocument document, int pageIndex,
                               Bitmap bmp, float pdfX, float pdfY, float pdfWidth, float pdfHeight) throws Exception {
        PDPage page = document.getPage(pageIndex);
        PDImageXObject pdImage = LosslessFactory.createFromImage(document, bmp);

        try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true)) {
            contentStream.drawImage(pdImage, pdfX, pdfY, pdfWidth, pdfHeight);
        }
    }
}