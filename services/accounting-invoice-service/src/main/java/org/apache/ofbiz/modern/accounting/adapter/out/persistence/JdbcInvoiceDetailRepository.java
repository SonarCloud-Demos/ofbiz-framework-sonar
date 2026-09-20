package org.apache.ofbiz.modern.accounting.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ofbiz.modern.accounting.application.port.out.LoadInvoiceDetail;
import org.apache.ofbiz.modern.accounting.application.port.out.LoadInvoiceSummaries;
import org.apache.ofbiz.modern.accounting.domain.InvoiceDetail;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSearchCriteria;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSummary;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSummaryPage;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class JdbcInvoiceDetailRepository implements LoadInvoiceDetail {

    private static final String INVOICE_ID = "invoiceId";
    private static final String LINE_ITEMS_SQL = """
            SELECT ii.invoice_item_seq_id,
                   ii.invoice_item_type_id,
                   iit.description AS invoice_item_type_description,
                   ii.product_id,
                   ii.description,
                   COALESCE(ii.quantity, 1) AS quantity,
                   COALESCE(ii.amount, 0) AS amount,
                   COALESCE(ii.quantity, 1) * COALESCE(ii.amount, 0) AS line_total
              FROM invoice_item ii
              LEFT JOIN invoice_item_type iit ON iit.invoice_item_type_id = ii.invoice_item_type_id
             WHERE ii.invoice_id = :invoiceId
             ORDER BY ii.invoice_item_seq_id
            """;
    private static final String PAYMENT_APPLICATIONS_SQL = """
            SELECT pa.payment_application_id,
                   pa.payment_id,
                   p.payment_type_id,
                   pt.description AS payment_type_description,
                   p.status_id,
                   si.description AS status_description,
                   p.effective_date,
                   pa.amount_applied
              FROM payment_application pa
              LEFT JOIN payment p ON p.payment_id = pa.payment_id
              LEFT JOIN payment_type pt ON pt.payment_type_id = p.payment_type_id
              LEFT JOIN status_item si ON si.status_id = p.status_id
             WHERE pa.invoice_id = :invoiceId
             ORDER BY p.effective_date, pa.payment_application_id
            """;
    private static final String STATUS_HISTORY_SQL = """
            SELECT invoice_status.status_id,
                   si.description AS status_description,
                   invoice_status.status_date,
                   invoice_status.change_by_user_login_id
              FROM invoice_status
              LEFT JOIN status_item si ON si.status_id = invoice_status.status_id
             WHERE invoice_status.invoice_id = :invoiceId
             ORDER BY invoice_status.status_date
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final LoadInvoiceSummaries loadInvoiceSummaries;

    public JdbcInvoiceDetailRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            LoadInvoiceSummaries loadInvoiceSummaries) {
        this.jdbcTemplate = jdbcTemplate;
        this.loadInvoiceSummaries = loadInvoiceSummaries;
    }

    @Override
    public Optional<InvoiceDetail> load(String invoiceId) {
        InvoiceSummaryPage page = loadInvoiceSummaries.load(new InvoiceSearchCriteria(
                invoiceId, null, null, null, null, 1, 0));
        if (page.invoices().isEmpty()) {
            return Optional.empty();
        }
        Map<String, ?> parameters = Map.of(INVOICE_ID, invoiceId);
        List<InvoiceDetail.LineItem> lineItems = jdbcTemplate.query(
                LINE_ITEMS_SQL, parameters, (resultSet, ignoredRowNumber) -> mapLineItem(resultSet));
        List<InvoiceDetail.PaymentApplication> paymentApplications = jdbcTemplate.query(
                PAYMENT_APPLICATIONS_SQL,
                parameters,
                (resultSet, ignoredRowNumber) -> mapPaymentApplication(resultSet));
        List<InvoiceDetail.StatusHistory> statusHistory = jdbcTemplate.query(
                STATUS_HISTORY_SQL,
                parameters,
                (resultSet, ignoredRowNumber) -> mapStatusHistory(resultSet));
        InvoiceSummary header = page.invoices().get(0);
        return Optional.of(new InvoiceDetail(header, lineItems, paymentApplications, statusHistory));
    }

    private static InvoiceDetail.LineItem mapLineItem(ResultSet resultSet) throws SQLException {
        return new InvoiceDetail.LineItem(
                resultSet.getString("invoice_item_seq_id"),
                resultSet.getString("invoice_item_type_id"),
                resultSet.getString("invoice_item_type_description"),
                resultSet.getString("product_id"),
                resultSet.getString("description"),
                resultSet.getBigDecimal("quantity"),
                resultSet.getBigDecimal("amount"),
                resultSet.getBigDecimal("line_total"));
    }

    private static InvoiceDetail.PaymentApplication mapPaymentApplication(ResultSet resultSet)
            throws SQLException {
        return new InvoiceDetail.PaymentApplication(
                resultSet.getString("payment_application_id"),
                resultSet.getString("payment_id"),
                resultSet.getString("payment_type_id"),
                resultSet.getString("payment_type_description"),
                resultSet.getString("status_id"),
                resultSet.getString("status_description"),
                toLocalDateTime(resultSet.getTimestamp("effective_date")),
                resultSet.getBigDecimal("amount_applied"));
    }

    private static InvoiceDetail.StatusHistory mapStatusHistory(ResultSet resultSet)
            throws SQLException {
        return new InvoiceDetail.StatusHistory(
                resultSet.getString("status_id"),
                resultSet.getString("status_description"),
                toLocalDateTime(resultSet.getTimestamp("status_date")),
                resultSet.getString("change_by_user_login_id"));
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
