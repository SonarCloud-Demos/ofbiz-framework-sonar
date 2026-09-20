#!/bin/sh

set -eu

GATEWAY_URL=${GATEWAY_URL:-https://localhost:18080}
LEGACY_URL=${LEGACY_URL:-https://localhost:18443}
TEST_INVOICE_ID=${TEST_INVOICE_ID:-MSVCTEST$(date +%s)}
WORK_DIR=$(mktemp -d "${TMPDIR:-/tmp}/ofbiz-phase1-smoke.XXXXXX")
CREATED_INVOICE=false

cleanup() {
    if [ "$CREATED_INVOICE" = true ]; then
        curl -kfsS -X DELETE "$GATEWAY_URL/api/accounting/invoices/$TEST_INVOICE_ID" >/dev/null || true
    fi
    rm -rf "$WORK_DIR"
}
trap cleanup EXIT HUP INT TERM

fail() {
    echo "FAIL: $1" >&2
    exit 1
}

fetch() {
    url=$1
    output=$2
    curl -kfsS -L --retry 30 --retry-delay 2 --retry-all-errors \
        "$url" -o "$output" || fail "GET $url"
}

fetch_unauthenticated() {
    url=$1
    output=$2
    curl -ksS -L "$url" -o "$output" || fail "GET unauthenticated page $url"
}

assert_contains() {
    file=$1
    expected=$2
    description=$3
    grep -Fq "$expected" "$file" || fail "$description"
}

assert_status() {
    expected=$1
    method=$2
    url=$3
    body=${4:-}
    if [ -n "$body" ]; then
        actual=$(curl -ksS -o "$WORK_DIR/response" -w '%{http_code}' \
            -X "$method" -H 'Content-Type: application/json' --data "$body" "$url")
    else
        actual=$(curl -ksS -o "$WORK_DIR/response" -w '%{http_code}' -X "$method" "$url")
    fi
    [ "$actual" = "$expected" ] || fail "$method $url returned $actual, expected $expected"
}

echo "Checking modern invoice route and UI continuity"
fetch "$GATEWAY_URL/accounting/control/findInvoices?invoiceId=8009" "$WORK_DIR/invoices.html"
assert_contains "$WORK_DIR/invoices.html" "OFBiz" "modern invoice UI does not contain the OFBiz brand"
assert_contains "$WORK_DIR/invoices.html" "#1BC5BD" "modern invoice UI does not contain the Emerald header color"
assert_contains "$WORK_DIR/invoices.html" "Legacy detail" "modern invoice UI does not link to legacy detail"
assert_contains "$WORK_DIR/invoices.html" "8009" "modern invoice UI did not return invoice 8009"

echo "Checking modern service APIs"
fetch "$GATEWAY_URL/api/accounting/invoices/health" "$WORK_DIR/accounting-health.json"
assert_contains "$WORK_DIR/accounting-health.json" '"status":"UP"' "Accounting health is not UP"
assert_contains "$WORK_DIR/accounting-health.json" '"service":"modern-accounting-invoice-service"' \
    "Accounting health came from the wrong service"
fetch "$GATEWAY_URL/api/notifications/health" "$WORK_DIR/notification-health.json"
assert_contains "$WORK_DIR/notification-health.json" '"status":"UP"' "Notification health is not UP"
fetch "$GATEWAY_URL/api/accounting/invoices?limit=2" "$WORK_DIR/invoices.json"
assert_contains "$WORK_DIR/invoices.json" '"source":"legacy-ofbiz-postgres"' "invoice list source marker is missing"
fetch "$GATEWAY_URL/api/accounting/invoices/8009" "$WORK_DIR/invoice-detail.json"
assert_contains "$WORK_DIR/invoice-detail.json" '"lineItems"' "invoice detail line items are missing"
assert_contains "$WORK_DIR/invoice-detail.json" '"paymentApplications"' \
    "invoice detail payment applications are missing"

echo "Checking legacy fallback, gateway rewriting, and representative assets"
fetch "$LEGACY_URL/webtools" "$WORK_DIR/direct-webtools.html"
fetch "$GATEWAY_URL/webtools" "$WORK_DIR/gateway-webtools.html"
fetch_unauthenticated "$LEGACY_URL/webtools/control/checkLogin" "$WORK_DIR/direct-login.html"
fetch_unauthenticated "$GATEWAY_URL/webtools/control/checkLogin" "$WORK_DIR/gateway-login.html"
assert_contains "$WORK_DIR/direct-login.html" "action=\"$LEGACY_URL" \
    "direct legacy login form does not retain its origin"
assert_contains "$WORK_DIR/gateway-login.html" "action=\"$GATEWAY_URL" \
    "gateway legacy login form action was not rewritten"
fetch_unauthenticated "$LEGACY_URL/accounting/control/findInvoices" "$WORK_DIR/direct-invoices.html"
assert_contains "$WORK_DIR/direct-invoices.html" "OFBiz" "direct legacy invoice fallback is unavailable"
fetch_unauthenticated "$GATEWAY_URL/accounting/control/findPayments" "$WORK_DIR/payments.html"
assert_contains "$WORK_DIR/payments.html" "OFBiz" "non-strangled Accounting route did not reach OFBiz"

for asset in \
    /helveticus/HELVETICUS_EMERALD.less \
    /helveticus/style.css \
    /common/js/node_modules/jquery-ui-dist/jquery-ui.min.js
do
    fetch "$LEGACY_URL$asset" "$WORK_DIR/direct-asset"
    fetch "$GATEWAY_URL$asset" "$WORK_DIR/gateway-asset"
done

echo "Checking limited invoice-header create, update, and delete"
CREATE_BODY=$(printf '%s' \
    "{\"invoiceId\":\"$TEST_INVOICE_ID\",\"invoiceTypeId\":\"SALES_INVOICE\",\"partyIdFrom\":\"Company\",\"partyId\":\"DemoCustomer\",\"currencyUomId\":\"USD\",\"description\":\"Phase 1 smoke test\"}")
UPDATE_BODY=$(printf '%s' \
    "{\"invoiceId\":\"$TEST_INVOICE_ID\",\"invoiceTypeId\":\"SALES_INVOICE\",\"partyIdFrom\":\"Company\",\"partyId\":\"DemoCustomer\",\"currencyUomId\":\"USD\",\"description\":\"Phase 1 smoke test updated\"}")
assert_status 201 POST "$GATEWAY_URL/api/accounting/invoices" "$CREATE_BODY"
CREATED_INVOICE=true
assert_status 204 PUT "$GATEWAY_URL/api/accounting/invoices/$TEST_INVOICE_ID" "$UPDATE_BODY"
assert_status 204 DELETE "$GATEWAY_URL/api/accounting/invoices/$TEST_INVOICE_ID"
CREATED_INVOICE=false

echo "Phase 1 hybrid smoke test passed"
