package com.codesec.domain.service.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;

/**
 * PdfReportTemplate — centralized font/color/page-event definitions.
 *
 * Separated from PdfExportService (SRP) so that:
 * - PDF visual appearance is configurable in one place
 * - Page events (footer, header) are isolated from rendering logic
 * - PdfExportService focuses on orchestration
 */
public final class PdfReportTemplate {

    // ======================== Fonts ========================
    public static final Font TITLE_FONT =
        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
    public static final Font SECTION_FONT =
        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.DARK_GRAY);
    public static final Font LABEL_FONT =
        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
    public static final Font VALUE_FONT =
        FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    public static final Font CODE_FONT =
        FontFactory.getFont(FontFactory.COURIER, 9, Font.NORMAL, Color.BLACK);
    public static final Font FOOTER_FONT =
        FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.GRAY);

    // ======================== Colors ========================
    public static final Color DARK_GRAY = Color.DARK_GRAY;
    public static final Color GRAY = Color.GRAY;
    public static final Color BLACK = Color.BLACK;

    private PdfReportTemplate() {} // utility class

    /** Creates a new A4 document with standard margins. */
    public static Document createDocument() {
        return new Document(PageSize.A4, 50, 50, 50, 50);
    }

    /** Returns a page event that renders "code-sec · Report · Page N" at the footer. */
    public static PdfPageEventHelper createPageEvent() {
        return new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter writer, Document document) {
                PdfContentByte cb = writer.getDirectContent();
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("code-sec · Security Audit Report · Page "
                        + writer.getCurrentPageNumber(), FOOTER_FONT),
                    document.getPageSize().getRight(50), 20, 0);
            }
        };
    }
}
