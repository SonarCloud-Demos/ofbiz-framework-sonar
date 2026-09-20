package org.apache.ofbiz.modern.accounting.adapter.out.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ofbiz.modern.accounting.application.port.out.LoadInvoiceSummaries;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSearchCriteria;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSummary;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSummaryPage;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class JdbcInvoiceSummaryRepository implements LoadInvoiceSummaries {

    private static final String SEARCH_SQL_PREFIX = """
            WITH item_totals AS (
                SELECT invoice_id,
                       COUNT(*) AS item_count,
                       COALESCE(SUM(COALESCE(quantity, 1) * COALESCE(amount, 0)), 0) AS invoice_total
                  FROM invoice_item
                 GROUP BY invoice_id
            ),
            payment_totals AS (
                SELECT pa.invoice_id,
                       COALESCE(SUM(pa.amount_applied), 0) AS applied_payment_total
                  FROM payment_application pa
                  LEFT JOIN payment p ON p.payment_id = pa.payment_id
                 WHERE p.effective_date IS NULL OR p.effective_date <= CURRENT_TIMESTAMP
                 GROUP BY pa.invoice_id
            )
            SELECT i.invoice_id,
                   i.invoice_type_id,
                   it.description AS invoice_type_description,
                   i.status_id,
                   si.description AS status_description,
                   i.party_id_from,
                   COALESCE(pgf.group_name, NULLIF(CONCAT_WS(' ', pf.first_name, pf.last_name), ''), i.party_id_from)
                       AS party_name_from,
                   i.party_id,
                   COALESCE(pgt.group_name, NULLIF(CONCAT_WS(' ', pt.first_name, pt.last_name), ''), i.party_id)
                       AS party_name,
                   i.invoice_date,
                   i.due_date,
                   i.currency_uom_id,
                   COALESCE(items.item_count, 0) AS item_count,
                   COALESCE(items.invoice_total, 0) AS invoice_total,
                   COALESCE(payments.applied_payment_total, 0) AS applied_payment_total,
                   COALESCE(items.invoice_total, 0) - COALESCE(payments.applied_payment_total, 0)
                       AS outstanding_total
              FROM invoice i
              LEFT JOIN invoice_type it ON it.invoice_type_id = i.invoice_type_id
              LEFT JOIN status_item si ON si.status_id = i.status_id
              LEFT JOIN person pf ON pf.party_id = i.party_id_from
              LEFT JOIN party_group pgf ON pgf.party_id = i.party_id_from
              LEFT JOIN person pt ON pt.party_id = i.party_id
              LEFT JOIN party_group pgt ON pgt.party_id = i.party_id
              LEFT JOIN item_totals items ON items.invoice_id = i.invoice_id
              LEFT JOIN payment_totals payments ON payments.invoice_id = i.invoice_id
            """;
    private static final String SEARCH_SQL_SUFFIX = """
             ORDER BY i.invoice_date DESC NULLS LAST, i.invoice_id
             LIMIT :limit OFFSET :offset
            """;
    private static final String COUNT_SQL_PREFIX = "SELECT COUNT(*) FROM invoice i";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcInvoiceSummaryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public InvoiceSummaryPage load(InvoiceSearchCriteria criteria) {
        InvoiceFilters filters = createFilters(criteria);
        filters.parameters()
                .addValue("limit", criteria.limit())
                .addValue("offset", criteria.offset());
        Long count = jdbcTemplate.queryForObject(
                COUNT_SQL_PREFIX + filters.sql(), filters.parameters(), Long.class);
        List<InvoiceSummary> invoices = jdbcTemplate.query(
                SEARCH_SQL_PREFIX + filters.sql() + SEARCH_SQL_SUFFIX,
                filters.parameters(),
                (resultSet, ignoredRowNumber) -> mapRow(resultSet));
        long total = count == null ? 0 : count;
        return new InvoiceSummaryPage(invoices, total, criteria.limit(), criteria.offset());
    }

    private InvoiceSummary mapRow(ResultSet resultSet) throws SQLException {
        BigDecimal invoiceTotal = resultSet.getBigDecimal("invoice_total");
        BigDecimal appliedPaymentTotal = resultSet.getBigDecimal("applied_payment_total");
        BigDecimal outstandingTotal = resultSet.getBigDecimal("outstanding_total");
        return new InvoiceSummary(
                resultSet.getString("invoice_id"),
                resultSet.getString("invoice_type_id"),
                resultSet.getString("invoice_type_description"),
                resultSet.getString("status_id"),
                resultSet.getString("status_description"),
                resultSet.getString("party_id_from"),
                resultSet.getString("party_name_from"),
                resultSet.getString("party_id"),
                resultSet.getString("party_name"),
                toLocalDateTime(resultSet.getTimestamp("invoice_date")),
                toLocalDateTime(resultSet.getTimestamp("due_date")),
                resultSet.getString("currency_uom_id"),
                resultSet.getInt("item_count"),
                invoiceTotal,
                appliedPaymentTotal,
                outstandingTotal);
    }

    private static InvoiceFilters createFilters(InvoiceSearchCriteria criteria) {
        StringBuilder sql = new StringBuilder(" WHERE 1 = 1");
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        addFilter(sql, parameters, "invoiceId", "i.invoice_id", criteria.invoiceId());
        addFilter(sql, parameters, "invoiceTypeId", "i.invoice_type_id", criteria.invoiceTypeId());
        addFilter(sql, parameters, "statusId", "i.status_id", criteria.statusId());
        addFilter(sql, parameters, "partyIdFrom", "i.party_id_from", criteria.partyIdFrom());
        addFilter(sql, parameters, "partyId", "i.party_id", criteria.partyId());
        return new InvoiceFilters(sql.toString(), parameters);
    }

    private static void addFilter(
            StringBuilder sql,
            MapSqlParameterSource parameters,
            String parameterName,
            String columnName,
            String value) {
        String normalizedValue = emptyToNull(value);
        if (normalizedValue != null) {
            sql.append(" AND ").append(columnName).append(" = :").append(parameterName);
            parameters.addValue(parameterName, normalizedValue);
        }
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record InvoiceFilters(String sql, MapSqlParameterSource parameters) {
    }
}
