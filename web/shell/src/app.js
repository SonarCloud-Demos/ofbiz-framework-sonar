const statusOutput = document.querySelector('#status');
const results = document.querySelector('#catalog-results');
const search = document.querySelector('#catalog-search');

async function loadCatalog(query = '') {
  statusOutput.textContent = 'Loading catalog…';
  results.replaceChildren();
  try {
    const params = new URLSearchParams();
    if (query) {
      params.set('q', query);
    }
    const suffix = params.size ? `?${params}` : '';
    const response = await fetch(`/api/catalog/products${suffix}`, {headers: {'Accept': 'application/json'}});
    if (!response.ok) {
      throw new Error(`Catalog service returned ${response.status}`);
    }
    const catalog = await response.json();
    for (const product of catalog.items ?? []) {
      const item = document.createElement('li');
      const name = document.createElement('strong');
      name.textContent = product.name;
      const details = document.createElement('span');
      details.textContent = `${product.id} · ${product.displayedPriceReference}`;
      item.append(name, details);
      results.append(item);
    }
    statusOutput.textContent = `${catalog.count ?? 0} products found.`;
  } catch {
    statusOutput.textContent = 'The modern catalog is unavailable. Use the legacy catalog and retry later.';
  }
}

search.addEventListener('submit', (event) => {
  event.preventDefault();
  const query = new FormData(search).get('q');
  loadCatalog(typeof query === 'string' ? query.trim() : '');
});

await loadCatalog();
