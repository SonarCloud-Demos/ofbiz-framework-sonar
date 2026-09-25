# Environments

The `test`, `stage`, and `prod` roots compose shared modules with separate Azure Storage backend keys and non-overlapping reference CIDRs. Runtime values are supplied externally; these roots contain no real subscription IDs, tenant IDs, credentials, state, secrets, owners, cost centers, budgets, or alert recipients.
