# ADR-001: Azure Container Apps as default compute

**Status:** Accepted for the platform foundation and first pilot. Revisit after the first read cutover and first write-ownership transfer.

**Decision:** Deploy independently scalable services, workers, jobs, the modern BFF, and transitional OFBiz containers to Azure Container Apps. Use AKS only if a measured requirement cannot be met.

**Rationale:** It provides revision traffic splitting, managed scaling, jobs, private environments, and lower platform overhead while retaining OCI portability.
