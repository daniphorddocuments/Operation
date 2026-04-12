package com.daniphord.mahanga.Service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class DocumentService {

    /**
     * Generate System Documentation PDF for Admin
     */
    public ByteArrayOutputStream generateSystemDocumentationPDF() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Title
        Paragraph title = new Paragraph("MAHANGA SYSTEM")
                .setFontSize(28)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(title);

        Paragraph subtitle = new Paragraph("Complete System Documentation")
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20)
                .setFontColor(ColorConstants.DARK_GRAY);
        document.add(subtitle);

        // Document Info
        Table infoTable = new Table(2).useAllAvailableWidth();
        infoTable.addCell(createHeaderCell("Document Type"));
        infoTable.addCell(createDataCell("System Administration Guide"));
        infoTable.addCell(createHeaderCell("Version"));
        infoTable.addCell(createDataCell("1.0.0"));
        infoTable.addCell(createHeaderCell("Date Generated"));
        infoTable.addCell(createDataCell(java.time.LocalDate.now().toString()));
        infoTable.addCell(createHeaderCell("Status"));
        infoTable.addCell(createDataCell("Production Ready"));
        document.add(infoTable);

        document.add(new Paragraph("\n"));

        // System Overview
        document.add(new Paragraph("1. SYSTEM OVERVIEW").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph(
                "The Mahanga System is an enterprise-grade multi-tenant platform designed for managing " +
                "shops, products, sales, and financial operations. It provides comprehensive tools for " +
                "system administrators, shop managers, accountants, and staff members."
        ).setMarginBottom(10));

        // Features
        document.add(new Paragraph("2. KEY FEATURES").setBold().setFontSize(14).setMarginTop(10));
        String[] features = {
                "Multi-tenant architecture with role-based access control",
                "Complete shop and product management system",
                "Sales tracking with real-time reporting",
                "Advanced financial management and reporting",
                "User authentication and authorization",
                "Comprehensive audit logging and tracking",
                "Security-first design with input validation",
                "Rate limiting and brute-force protection"
        };
        for (String feature : features) {
            document.add(new Paragraph("• " + feature).setMarginLeft(15).setMarginBottom(5));
        }

        // User Roles
        document.add(new Paragraph("3. USER ROLES & RESPONSIBILITIES").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("3.1 Administrator").setBold().setMarginLeft(10));
        document.add(new Paragraph(
                "Manages the entire system including shops, users, and system configuration. " +
                "Has access to all administrative functions and documentation."
        ).setMarginLeft(15).setMarginBottom(10));

        document.add(new Paragraph("3.2 Manager").setBold().setMarginLeft(10));
        document.add(new Paragraph(
                "Manages shop operations, products, and staff. Can view reports and manage daily operations."
        ).setMarginLeft(15).setMarginBottom(10));

        document.add(new Paragraph("3.3 Accountant").setBold().setMarginLeft(10));
        document.add(new Paragraph(
                "Handles financial operations, generates reports, and manages accounting functions."
        ).setMarginLeft(15).setMarginBottom(10));

        // Security
        document.add(new Paragraph("4. SECURITY FEATURES").setBold().setFontSize(14).setMarginTop(10));
        String[] security = {
                "End-to-end encryption for sensitive data",
                "Role-based access control (RBAC)",
                "Audit logging for all actions",
                "Rate limiting on authentication endpoints",
                "Account lockout after failed attempts",
                "CSRF protection",
                "Input validation and sanitization",
                "Secure password hashing with bcrypt"
        };
        for (String sec : security) {
            document.add(new Paragraph("• " + sec).setMarginLeft(15).setMarginBottom(5));
        }

        // Admin Functions
        document.add(new Paragraph("5. ADMIN DASHBOARD FUNCTIONS").setBold().setFontSize(14).setMarginTop(10));
        String[] adminFunctions = {
                "View system statistics and metrics",
                "Manage shops - Create, update, and delete shops",
                "Manage users - Create, update, and deactivate users",
                "Monitor system activity and alerts",
                "Access audit logs",
                "Download system documentation",
                "View reports and analytics"
        };
        for (String func : adminFunctions) {
            document.add(new Paragraph("• " + func).setMarginLeft(15).setMarginBottom(5));
        }

        // Support
        document.add(new Paragraph("6. TECHNICAL SUPPORT").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("Email: nkurushidaniphord@gmail.com").setMarginLeft(15));
        document.add(new Paragraph("Phone: +255 679 299 258").setMarginLeft(15));
        document.add(new Paragraph("WhatsApp: Available").setMarginLeft(15));

        document.close();
        return baos;
    }

    /**
     * Generate User Manual PDF for Managers
     */
    public ByteArrayOutputStream generateManagerUserManualPDF() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Title
        Paragraph title = new Paragraph("MANAGER USER MANUAL")
                .setFontSize(28)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(title);

        Paragraph subtitle = new Paragraph("Complete Guide to Shop Management")
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20)
                .setFontColor(ColorConstants.DARK_GRAY);
        document.add(subtitle);

        // Getting Started
        document.add(new Paragraph("1. GETTING STARTED").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("1.1 Accessing the Dashboard").setBold().setMarginLeft(10).setMarginTop(5));
        document.add(new Paragraph(
                "Log in with your manager credentials. You will be redirected to your shop dashboard " +
                "where you can see an overview of your shop's performance, sales, and employees."
        ).setMarginLeft(15).setMarginBottom(10));

        // Shop Management
        document.add(new Paragraph("2. SHOP MANAGEMENT").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("2.1 Dashboard Overview").setBold().setMarginLeft(10));
        String[] dashboardItems = {
                "Shop Statistics - View sales, products, and employee information",
                "Today's Sales - Real-time sales data for the current day",
                "Products Overview - See inventory levels and product performance",
                "Staff Management - View and manage employees"
        };
        for (String item : dashboardItems) {
            document.add(new Paragraph("• " + item).setMarginLeft(15).setMarginBottom(5));
        }

        // Product Management
        document.add(new Paragraph("3. PRODUCT MANAGEMENT").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("3.1 Adding Products").setBold().setMarginLeft(10).setMarginTop(5));
        String[] addSteps = {
                "Click 'Add Product' button",
                "Enter product name, category, and price",
                "Set initial quantity in stock",
                "Click 'Save' to confirm"
        };
        for (int i = 0; i < addSteps.length; i++) {
            document.add(new Paragraph((i + 1) + ". " + addSteps[i]).setMarginLeft(15).setMarginBottom(5));
        }

        document.add(new Paragraph("3.2 Updating Products").setBold().setMarginLeft(10).setMarginTop(10));
        document.add(new Paragraph(
                "Click the product name from the product list, make necessary changes, and save. " +
                "You can update prices, quantities, and descriptions at any time."
        ).setMarginLeft(15).setMarginBottom(10));

        // Sales Management
        document.add(new Paragraph("4. SALES MANAGEMENT").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("4.1 Recording Sales").setBold().setMarginLeft(10).setMarginTop(5));
        String[] salesSteps = {
                "Click on 'New Sale' button",
                "Select customer (if existing) or create new customer",
                "Add products to the sale",
                "Set quantities and verify prices",
                "Review total amount",
                "Process payment and confirm sale"
        };
        for (int i = 0; i < salesSteps.length; i++) {
            document.add(new Paragraph((i + 1) + ". " + salesSteps[i]).setMarginLeft(15).setMarginBottom(5));
        }

        // Staff Management
        document.add(new Paragraph("5. STAFF MANAGEMENT").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("5.1 Managing Employees").setBold().setMarginLeft(10).setMarginTop(5));
        document.add(new Paragraph(
                "View a list of all employees assigned to your shop. You can view their details and performance metrics."
        ).setMarginLeft(15).setMarginBottom(10));

        // Reports
        document.add(new Paragraph("6. REPORTS & ANALYTICS").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("6.1 Viewing Reports").setBold().setMarginLeft(10).setMarginTop(5));
        String[] reports = {
                "Daily Sales Report - Summary of sales for a specific day",
                "Monthly Performance - Monthly statistics and trends",
                "Product Performance - Top-selling products and inventory status",
                "Employee Performance - Sales and activity metrics per employee"
        };
        for (String report : reports) {
            document.add(new Paragraph("• " + report).setMarginLeft(15).setMarginBottom(5));
        }

        // Tips & Best Practices
        document.add(new Paragraph("7. TIPS & BEST PRACTICES").setBold().setFontSize(14).setMarginTop(10));
        String[] tips = {
                "Update inventory regularly to maintain accurate stock levels",
                "Review daily sales reports to monitor shop performance",
                "Keep customer information up to date",
                "Communicate with your team about pricing changes",
                "Monitor top-performing products",
                "Address stock-out items promptly"
        };
        for (String tip : tips) {
            document.add(new Paragraph("• " + tip).setMarginLeft(15).setMarginBottom(5));
        }

        // Troubleshooting
        document.add(new Paragraph("8. TROUBLESHOOTING").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("Q: I cannot log in to my dashboard").setBold().setMarginLeft(10).setMarginTop(5));
        document.add(new Paragraph(
                "A: Verify your username and password. If you've forgotten your password, use the 'Forgot Password' link. " +
                "Contact support if the issue persists."
        ).setMarginLeft(15).setMarginBottom(10));

        document.add(new Paragraph("Q: How do I record a product return?").setBold().setMarginLeft(10));
        document.add(new Paragraph(
                "A: Create a negative sale transaction with the returned products to adjust inventory."
        ).setMarginLeft(15).setMarginBottom(10));

        // Support
        document.add(new Paragraph("9. SUPPORT & CONTACT").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("For questions or technical issues, contact:").setMarginLeft(15).setMarginBottom(5));
        document.add(new Paragraph("Email: nkurushidaniphord@gmail.com").setMarginLeft(15));
        document.add(new Paragraph("Phone: +255 679 299 258").setMarginLeft(15));
        document.add(new Paragraph("WhatsApp: Available for urgent matters").setMarginLeft(15));

        document.close();
        return baos;
    }

    /**
     * Generate User Manual PDF for Accountants
     */
    public ByteArrayOutputStream generateAccountantUserManualPDF() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Title
        Paragraph title = new Paragraph("ACCOUNTANT USER MANUAL")
                .setFontSize(28)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(title);

        Paragraph subtitle = new Paragraph("Financial Management & Reporting Guide")
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20)
                .setFontColor(ColorConstants.DARK_GRAY);
        document.add(subtitle);

        // Getting Started
        document.add(new Paragraph("1. GETTING STARTED").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("1.1 Dashboard Access").setBold().setMarginLeft(10).setMarginTop(5));
        document.add(new Paragraph(
                "Log in with your accountant credentials to access the accounting dashboard. " +
                "Here you'll find financial summaries, reports, and accounting tools."
        ).setMarginLeft(15).setMarginBottom(10));

        // Financial Overview
        document.add(new Paragraph("2. FINANCIAL OVERVIEW").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("2.1 Dashboard Metrics").setBold().setMarginLeft(10).setMarginTop(5));
        String[] metrics = {
                "Total Revenue - Sum of all sales across shops",
                "Total Expenses - Sum of all recorded expenses",
                "Outstanding Payments - Pending invoices and receivables",
                "Loan Summary - Current loans and payment status"
        };
        for (String metric : metrics) {
            document.add(new Paragraph("• " + metric).setMarginLeft(15).setMarginBottom(5));
        }

        // Financial Reports
        document.add(new Paragraph("3. FINANCIAL REPORTS").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("3.1 Report Types").setBold().setMarginLeft(10).setMarginTop(5));
        document.add(new Paragraph("Revenue Report").setBold().setMarginLeft(15).setMarginTop(5));
        document.add(new Paragraph(
                "Detailed breakdown of revenue by shop, product, and time period. " +
                "Use filters to customize your analysis."
        ).setMarginLeft(25).setMarginBottom(10));

        document.add(new Paragraph("Expense Report").setBold().setMarginLeft(15));
        document.add(new Paragraph(
                "Track all expenses including operational costs, salaries, and other expenditures."
        ).setMarginLeft(25).setMarginBottom(10));

        document.add(new Paragraph("Profit & Loss Report").setBold().setMarginLeft(15));
        document.add(new Paragraph(
                "Comprehensive P&L statement showing revenue, expenses, and net profit."
        ).setMarginLeft(25).setMarginBottom(10));

        document.add(new Paragraph("Invoice Report").setBold().setMarginLeft(15));
        document.add(new Paragraph(
                "Track issued invoices, payments received, and outstanding amounts."
        ).setMarginLeft(25).setMarginBottom(10));

        // Loan Management
        document.add(new Paragraph("4. LOAN MANAGEMENT").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("4.1 Managing Shop Loans").setBold().setMarginLeft(10).setMarginTop(5));
        String[] loanSteps = {
                "View all active loans in the Loan Management section",
                "Track repayment schedules and outstanding balances",
                "Record loan payments when received",
                "Generate loan statements for shops"
        };
        for (int i = 0; i < loanSteps.length; i++) {
            document.add(new Paragraph((i + 1) + ". " + loanSteps[i]).setMarginLeft(15).setMarginBottom(5));
        }

        // Payment Processing
        document.add(new Paragraph("5. PAYMENT PROCESSING").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("5.1 Recording Payments").setBold().setMarginLeft(10).setMarginTop(5));
        String[] paymentSteps = {
                "Click 'Record Payment'",
                "Select payment type (Shop fees, Loan repayment, etc.)",
                "Enter amount and payment method",
                "Add payment date and reference",
                "Confirm and save"
        };
        for (int i = 0; i < paymentSteps.length; i++) {
            document.add(new Paragraph((i + 1) + ". " + paymentSteps[i]).setMarginLeft(15).setMarginBottom(5));
        }

        // Month-end Closing
        document.add(new Paragraph("6. MONTH-END PROCEDURES").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("6.1 Closing the Month").setBold().setMarginLeft(10).setMarginTop(5));
        String[] monthEnd = {
                "Verify all transactions are recorded",
                "Reconcile shop accounts with submitted reports",
                "Process month-end adjustments",
                "Generate final P&L statement",
                "Prepare billing for next month",
                "Archive month's data"
        };
        for (int i = 0; i < monthEnd.length; i++) {
            document.add(new Paragraph((i + 1) + ". " + monthEnd[i]).setMarginLeft(15).setMarginBottom(5));
        }

        // Tax & Compliance
        document.add(new Paragraph("7. TAX & COMPLIANCE").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("7.1 Maintaining Records").setBold().setMarginLeft(10).setMarginTop(5));
        document.add(new Paragraph(
                "The system automatically maintains detailed records of all financial transactions. " +
                "All records are audit-logged and timestamped for compliance purposes."
        ).setMarginLeft(15).setMarginBottom(10));

        // Data Analysis
        document.add(new Paragraph("8. FINANCIAL ANALYSIS").setBold().setFontSize(14).setMarginTop(10));
        String[] analysis = {
                "Trend Analysis - Compare performance across periods",
                "Shop Comparison - Identify top and underperforming shops",
                "Revenue Breakdown - Analyze revenue by product and shop",
                "Cost Analysis - Understand operational expenses"
        };
        for (String item : analysis) {
            document.add(new Paragraph("• " + item).setMarginLeft(15).setMarginBottom(5));
        }

        // Best Practices
        document.add(new Paragraph("9. BEST PRACTICES").setBold().setFontSize(14).setMarginTop(10));
        String[] practices = {
                "Review daily financial summaries",
                "Reconcile accounts weekly",
                "Follow month-end procedures consistently",
                "Keep backup of important reports",
                "Monitor for unusual transactions",
                "Communicate with shop managers about their accounts"
        };
        for (String practice : practices) {
            document.add(new Paragraph("• " + practice).setMarginLeft(15).setMarginBottom(5));
        }

        // Support
        document.add(new Paragraph("10. SUPPORT & CONTACT").setBold().setFontSize(14).setMarginTop(10));
        document.add(new Paragraph("For questions or technical issues, contact:").setMarginLeft(15).setMarginBottom(5));
        document.add(new Paragraph("Email: nkurushidaniphord@gmail.com").setMarginLeft(15));
        document.add(new Paragraph("Phone: +255 679 299 258").setMarginLeft(15));

        document.close();
        return baos;
    }

    /**
     * Helper method to create header cells for tables
     */
    private static com.itextpdf.layout.element.Cell createHeaderCell(String text) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBorder(new SolidBorder(ColorConstants.DARK_GRAY, 1))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(8);
    }

    /**
     * Helper method to create data cells for tables
     */
    private static com.itextpdf.layout.element.Cell createDataCell(String text) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text))
                .setBorder(new SolidBorder(ColorConstants.DARK_GRAY, 1))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(8);
    }
}

