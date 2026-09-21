package org.apache.ofbiz.modern.accounting.invoices.adapter.in.web;

import java.math.BigDecimal;
import org.apache.ofbiz.modern.accounting.invoices.application.port.in.InvoiceUseCases;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Search;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Summary;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InvoiceUiController {
    private static final String CELL = "</td><td>";
    private static final String NAV = "<a href='/accounting/control/findPayments'>Payments</a><a href='/accounting/control/findPaymentGroups'>Payment Groups</a><a href='/accounting/control/findFinAccountTrans'>Transactions</a><a href='/accounting/control/findBillingAccount'>Billing Accounts</a><a href='/accounting/control/FindFinAccount'>Financial Accounts</a><a href='/accounting/control/FindTaxAuthority'>Tax Authorities</a><a href='/accounting/control/FindAgreement'>Agreements</a><a href='/accounting/control/FindFixedAsset'>Fixed Assets</a><a href='/accounting/control/FindBudget'>Budgets</a><a href='/accounting/control/EditGlobalGlAccount'>GL Settings</a><a href='/accounting/control/FindOrganization'>Companies</a>";
    private final InvoiceUseCases service;
    public InvoiceUiController(InvoiceUseCases service) { this.service = service; }

    @GetMapping(value={"/modern/accounting/invoices", "/accounting/control/findInvoices"}, produces=MediaType.TEXT_HTML_VALUE)
    public String page(@RequestParam(required=false) String invoiceId, @RequestParam(required=false) String invoiceTypeId,
                       @RequestParam(required=false) String statusId, @RequestParam(required=false) String partyIdFrom,
                       @RequestParam(required=false) String partyId) {
        var page = service.search(new Search(invoiceId, invoiceTypeId, statusId, partyIdFrom, partyId, 50, 0));
        StringBuilder rows = new StringBuilder();
        page.invoices().forEach(invoice -> appendRow(rows, invoice));
        return """
          <!doctype html><html><head><meta charset='utf-8'><title>OFBiz Accounting Invoices</title><style>
          *{box-sizing:border-box}body{margin:0;font:14px Arial;color:#133d3b;background:#f6f8fa}header{height:64px;background:#1BC5BD;color:white;display:flex;align-items:center;padding:0 24px;font-size:24px;font-weight:bold}header span{background:white;color:#133d3b;padding:8px 14px;border-radius:4px;margin-right:18px}nav{background:#dcfffd;padding:12px 20px;display:flex;gap:16px;flex-wrap:wrap}a{color:#133d3b}main{margin:20px;background:white;padding:20px;border-radius:6px}form{display:flex;gap:10px;flex-wrap:wrap;margin-bottom:18px}input{padding:8px;border:1px solid #aac}button{background:#1BC5BD;border:0;padding:9px 18px;color:#133d3b;font-weight:bold}table{width:100%%;border-collapse:collapse}th,td{padding:9px;border-bottom:1px solid #dcfffd;text-align:left}.note{background:#dcfffd;padding:12px;margin-top:18px}
          </style></head><body><header><span>OFBiz</span>Accounting · Modern Invoice Search</header><nav>%s</nav><main><h1>Invoices</h1>
          <form method='get' action='/accounting/control/findInvoices'><input name='invoiceId' placeholder='Invoice ID' value='%s'><input name='invoiceTypeId' placeholder='Type' value='%s'><input name='statusId' placeholder='Status' value='%s'><input name='partyIdFrom' placeholder='From party' value='%s'><input name='partyId' placeholder='To party' value='%s'><button>Search</button></form>
          <table><thead><tr><th>Invoice</th><th>Type</th><th>Status</th><th>Parties</th><th>Total</th><th>Applied</th><th>Outstanding</th><th>Legacy</th></tr></thead><tbody>%s</tbody></table><p class='note'>%s</p></main></body></html>
          """.formatted(NAV, esc(invoiceId), esc(invoiceTypeId), esc(statusId), esc(partyIdFrom), esc(partyId), rows, page.totalsNote());
    }

    private static void appendRow(StringBuilder rows, Summary invoice) {
        String id=esc(invoice.invoiceId());
        rows.append("<tr><td><a href='/api/accounting/invoices/").append(id).append("'>").append(id).append("</a>").append(CELL).append(esc(invoice.invoiceTypeDescription())).append(CELL).append(esc(invoice.statusDescription())).append(CELL).append(esc(invoice.partyIdFrom())).append(" → ").append(esc(invoice.partyId())).append(CELL).append(money(invoice.invoiceTotal())).append(CELL).append(money(invoice.appliedPaymentTotal())).append(CELL).append(money(invoice.outstandingTotal())).append(CELL).append("<a href='/accounting/control/viewInvoice?invoiceId=").append(id).append("'>Legacy detail</a></td></tr>");
    }
    private static String money(BigDecimal value){return value==null?"0":value.toPlainString();}
    private static String esc(String value){return value==null?"":value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
}
