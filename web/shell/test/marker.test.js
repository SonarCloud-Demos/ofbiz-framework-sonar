import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import test from 'node:test';

const shellUrl = new URL('../src/index.html', import.meta.url);

test('the trusted shell owns exactly one modern marker', async () => {
  const shell = await readFile(shellUrl, 'utf8');

  assert.equal(shell.match(/data-runtime="modern"/g)?.length, 1);
  assert.equal(shell.match(/Modern experience/g)?.length, 1);
});

test('the marker is static and cannot be supplied by URL or API data', async () => {
  const shell = await readFile(shellUrl, 'utf8');

  assert.doesNotMatch(shell, /location\.|URLSearchParams|innerHTML/);
});

test('the shell has the dependency-free accessibility baseline', async () => {
  const shell = await readFile(shellUrl, 'utf8');

  assert.match(shell, /<html lang="[a-z]{2}">/);
  assert.match(shell, /<title>[^<]+<\/title>/);
  assert.equal(shell.match(/<main(?:\s|>)/g)?.length, 1);
  assert.equal(shell.match(/<h1(?:\s|>)/g)?.length, 1);
  assert.match(shell, /aria-live="polite"/);
  assert.doesNotMatch(shell, /<img(?![^>]*\salt=)[^>]*>/);
  assert.match(shell, /<nav aria-label="[^"]+">/);
  assert.match(shell, /href="\/modern\/profile"/);
  assert.match(shell, /<form id="catalog-search" role="search">/);
  assert.match(shell, /<label for="query">/);
});

test('catalog rendering uses safe DOM APIs', async () => {
  const script = await readFile(new URL('../src/app.js', import.meta.url), 'utf8');

  assert.match(script, /textContent/);
  assert.match(script, /replaceChildren/);
  assert.doesNotMatch(script, /innerHTML|insertAdjacentHTML|document\.write/);
});
