# ADR-006: Identity and browser security

**Status:** Accepted for the platform foundation and first pilot. Revisit after the first read cutover and first write-ownership transfer.

**Decision:** Use Entra ID Authorization Code + PKCE with a BFF that keeps tokens out of browser storage. A time-bounded identity bridge creates only the minimum OFBiz session. Services validate tokens and authorize independently.

**Rationale:** It supports MFA/Conditional Access and avoids leaking bearer tokens into browser code or across the legacy boundary.
