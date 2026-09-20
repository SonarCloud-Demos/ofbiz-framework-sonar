package org.apache.ofbiz.modern.accounting.adapter.in.web;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDateTime;
import org.apache.ofbiz.modern.accounting.application.port.in.ManageInvoiceHeaders;
import org.apache.ofbiz.modern.accounting.application.port.in.SearchInvoices;
import org.apache.ofbiz.modern.accounting.application.port.in.GetInvoiceDetail;
import org.apache.ofbiz.modern.accounting.domain.InvoiceHeaderMutation;
import org.springframework.stereotype.Component;

@Component
@Path("/api/accounting/invoices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InvoiceResource {

    private final SearchInvoices searchInvoices;
    private final GetInvoiceDetail getInvoiceDetail;
    private final ManageInvoiceHeaders manageInvoiceHeaders;

    public InvoiceResource(
            SearchInvoices searchInvoices,
            GetInvoiceDetail getInvoiceDetail,
            ManageInvoiceHeaders manageInvoiceHeaders) {
        this.searchInvoices = searchInvoices;
        this.getInvoiceDetail = getInvoiceDetail;
        this.manageInvoiceHeaders = manageInvoiceHeaders;
    }

    @GET
    public SearchInvoices.InvoicePageView search(
            @QueryParam("invoiceId") String invoiceId,
            @QueryParam("invoiceTypeId") String invoiceTypeId,
            @QueryParam("statusId") String statusId,
            @QueryParam("partyIdFrom") String partyIdFrom,
            @QueryParam("partyId") String partyId,
            @DefaultValue("10") @QueryParam("limit") int limit,
            @DefaultValue("0") @QueryParam("offset") int offset) {
        SearchInvoices.InvoiceSearchQuery query = new SearchInvoices.InvoiceSearchQuery(
                invoiceId, invoiceTypeId, statusId, partyIdFrom, partyId, limit, offset);
        return searchInvoices.search(query);
    }

    @GET
    @Path("/{invoiceId}")
    public GetInvoiceDetail.InvoiceDetailView get(@PathParam("invoiceId") String invoiceId) {
        return getInvoiceDetail.get(invoiceId).orElseThrow(NotFoundException::new);
    }

    @POST
    public Response create(InvoiceHeaderRequest request) {
        InvoiceHeaderMutation invoice = request.toMutation(request.invoiceId());
        ManageInvoiceHeaders.MutationResult result = manageInvoiceHeaders.create(invoice);
        if (result == ManageInvoiceHeaders.MutationResult.DUPLICATE) {
            throw conflict("Invoice already exists");
        }
        return Response.created(URI.create("/api/accounting/invoices/" + invoice.invoiceId())).build();
    }

    @PUT
    @Path("/{invoiceId}")
    public Response update(@PathParam("invoiceId") String invoiceId, InvoiceHeaderRequest request) {
        if (request.invoiceId() != null && !invoiceId.equals(request.invoiceId())) {
            throw badRequest("Path invoiceId must match the request body");
        }
        if (manageInvoiceHeaders.update(request.toMutation(invoiceId))
                == ManageInvoiceHeaders.MutationResult.NOT_FOUND) {
            throw new NotFoundException();
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{invoiceId}")
    public Response delete(@PathParam("invoiceId") String invoiceId) {
        ManageInvoiceHeaders.MutationResult result = manageInvoiceHeaders.delete(invoiceId);
        if (result == ManageInvoiceHeaders.MutationResult.NOT_FOUND) {
            throw new NotFoundException();
        }
        if (result == ManageInvoiceHeaders.MutationResult.HAS_DEPENDENCIES) {
            throw conflict("Invoice has line items or payment applications");
        }
        return Response.noContent().build();
    }

    private static WebApplicationException conflict(String message) {
        return new WebApplicationException(message, Response.Status.CONFLICT);
    }

    private static WebApplicationException badRequest(String message) {
        return new WebApplicationException(message, Response.Status.BAD_REQUEST);
    }

    public record InvoiceHeaderRequest(
            String invoiceId,
            String invoiceTypeId,
            String partyIdFrom,
            String partyId,
            String roleTypeId,
            String statusId,
            String billingAccountId,
            String contactMechId,
            LocalDateTime invoiceDate,
            LocalDateTime dueDate,
            LocalDateTime paidDate,
            String invoiceMessage,
            String referenceNumber,
            String description,
            String currencyUomId,
            String recurrenceInfoId) {

        InvoiceHeaderMutation toMutation(String effectiveInvoiceId) {
            return new InvoiceHeaderMutation(effectiveInvoiceId, invoiceTypeId, partyIdFrom, partyId,
                    roleTypeId, statusId, billingAccountId, contactMechId, invoiceDate, dueDate, paidDate,
                    invoiceMessage, referenceNumber, description, currencyUomId, recurrenceInfoId);
        }
    }
}
