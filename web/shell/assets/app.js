const form = document.querySelector('#search-form');
const productIdInput = document.querySelector('#product-id');
const internalNameInput = document.querySelector('#internal-name');
const results = document.querySelector('#results');
const resultRows = document.querySelector('#result-rows');
const searchStatus = document.querySelector('#search-status');
const identityStatus = document.querySelector('#identity-status');
const loginButton = document.querySelector('#login');
const logoutButton = document.querySelector('#logout');
let authenticated = false;

function renderIdentity(session) {
  authenticated = session.authenticated === true || session.authenticated === 1;
  identityStatus.textContent = authenticated ? `Signed in as ${session.displayName || 'authenticated user'}` : 'Not signed in';
  loginButton.textContent = session.mode === 'local' ? 'Sign in locally' : 'Sign in';
  loginButton.dataset.mode = session.mode;
  loginButton.hidden = authenticated;
  logoutButton.hidden = !authenticated;
}

async function requestSession() {
  const response = await fetch('/auth/session', { credentials: 'same-origin', cache: 'no-store' });
  if (!response.ok) throw new Error(`Session request failed with status ${response.status}`);
  renderIdentity(await response.json());
}

async function changeSession(path) {
  const response = await fetch(path, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'X-Requested-With': 'OFBizModernShell' }
  });
  if (!response.ok) throw new Error(`Session change failed with status ${response.status}`);
  renderIdentity(await response.json());
  await search();
}

function cell(value) {
  const element = document.createElement('td');
  element.textContent = value || '—';
  return element;
}

function renderProducts(page) {
  resultRows.replaceChildren(...page.items.map((product) => {
    const row = document.createElement('tr');
    const idCell = document.createElement('td');
    const editLink = document.createElement('a');
    editLink.href = `/catalog/control/EditProduct?productId=${encodeURIComponent(product.productId)}`;
    editLink.textContent = product.productId;
    editLink.setAttribute('aria-label', `Edit ${product.productId} in the legacy catalog`);
    idCell.append(editLink);
    row.append(
      idCell,
      cell(product.productTypeId),
      cell(product.internalName),
      cell(product.brandName),
      cell(product.productName),
      cell(product.description)
    );
    return row;
  }));
  results.hidden = page.items.length === 0;
  searchStatus.textContent = `${page.total} product${page.total === 1 ? '' : 's'} found.`;
}

async function search() {
  if (!authenticated) {
    searchStatus.textContent = 'Sign in to search the catalog.';
    resultRows.replaceChildren();
    results.hidden = true;
    return;
  }
  searchStatus.textContent = 'Searching…';
  resultRows.replaceChildren();
  results.hidden = true;
  const parameters = new URLSearchParams({
    productId: productIdInput.value,
    internalName: internalNameInput.value,
    sort: 'productId',
    direction: 'asc',
    page: '0',
    size: '20'
  });
  const response = await fetch(`/api/catalog/v1/products?${parameters}`, {
    headers: { Accept: 'application/json' },
    credentials: 'same-origin'
  });
  if (!response.ok) {
    throw new Error(`Catalog request failed with status ${response.status}`);
  }
  const body = await response.json();
  renderProducts(body);
}

form.addEventListener('submit', (event) => {
  event.preventDefault();
  search().catch(() => {
    searchStatus.textContent = 'The catalog is unavailable. Try again later.';
  });
});

loginButton.addEventListener('click', () => {
  if (loginButton.dataset.mode === 'local') {
    changeSession('/auth/local/login').catch(() => { identityStatus.textContent = 'Sign-in failed.'; });
  } else {
    window.location.assign('/auth/login');
  }
});

logoutButton.addEventListener('click', () => {
  changeSession('/auth/logout').catch(() => { identityStatus.textContent = 'Sign-out failed.'; });
});

try {
  await requestSession();
  await search();
} catch {
  searchStatus.textContent = 'The catalog is unavailable. Try again later.';
}
