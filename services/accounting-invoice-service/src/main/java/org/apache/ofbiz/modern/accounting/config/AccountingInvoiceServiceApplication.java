package org.apache.ofbiz.modern.accounting.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.apache.ofbiz.modern.accounting")
public class AccountingInvoiceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountingInvoiceServiceApplication.class, args);
    }
}
