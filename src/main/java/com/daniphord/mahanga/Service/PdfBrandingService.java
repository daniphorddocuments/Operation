package com.daniphord.mahanga.Service;

import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class PdfBrandingService {

    private volatile String fireLogoDataUri;
    private volatile String tanzaniaLogoDataUri;

    public PdfBrandingService() {
        this.fireLogoDataUri = dataUri("static/images/fire-rescue-force-logo.png");
        this.tanzaniaLogoDataUri = dataUri("static/images/tanzania-logo.png");
    }

    public byte[] generatePdf(String reportTitle, String bodyHtml) {
        return generatePdf(reportTitle, bodyHtml, "en");
    }

    public byte[] generatePdf(String reportTitle, String bodyHtml, String lang) {
        String normalizedLang = "sw".equalsIgnoreCase(lang) ? "sw" : "en";
        String nation = "sw".equals(normalizedLang) ? "JAMHURI YA MUUNGANO WA TANZANIA" : "THE UNITED REPUBLIC OF TANZANIA";
        String ministry = "sw".equals(normalizedLang) ? "WIZARA YA MAMBO YA NDANI YA NCHI" : "MINISTRY OF HOME AFFAIRS";
        String agency = "sw".equals(normalizedLang) ? "JESHI LA ZIMAMOTO NA UOKOAJI" : "FIRE AND RESCUE FORCE";
        String html = """
                <html>
                <head>
                    <style>
                        @page { margin: 44px 42px 50px 42px; }
                        body {
                            font-family: Arial, Helvetica, sans-serif;
                            font-size: 12px;
                            color: #111111;
                            line-height: 1.55;
                        }
                        .report-shell { width: 100%%; }
                        .report-header { width: 100%%; border-bottom: 2px solid #000; padding-bottom: 14px; margin-bottom: 18px; }
                        .report-header table { width: 100%%; border-collapse: collapse; }
                        .report-header td { vertical-align: top; }
                        .report-logo-cell { width: 88px; }
                        .report-logo {
                            width: 74px;
                            height: 74px;
                            object-fit: contain;
                        }
                        .report-heading {
                            text-align: center;
                            padding: 0 10px;
                        }
                        .report-heading .nation {
                            font-size: 14px;
                            font-weight: 700;
                            letter-spacing: 0.04em;
                        }
                        .report-heading .ministry {
                            font-size: 13px;
                            font-weight: 700;
                            margin-top: 4px;
                        }
                        .report-heading .agency {
                            font-size: 12px;
                            font-weight: 700;
                            margin-top: 4px;
                        }
                        .report-title {
                            margin-top: 14px;
                            font-size: 15px;
                            font-weight: 700;
                            text-transform: uppercase;
                        }
                        h1, h2, h3 {
                            font-family: Arial, Helvetica, sans-serif;
                            color: #111111;
                            margin: 0 0 8px;
                        }
                        h2 {
                            margin-top: 18px;
                            font-size: 13px;
                            text-transform: uppercase;
                        }
                        p, li, td, th, div {
                            font-family: Arial, Helvetica, sans-serif;
                            font-size: 12px;
                        }
                        p, li {
                            text-align: justify;
                        }
                        .meta-table,
                        .data-table {
                            width: 100%%;
                            border-collapse: collapse;
                            margin-top: 10px;
                        }
                        .meta-table td {
                            padding: 6px 8px;
                            border: 1px solid #c9c9c9;
                        }
                        .data-table th,
                        .data-table td {
                            border: 1px solid #bdbdbd;
                            padding: 7px 8px;
                            vertical-align: top;
                        }
                        .data-table th {
                            background: #efefef;
                            text-align: left;
                        }
                        .section-card {
                            border: 1px solid #d7d7d7;
                            padding: 12px;
                            margin-top: 12px;
                        }
                        .diagram-grid {
                            width: 100%%;
                            border-collapse: separate;
                            border-spacing: 10px;
                            margin-top: 12px;
                        }
                        .diagram-cell {
                            border: 1px solid #c7c7c7;
                            background: #f8f8f8;
                            padding: 14px;
                            text-align: center;
                            font-weight: 700;
                        }
                        .muted {
                            color: #555555;
                        }
                        ul {
                            margin: 6px 0 0 18px;
                            padding: 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="report-shell">
                        <div class="report-header">
                            <table>
                                <tr>
                                    <td class="report-logo-cell"><img class="report-logo" src="%s" alt="Fire and Rescue Force logo"></td>
                                    <td class="report-heading">
                                        <div class="nation">%s</div>
                                        <div class="ministry">%s</div>
                                        <div class="agency">%s</div>
                                        <div class="report-title">%s</div>
                                    </td>
                                    <td class="report-logo-cell" style="text-align:right;"><img class="report-logo" src="%s" alt="Tanzania logo"></td>
                                </tr>
                            </table>
                        </div>
                        %s
                    </div>
                </body>
                </html>
                """.formatted(
                fireLogoDataUri,
                nation,
                ministry,
                agency,
                escape(reportTitle),
                tanzaniaLogoDataUri,
                bodyHtml
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);
        return outputStream.toByteArray();
    }

    public synchronized void updateSignature(String signatureText) {
        // Signature stamping is intentionally disabled for downloadable documents.
    }

    public synchronized void updateSignatureFooter(String signatureFooterBase64) {
        // Signature footer stamping is intentionally disabled for downloadable documents.
    }

    public synchronized void updateLogos(String fireLogoBase64, String tanzaniaLogoBase64) {
        if (fireLogoBase64 != null && !fireLogoBase64.isBlank()) {
            this.fireLogoDataUri = toDataUri(fireLogoBase64.trim());
        }
        if (tanzaniaLogoBase64 != null && !tanzaniaLogoBase64.isBlank()) {
            this.tanzaniaLogoDataUri = toDataUri(tanzaniaLogoBase64.trim());
        }
    }

    public Map<String, String> currentBranding() {
        Map<String, String> branding = new HashMap<>();
        branding.put("signatureText", "");
        branding.put("signatureFooterDataUri", "");
        branding.put("fireLogoDataUri", fireLogoDataUri == null ? "" : fireLogoDataUri);
        branding.put("tanzaniaLogoDataUri", tanzaniaLogoDataUri == null ? "" : tanzaniaLogoDataUri);
        return branding;
    }

    private String dataUri(String classpathLocation) {
        try (InputStream inputStream = new ClassPathResource(classpathLocation).getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException exception) {
            return "";
        }
    }

    private String toDataUri(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("data:")) {
            return trimmed;
        }
        return "data:image/png;base64," + trimmed;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
