package org.apache.ofbiz.modern.accounting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.ofbiz.modern.accounting.application.port.in.ManageInvoiceHeaders.MutationResult;
import org.apache.ofbiz.modern.accounting.domain.InvoiceHeaderMutation;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class JdbcInvoiceHeaderRepositoryTest {

    @Test
    void reportsDuplicateCreateAndMissingUpdate() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.update(anyString(), any(SqlParameterSource.class)))
                .thenThrow(new DuplicateKeyException("duplicate"))
                .thenReturn(0);
        JdbcInvoiceHeaderRepository repository = new JdbcInvoiceHeaderRepository(jdbc, mock(TransactionTemplate.class));

        assertThat(repository.insert(invoice())).isEqualTo(MutationResult.DUPLICATE);
        assertThat(repository.update(invoice())).isEqualTo(MutationResult.NOT_FOUND);
    }

    @Test
    void preventsDeleteWhenAnItemExists() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        TransactionTemplate transactions = executingTransaction();
        when(jdbc.query(anyString(), any(java.util.Map.class), any(RowMapper.class)))
                .thenReturn(List.of("9000"));
        when(jdbc.queryForObject(anyString(), any(java.util.Map.class), eq(Long.class))).thenReturn(1L);
        JdbcInvoiceHeaderRepository repository = new JdbcInvoiceHeaderRepository(jdbc, transactions);

        assertThat(repository.deleteGuarded("9000")).isEqualTo(MutationResult.HAS_DEPENDENCIES);
    }

    @Test
    void deletesAnInvoiceWithoutItemsOrApplications() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.query(anyString(), any(java.util.Map.class), any(RowMapper.class)))
                .thenReturn(List.of("9000"));
        when(jdbc.queryForObject(anyString(), any(java.util.Map.class), eq(Long.class))).thenReturn(0L);
        JdbcInvoiceHeaderRepository repository = new JdbcInvoiceHeaderRepository(jdbc, executingTransaction());

        assertThat(repository.deleteGuarded("9000")).isEqualTo(MutationResult.DELETED);
        verify(jdbc).update(eq("DELETE FROM invoice WHERE invoice_id=:invoiceId"), any(java.util.Map.class));
    }

    private static TransactionTemplate executingTransaction() {
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        return transactions;
    }

    private static InvoiceHeaderMutation invoice() {
        return new InvoiceHeaderMutation("9000", "SALES_INVOICE", "FROM", "TO", null, null,
                null, null, null, null, null, null, null, null, "USD", null);
    }
}
