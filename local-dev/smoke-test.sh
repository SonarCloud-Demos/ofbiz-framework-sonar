#!/bin/sh
set -eu
base=https://localhost:18080
legacy=https://localhost:18443
curl -ksSf --retry 30 --retry-delay 2 --retry-all-errors \
  "$base/api/accounting/invoices/health" | grep 'modern-accounting-invoice-service'
modern_page=$(curl -ksSf "$base/accounting/control/findInvoices?invoiceId=8009")
printf '%s' "$modern_page" | grep 'Legacy detail'
printf '%s' "$modern_page" | grep 'OFBiz'
printf '%s' "$modern_page" | grep '#1BC5BD'
printf '%s' "$modern_page" | grep 'Accounting'
printf '%s' "$modern_page" | grep '8009'
curl -ksSf "$base/api/accounting/invoices?limit=2" | grep 'legacy-ofbiz-postgres'
curl -ksSf "$base/api/accounting/invoices/8009" | grep 'lineItems'
test_id=MSVCTESTPHASE1
cleanup() { curl -ksS -X DELETE "$base/api/accounting/invoices/$test_id" >/dev/null || true; }
trap cleanup EXIT
cleanup
payload='{"invoiceId":"MSVCTESTPHASE1","invoiceTypeId":"SALES_INVOICE","partyIdFrom":"Company","partyId":"DemoCustomer","statusId":"INVOICE_IN_PROCESS","currencyUomId":"USD"}'
curl -ksSf -H 'Content-Type: application/json' -d "$payload" "$base/api/accounting/invoices" | grep "$test_id"
updated='{"invoiceTypeId":"SALES_INVOICE","partyIdFrom":"Company","partyId":"DemoCustomer","statusId":"INVOICE_READY","currencyUomId":"USD","description":"Phase 1 smoke test"}'
curl -ksSf -X PUT -H 'Content-Type: application/json' -d "$updated" "$base/api/accounting/invoices/$test_id" | grep 'Phase 1 smoke test'
curl -ksSf -X DELETE "$base/api/accounting/invoices/$test_id"
trap - EXIT
curl -ksSf "$legacy/accounting/control/findInvoices" >/dev/null
curl -ksSf "$base/accounting/control/findPayments" >/dev/null
for path in /helveticus/HELVETICUS_EMERALD.less /helveticus/style.css /common/js/node_modules/jquery-ui-dist/jquery-ui.min.js; do
  curl -ksSf "$base$path" >/dev/null
  curl -ksSf "$legacy$path" >/dev/null
done
