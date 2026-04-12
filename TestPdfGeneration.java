package com.daniphord.mahanga;

import com.daniphord.mahanga.Service.DocumentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.FileOutputStream;

@SpringBootApplication
public class TestPdfGeneration {

    public static void main(String[] args) {
        SpringApplication.run(TestPdfGeneration.class, args);
    }

    @Bean
    public CommandLineRunner testPdf(DocumentService documentService) {
        return args -> {
            try {
                System.out.println("Testing PDF generation...");

                // Test system documentation PDF
                var systemPdf = documentService.generateSystemDocumentationPDF();
                try (FileOutputStream fos = new FileOutputStream("system_docs.pdf")) {
                    systemPdf.writeTo(fos);
                }
                System.out.println("System documentation PDF generated successfully!");

                // Test manager manual PDF
                var managerPdf = documentService.generateManagerUserManualPDF();
                try (FileOutputStream fos = new FileOutputStream("manager_manual.pdf")) {
                    managerPdf.writeTo(fos);
                }
                System.out.println("Manager manual PDF generated successfully!");

                // Test accountant manual PDF
                var accountantPdf = documentService.generateAccountantUserManualPDF();
                try (FileOutputStream fos = new FileOutputStream("accountant_manual.pdf")) {
                    accountantPdf.writeTo(fos);
                }
                System.out.println("Accountant manual PDF generated successfully!");

                System.out.println("All PDF generation tests passed!");
                System.exit(0);

            } catch (Exception e) {
                System.err.println("PDF generation test failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        };
    }
}
