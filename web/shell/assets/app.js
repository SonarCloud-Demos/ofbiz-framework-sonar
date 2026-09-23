const form = document.querySelector('#search-form');
const queryInput = document.querySelector('#query');
const results = document.querySelector('#results');
const searchStatus = document.querySelector('#search-status');

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

try {
  await search('');
} catch {
  searchStatus.textContent = 'The catalog is unavailable. Try again later.';
}
