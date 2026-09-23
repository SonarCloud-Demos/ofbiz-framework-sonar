const form = document.querySelector('#search-form');
const queryInput = document.querySelector('#query');
const results = document.querySelector('#results');
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
  await search('');
}

function renderProducts(products) {
  results.replaceChildren(...products.map((product) => {
    const item = document.createElement('li');
    const heading = document.createElement('strong');
    const description = document.createElement('p');
    heading.textContent = `${product.name} (${product.id})`;
    description.textContent = product.description;
    item.append(heading, description);
    return item;
  }));
  searchStatus.textContent = `${products.length} product${products.length === 1 ? '' : 's'} found.`;
}

async function search(query) {
  if (!authenticated) {
    searchStatus.textContent = 'Sign in to search the catalog.';
    results.replaceChildren();
    return;
  }
  searchStatus.textContent = 'Searching…';
  results.replaceChildren();
  const response = await fetch(`/api/catalog/v1/products?query=${encodeURIComponent(query)}&limit=25`, {
    headers: { Accept: 'application/json' },
    credentials: 'same-origin'
  });
  if (!response.ok) {
    throw new Error(`Catalog request failed with status ${response.status}`);
  }
  const body = await response.json();
  renderProducts(body.products);
}

form.addEventListener('submit', (event) => {
  event.preventDefault();
  search(queryInput.value).catch(() => {
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
  await search('');
} catch {
  searchStatus.textContent = 'The catalog is unavailable. Try again later.';
}
