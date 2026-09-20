package org.apache.ofbiz.modern.accounting.adapter.out.persistence;

import java.sql.Types;
import java.util.Map;
import org.apache.ofbiz.modern.accounting.application.port.in.ManageInvoiceHeaders.MutationResult;
import org.apache.ofbiz.modern.accounting.application.port.out.StoreInvoiceHeaders;
import org.apache.ofbiz.modern.accounting.domain.InvoiceHeaderMutation;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public class JdbcInvoiceHeaderRepository implements StoreInvoiceHeaders {

    private static final String COLUMNS = """
            invoice_id, invoice_type_id, party_id_from, party_id, role_type_id, status_id,
            billing_account_id, contact_mech_id, invoice_date, due_date, paid_date, invoice_message,
            reference_number, description, currency_uom_id, recurrence_info_id
            """;
    private static final String VALUES = """
            :invoiceId, :invoiceTypeId, :partyIdFrom, :partyId, :roleTypeId, :statusId,
            :billingAccountId, :contactMechId, :invoiceDate, :dueDate, :paidDate, :invoiceMessage,
            :referenceNumber, :description, :currencyUomId, :recurrenceInfoId
            """;
    private static final String UPDATE_SQL = """
            UPDATE invoice SET invoice_type_id=:invoiceTypeId, party_id_from=:partyIdFrom,
            party_id=:partyId, role_type_id=:roleTypeId, status_id=:statusId,
            billing_account_id=:billingAccountId, contact_mech_id=:contactMechId,
            invoice_date=:invoiceDate, due_date=:dueDate, paid_date=:paidDate,
            invoice_message=:invoiceMessage, reference_number=:referenceNumber,
            description=:description, currency_uom_id=:currencyUomId,
            recurrence_info_id=:recurrenceInfoId WHERE invoice_id=:invoiceId
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public JdbcInvoiceHeaderRepository(NamedParameterJdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    @Override
    public MutationResult insert(InvoiceHeaderMutation invoice) {
        try {
            jdbc.update("INSERT INTO invoice (" + COLUMNS + ") VALUES (" + VALUES + ")", parameters(invoice));
            return MutationResult.CREATED;
        } catch (DuplicateKeyException exception) {
            return MutationResult.DUPLICATE;
        }
    }

    @Override
    public MutationResult update(InvoiceHeaderMutation invoice) {
        return jdbc.update(UPDATE_SQL, parameters(invoice)) == 0
                ? MutationResult.NOT_FOUND : MutationResult.UPDATED;
    }

    @Override
    public MutationResult deleteGuarded(String invoiceId) {
        MutationResult result = transactions.execute(status -> deleteInTransaction(invoiceId));
        return result == null ? MutationResult.NOT_FOUND : result;
    }

    private MutationResult deleteInTransaction(String invoiceId) {
        Map<String, ?> parameters = Map.of("invoiceId", invoiceId);
        if (jdbc.query("SELECT invoice_id FROM invoice WHERE invoice_id=:invoiceId FOR UPDATE",
                parameters, (resultSet, rowNumber) -> resultSet.getString(1)).isEmpty()) {
            return MutationResult.NOT_FOUND;
        }
        if (count("invoice_item", parameters) > 0 || count("payment_application", parameters) > 0) {
            return MutationResult.HAS_DEPENDENCIES;
        }
        jdbc.update("DELETE FROM invoice WHERE invoice_id=:invoiceId", parameters);
        return MutationResult.DELETED;
    }

    private long count(String table, Map<String, ?> parameters) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE invoice_id=:invoiceId", parameters, Long.class);
        return count == null ? 0 : count;
    }

    private static MapSqlParameterSource parameters(InvoiceHeaderMutation invoice) {
        return new MapSqlParameterSource()
                .addValue("invoiceId", invoice.invoiceId()).addValue("invoiceTypeId", invoice.invoiceTypeId())
                .addValue("partyIdFrom", invoice.partyIdFrom()).addValue("partyId", invoice.partyId())
                .addValue("roleTypeId", invoice.roleTypeId(), Types.VARCHAR).addValue("statusId", invoice.statusId())
                .addValue("billingAccountId", invoice.billingAccountId(), Types.VARCHAR)
                .addValue("contactMechId", invoice.contactMechId(), Types.VARCHAR)
                .addValue("invoiceDate", invoice.invoiceDate()).addValue("dueDate", invoice.dueDate(), Types.TIMESTAMP)
                .addValue("paidDate", invoice.paidDate(), Types.TIMESTAMP)
                .addValue("invoiceMessage", invoice.invoiceMessage(), Types.VARCHAR)
                .addValue("referenceNumber", invoice.referenceNumber(), Types.VARCHAR)
                .addValue("description", invoice.description(), Types.VARCHAR)
                .addValue("currencyUomId", invoice.currencyUomId(), Types.VARCHAR)
                .addValue("recurrenceInfoId", invoice.recurrenceInfoId(), Types.VARCHAR);
    }
}
