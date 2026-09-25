const statusOutput = document.querySelector('#status');

try {
  const response = await fetch('/api/reference', {headers: {'Accept': 'application/json'}});
  if (!response.ok) {
    throw new Error(`Reference service returned ${response.status}`);
  }
  const result = await response.json();
  statusOutput.textContent = `Connected to ${result.service ?? 'reference service'}.`;
} catch {
  statusOutput.textContent = 'Reference service is unavailable.';
}
