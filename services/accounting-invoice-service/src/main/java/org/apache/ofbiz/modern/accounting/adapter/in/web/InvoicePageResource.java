package org.apache.ofbiz.modern.accounting.adapter.in.web;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.ofbiz.modern.accounting.application.port.in.SearchInvoices;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
@Path("/modern/accounting/invoices")
@Produces(MediaType.TEXT_HTML)
public class InvoicePageResource {

    private static final String CONTENT_SECURITY_POLICY =
            "default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self'; "
                    + "base-uri 'none'; frame-ancestors 'none'; form-action 'self'";
    private static final String SEARCH_ACTION = "/accounting/control/findInvoices";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<NavigationLink> NAVIGATION = List.of(
            new NavigationLink("Invoices", SEARCH_ACTION),
            new NavigationLink("Payments", "/accounting/control/findPayments"),
            new NavigationLink("Payment Groups", "/accounting/control/FindPaymentGroup"),
            new NavigationLink("Transactions", "/accounting/control/FindGatewayResponses"),
            new NavigationLink("Billing Accounts", "/accounting/control/FindBillingAccount"),
            new NavigationLink("Financial Accounts", "/accounting/control/FinAccountMain"),
            new NavigationLink("Tax Authorities", "/accounting/control/FindTaxAuthority"),
            new NavigationLink("Agreements", "/accounting/control/FindAgreement"),
            new NavigationLink("Fixed Assets", "/accounting/control/ListFixedAssets"),
            new NavigationLink("Budgets", "/accounting/control/ListBudgets"),
            new NavigationLink("GL Settings", "/accounting/control/globalGLSettings"),
            new NavigationLink("Companies", "/accounting/control/ListCompanies"));

    private final SearchInvoices searchInvoices;

    public InvoicePageResource(SearchInvoices searchInvoices) {
        this.searchInvoices = searchInvoices;
    }

    @GET
    public Response page(
            @QueryParam("invoiceId") String invoiceId,
            @QueryParam("invoiceTypeId") String invoiceTypeId,
            @QueryParam("statusId") String statusId,
            @QueryParam("partyIdFrom") String partyIdFrom,
            @QueryParam("partyId") String partyId,
            @DefaultValue("25") @QueryParam("limit") int limit,
            @DefaultValue("0") @QueryParam("offset") int offset) {
        SearchInvoices.InvoiceSearchQuery query = new SearchInvoices.InvoiceSearchQuery(
                invoiceId, invoiceTypeId, statusId, partyIdFrom, partyId, limit, offset);
        SearchInvoices.InvoicePageView result = searchInvoices.search(query);
        String html = renderPage(query, result);
        return Response.ok(html, MediaType.TEXT_HTML_TYPE)
                .header("Content-Security-Policy", CONTENT_SECURITY_POLICY)
                .header("X-Content-Type-Options", "nosniff")
                .build();
    }

    private static String renderPage(
            SearchInvoices.InvoiceSearchQuery query,
            SearchInvoices.InvoicePageView result) {
        StringBuilder html = new StringBuilder(16_384);
        appendDocumentStart(html);
        appendHeader(html);
        appendNavigation(html);
        html.append("<main><section class=\"panel\"><h1>Accounting Invoices</h1>");
        appendSearchForm(html, query);
        appendResults(html, result);
        html.append("</section></main></body></html>");
        return html.toString();
    }

    private static void appendDocumentStart(StringBuilder html) {
        html.append("""
                <!doctype html><html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Accounting Invoices - OFBiz Modern</title>
                <style>
                :root{--emerald:#1BC5BD;--light:#dcfffd;--dark:#133d3b;--ink:#243332}
                *{box-sizing:border-box}body{margin:0;font-family:Arial,sans-serif;color:var(--ink);background:#f4f7f7}
                header{height:64px;background:var(--emerald);display:flex;align-items:center;padding:8px 24px;color:var(--dark)}
                header img{height:42px;width:auto;margin-right:18px}header strong{font-size:1.25rem}
                nav{background:var(--dark);padding:10px 20px;display:flex;gap:6px;flex-wrap:wrap}
                nav a{color:white;text-decoration:none;padding:7px 9px;border-radius:3px}nav a:hover,nav a:focus{background:var(--emerald);color:var(--dark)}
                main{padding:24px}.panel{background:white;border-top:5px solid var(--emerald);padding:20px;box-shadow:0 2px 8px #0002}
                form{background:var(--light);padding:16px;display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;margin-bottom:20px}
                label{font-weight:bold;display:flex;flex-direction:column;gap:5px}input{padding:8px;border:1px solid #86aaa7;border-radius:3px}
                button{align-self:end;padding:9px 18px;border:0;border-radius:3px;background:var(--emerald);color:var(--dark);font-weight:bold;cursor:pointer}
                .table-wrap{overflow-x:auto}table{width:100%;border-collapse:collapse}th{background:var(--dark);color:white;text-align:left}
                th,td{padding:9px;border-bottom:1px solid #d9e3e2;vertical-align:top}tbody tr:nth-child(even){background:#f6fbfa}
                a{color:#086e69}.meta{display:flex;gap:18px;flex-wrap:wrap}.note{padding:10px;background:#fff8d8;border-left:4px solid #d8a600}
                .actions{white-space:nowrap}.actions a{margin-right:8px}@media(max-width:700px){main{padding:10px}header{padding:8px 12px}}
                </style></head><body>
                """);
    }

    private static void appendHeader(StringBuilder html) {
        html.append("<header><a href=\"/accounting/control/findInvoices\">")
                .append("<img src=\"/images/ofbiz_logo.png\" alt=\"Apache OFBiz\"></a>")
                .append("<strong>Accounting Manager · Modern Invoice Slice</strong></header>");
    }

    private static void appendNavigation(StringBuilder html) {
        html.append("<nav aria-label=\"Accounting sections\">");
        for (NavigationLink link : NAVIGATION) {
            html.append("<a href=\"").append(link.path()).append("\">")
                    .append(link.label()).append("</a>");
        }
        html.append("</nav>");
    }

    private static void appendSearchForm(
            StringBuilder html,
            SearchInvoices.InvoiceSearchQuery query) {
        html.append("<form method=\"get\" action=\"").append(SEARCH_ACTION).append("\">");
        appendInput(html, "invoiceId", "Invoice ID", query.invoiceId());
        appendInput(html, "invoiceTypeId", "Invoice type", query.invoiceTypeId());
        appendInput(html, "statusId", "Status", query.statusId());
        appendInput(html, "partyIdFrom", "From party", query.partyIdFrom());
        appendInput(html, "partyId", "To party", query.partyId());
        html.append("<input type=\"hidden\" name=\"limit\" value=\"")
                .append(query.limit()).append("\"><button type=\"submit\">Find invoices</button></form>");
    }

    private static void appendInput(StringBuilder html, String name, String label, String value) {
        html.append("<label>").append(label).append("<input name=\"").append(name)
                .append("\" value=\"").append(escapeInputValue(value)).append("\"></label>");
    }

    private static void appendResults(
            StringBuilder html,
            SearchInvoices.InvoicePageView result) {
        html.append("<div class=\"meta\"><p><strong>").append(result.total())
                .append("</strong> invoice(s)</p><p>Source: ").append(escape(result.source()))
                .append("</p></div><p class=\"note\">").append(escape(result.totalsNote()))
                .append("</p><div class=\"table-wrap\"><table><thead><tr>")
                .append("<th>Invoice</th><th>Type</th><th>Status</th><th>From</th><th>To</th>")
                .append("<th>Date</th><th>Items</th><th>Total</th><th>Applied</th><th>Outstanding</th><th>Actions</th>")
                .append("</tr></thead><tbody>");
        for (SearchInvoices.InvoiceSummaryView invoice : result.invoices()) {
            appendInvoiceRow(html, invoice);
        }
        if (result.invoices().isEmpty()) {
            html.append("<tr><td colspan=\"11\">No invoices matched the search.</td></tr>");
        }
        html.append("</tbody></table></div>");
    }

    private static void appendInvoiceRow(
            StringBuilder html,
            SearchInvoices.InvoiceSummaryView invoice) {
        String encodedInvoiceId = URLEncoder.encode(invoice.invoiceId(), StandardCharsets.UTF_8);
        html.append("<tr><td><a href=\"/api/accounting/invoices/").append(encodedInvoiceId)
                .append("\">").append(escape(invoice.invoiceId())).append("</a></td>");
        appendCell(html, combined(invoice.invoiceTypeDescription(), invoice.invoiceTypeId()));
        appendCell(html, combined(invoice.statusDescription(), invoice.statusId()));
        appendCell(html, combined(invoice.partyNameFrom(), invoice.partyIdFrom()));
        appendCell(html, combined(invoice.partyName(), invoice.partyId()));
        appendCell(html, formatDate(invoice.invoiceDate()));
        appendCell(html, Integer.toString(invoice.itemCount()));
        appendCell(html, formatMoney(invoice.invoiceTotal()));
        appendCell(html, formatMoney(invoice.appliedPaymentTotal()));
        appendCell(html, formatMoney(invoice.outstandingTotal()));
        html.append("<td class=\"actions\"><a href=\"/accounting/control/viewInvoice?invoiceId=")
                .append(encodedInvoiceId).append("\">Legacy detail</a></td></tr>");
    }

    private static void appendCell(StringBuilder html, String value) {
        html.append("<td>").append(escape(value)).append("</td>");
    }

    private static String combined(String description, String id) {
        if (description == null || description.isBlank()) {
            return displayValue(id);
        }
        return description + " [" + displayValue(id) + "]";
    }

    private static String formatDate(LocalDateTime value) {
        return value == null ? "—" : DATE_FORMAT.format(value);
    }

    private static String formatMoney(BigDecimal value) {
        return value == null ? "—" : value.toPlainString();
    }

    private static String displayValue(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(displayValue(value));
    }

    private static String escapeInputValue(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private record NavigationLink(String label, String path) {
    }
}
