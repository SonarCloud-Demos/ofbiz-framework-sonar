package org.apache.ofbiz.modern.accounting.invoices.adapter.out.persistence;

import static org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.SOURCE;
import static org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.TOTALS_NOTE;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.ofbiz.modern.accounting.invoices.application.port.out.InvoiceRepository;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Detail;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.HeaderCommand;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Line;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Page;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.PaymentApplication;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Search;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.StatusHistory;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Summary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcInvoiceRepository implements InvoiceRepository {
    private static final String SUMMARY_SQL = """
            SELECT i.invoice_id, i.invoice_type_id, it.description invoice_type_description,
                   i.status_id, si.description status_description, i.party_id_from, i.party_id,
                   i.invoice_date, i.due_date, i.currency_uom_id,
                   (SELECT COUNT(*) FROM invoice_item ii WHERE ii.invoice_id=i.invoice_id) item_count,
                   COALESCE((SELECT SUM(ii.amount * COALESCE(ii.quantity, 1)) FROM invoice_item ii WHERE ii.invoice_id=i.invoice_id), 0) invoice_total,
                   COALESCE((SELECT SUM(pa.amount_applied) FROM payment_application pa WHERE pa.invoice_id=i.invoice_id), 0) applied_total
              FROM invoice i
              LEFT JOIN invoice_type it ON it.invoice_type_id=i.invoice_type_id
              LEFT JOIN status_item si ON si.status_id=i.status_id
             WHERE (CAST(:invoiceId AS VARCHAR) IS NULL OR i.invoice_id=:invoiceId)
               AND (CAST(:invoiceTypeId AS VARCHAR) IS NULL OR i.invoice_type_id=:invoiceTypeId)
               AND (CAST(:statusId AS VARCHAR) IS NULL OR i.status_id=:statusId)
               AND (CAST(:partyIdFrom AS VARCHAR) IS NULL OR i.party_id_from=:partyIdFrom)
               AND (CAST(:partyId AS VARCHAR) IS NULL OR i.party_id=:partyId)
             ORDER BY i.invoice_date DESC NULLS LAST, i.invoice_id
            """;
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public JdbcInvoiceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    @Override public Page search(Search search) {
        MapSqlParameterSource parameters = params(search);
        String countSql = "SELECT COUNT(*) FROM invoice i WHERE (CAST(:invoiceId AS VARCHAR) IS NULL OR i.invoice_id=:invoiceId) AND (CAST(:invoiceTypeId AS VARCHAR) IS NULL OR i.invoice_type_id=:invoiceTypeId) AND (CAST(:statusId AS VARCHAR) IS NULL OR i.status_id=:statusId) AND (CAST(:partyIdFrom AS VARCHAR) IS NULL OR i.party_id_from=:partyIdFrom) AND (CAST(:partyId AS VARCHAR) IS NULL OR i.party_id=:partyId)";
        Long total = namedJdbc.queryForObject(countSql, parameters, Long.class);
        parameters.addValue("limit", search.limit()).addValue("offset", search.offset());
        List<Summary> rows = namedJdbc.query(SUMMARY_SQL + " LIMIT :limit OFFSET :offset", parameters, this::summary);
        return new Page(rows, search.limit(), search.offset(), total == null ? 0 : total, SOURCE, TOTALS_NOTE);
    }

    @Override public Optional<Detail> findById(String id) {
        Search search = new Search(id, null, null, null, null, 1, 0);
        List<Summary> found = search(search).invoices();
        if (found.isEmpty()) return Optional.empty();
        List<Line> lines = jdbc.query("SELECT invoice_item_seq_id, invoice_item_type_id, product_id, description, quantity, amount FROM invoice_item WHERE invoice_id=? ORDER BY invoice_item_seq_id", this::line, id);
        List<PaymentApplication> payments = jdbc.query("SELECT payment_application_id, payment_id, invoice_item_seq_id, amount_applied FROM payment_application WHERE invoice_id=? ORDER BY payment_application_id", this::payment, id);
        List<StatusHistory> history = jdbc.query("SELECT s.status_id, si.description, s.status_date, s.change_by_user_login_id FROM invoice_status s LEFT JOIN status_item si ON si.status_id=s.status_id WHERE s.invoice_id=? ORDER BY s.status_date", this::history, id);
        return Optional.of(new Detail(found.get(0), lines, payments, history, SOURCE, TOTALS_NOTE));
    }

    @Override @Transactional public Detail create(HeaderCommand command) {
        try {
            jdbc.update("INSERT INTO invoice (invoice_id, invoice_type_id, party_id_from, party_id, status_id, invoice_date, due_date, description, currency_uom_id, last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp) VALUES (?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", command.invoiceId(), command.invoiceTypeId(), command.partyIdFrom(), command.partyId(), command.statusId(), timestamp(command.invoiceDate()), timestamp(command.dueDate()), command.description(), command.currencyUomId());
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("invoiceId already exists", exception);
        }
        return findById(command.invoiceId()).orElseThrow();
    }

    @Override @Transactional public Optional<Detail> update(String id, HeaderCommand command) {
        int changed = jdbc.update("UPDATE invoice SET invoice_type_id=?, party_id_from=?, party_id=?, status_id=?, invoice_date=?, due_date=?, description=?, currency_uom_id=?, last_updated_stamp=CURRENT_TIMESTAMP, last_updated_tx_stamp=CURRENT_TIMESTAMP WHERE invoice_id=?", command.invoiceTypeId(), command.partyIdFrom(), command.partyId(), command.statusId(), timestamp(command.invoiceDate()), timestamp(command.dueDate()), command.description(), command.currencyUomId(), id);
        return changed == 0 ? Optional.empty() : findById(id);
    }

    @Override @Transactional public boolean deleteIfIndependent(String id) {
        Integer dependencies = jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM invoice_item WHERE invoice_id=?) + (SELECT COUNT(*) FROM payment_application WHERE invoice_id=?)", Integer.class, id, id);
        return dependencies != null && dependencies == 0 && jdbc.update("DELETE FROM invoice WHERE invoice_id=?", id) == 1;
    }

    @Override public boolean exists(String id) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM invoice WHERE invoice_id=?", Integer.class, id);
        return count != null && count > 0;
    }

    private static MapSqlParameterSource params(Search search) {
        return new MapSqlParameterSource()
                .addValue("invoiceId", nullable(search.invoiceId()))
                .addValue("invoiceTypeId", nullable(search.invoiceTypeId()))
                .addValue("statusId", nullable(search.statusId()))
                .addValue("partyIdFrom", nullable(search.partyIdFrom()))
                .addValue("partyId", nullable(search.partyId()));
    }
    private Summary summary(ResultSet rs, int row) throws SQLException {
        BigDecimal total = rs.getBigDecimal("invoice_total");
        BigDecimal applied = rs.getBigDecimal("applied_total");
        return new Summary(rs.getString("invoice_id"), rs.getString("invoice_type_id"), rs.getString("invoice_type_description"), rs.getString("status_id"), rs.getString("status_description"), rs.getString("party_id_from"), rs.getString("party_id"), instant(rs, "invoice_date"), instant(rs, "due_date"), rs.getString("currency_uom_id"), rs.getLong("item_count"), total, applied, total.subtract(applied));
    }
    private Line line(ResultSet rs, int row) throws SQLException {
        BigDecimal quantity = rs.getBigDecimal("quantity");
        BigDecimal amount = rs.getBigDecimal("amount");
        BigDecimal normalizedQuantity = quantity == null ? BigDecimal.ONE : quantity;
        BigDecimal lineTotal = amount == null ? BigDecimal.ZERO : amount.multiply(normalizedQuantity);
        return new Line(rs.getString("invoice_item_seq_id"), rs.getString("invoice_item_type_id"),
                rs.getString("product_id"), rs.getString("description"), quantity, amount, lineTotal);
    }
    private PaymentApplication payment(ResultSet rs,int row)throws SQLException{return new PaymentApplication(rs.getString("payment_application_id"),rs.getString("payment_id"),rs.getString("invoice_item_seq_id"),rs.getBigDecimal("amount_applied"));}
    private StatusHistory history(ResultSet rs,int row)throws SQLException{return new StatusHistory(rs.getString("status_id"),rs.getString("description"),instant(rs,"status_date"),rs.getString("change_by_user_login_id"));}
    private static Instant instant(ResultSet rs,String column)throws SQLException{Timestamp value=rs.getTimestamp(column);return value==null?null:value.toInstant();}
    private static Timestamp timestamp(Instant value){return value==null?null:Timestamp.from(value);}
    private static String nullable(String value){return value==null||value.isBlank()?null:value;}
}
