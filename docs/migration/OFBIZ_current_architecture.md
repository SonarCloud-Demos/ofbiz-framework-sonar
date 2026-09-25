# OFBiz current architecture

## Scope and basis

This document describes the architecture present in this repository at commit `2af28b8` on branch `trunk`. It is a description of the current implementation, not a target design. It is based on the repository's build, component, entity, service, web, startup, and container-deployment configuration, supplemented by a static Java dependency graph. No intended-architecture constraints are configured in SonarQube for this project, so the boundaries described below are observed conventions rather than centrally enforced dependency rules.

The most important architectural fact is that this is a **modular monolith**. Framework and business components are modules for development, configuration, and runtime discovery, but they are compiled into a shared classpath and normally execute in one JVM. Web applications, services, entity definitions, themes, and scheduled work are assembled into that process. They are not independently deployable services.

## System context

Apache OFBiz is an ERP application and application framework. In its normal deployment the system has four kinds of external interaction:

- Browser and API clients connect over HTTP/HTTPS to web applications hosted by embedded Tomcat.
- OFBiz reads and writes a relational database through the Entity Engine. The repository includes local/default data-source definitions and a Docker example using PostgreSQL.
- Integration endpoints can use REST, email, and optional RMI facilities. Business components may also call external systems through component-specific services.
- Operators build, load data, start, stop, and test the system through Gradle tasks, distribution scripts, or the container entrypoint.

```text
Browser / API client
        |
        | HTTPS
        v
+-------------------------------------------------------------+
| One OFBiz JVM                                               |
|                                                             |
| Embedded Tomcat -> webapps -> events/screens/controllers    |
|                                |                            |
|                                v                            |
|                        Service Engine                       |
|                         |          |                         |
|                         v          v                         |
|                    Entity Engine  component Java/Groovy     |
|                         |                                    |
|       shared security, widgets, configuration, cache         |
+-------------------------|-----------------------------------+
                          v
                  Relational database
```

## Repository and module structure

### Top-level areas

| Area | Architectural role |
| --- | --- |
| `framework/` | Reusable OFBiz runtime: bootstrap, component loading, persistence, service dispatch, security, web hosting, widgets, testing, and utilities. |
| `applications/` | ERP domain components and their entity extensions, services, screens, web applications, and seed/demo data. |
| `themes/` | Shared and selectable presentation themes, templates, static assets, widgets, and theme web applications. |
| `plugins/` | Extension point for optional components. It is referenced by component loading, Gradle, distributions, and Docker, but is absent in this checkout. |
| `docker/` and `Dockerfile` | OCI image construction, runtime initialization, hook mechanism, configuration overlays, and a PostgreSQL deployment example. |
| `docs/` | User, developer, and migration documentation. |
| `buildSrc/`, `build.gradle`, `common.gradle`, `settings.gradle` | Gradle build logic and dynamic discovery of active OFBiz components. |
| `runtime/` | Generated or mutable runtime state such as data, logs, temporary files, and configuration overlays. It is not source architecture and may be created during build/run. |

### The component is the primary logical module

Every active module is identified by an `ofbiz-component.xml` descriptor. The root loader in `framework/base/config/component-load.xml` scans, in order, `framework`, `themes`, `applications`, and `plugins`. A descriptor can contribute any combination of:

- configuration directories and classpath entries;
- Java and Groovy implementation classes;
- entity models, entity event-condition-action rules, and seed/demo data;
- service models, service groups, service event-condition-action rules, and mail condition-action rules;
- startup containers;
- web applications and their mount points.

The same discovery mechanism drives both the runtime and Gradle. `settings.gradle` includes every active component as a Gradle subproject, while the root build aggregates component source directories and dependencies. Consequently, a component is a strong packaging and registration convention, but not a process boundary or an isolated class-loader boundary.

### Framework components

The framework is layered by responsibility:

| Component | Responsibility |
| --- | --- |
| `base` and `start` | Main entry point, command parsing, lifecycle, component registry, configuration, shared utilities, crypto/secrets, caches, concurrency, and container loading. |
| `entity` | Entity model metadata, delegators, data-source abstraction, query and transaction support, database access, and entity caching. |
| `entityext` | Higher-level entity utilities, data loading, synchronization, tenants, and scheduled entity services. |
| `service` | Service metadata, dispatch, execution engines, scheduling, transactions, ECAs, RMI, and mail support. |
| `security` | Authentication/authorization abstractions and the security data model used by applications and web layers. |
| `catalina` | Embedded Tomcat configuration, connectors, realms, valves, and deployment of component-declared web applications. |
| `webapp` | Shared servlet filters, events, request/session handling, controller support, and web security utilities. |
| `widget` | Metadata-driven screens, forms, menus, and trees, rendered through templates such as FreeMarker. |
| `minilang` | Interpreter/runtime for legacy XML MiniLang business logic. |
| `common` | Cross-domain entities, seed data, services, widgets, templates, and general reusable behavior. |
| `webtools` | Administrative and diagnostic UI for entities, services, data import/export, logs, and tests. |
| `rest-api` | REST endpoints and API documentation web applications. |
| `datafile` | Structured flat-file parsing and writing support. |
| `testtools` | Integration-test container, test models, and reporting support. |

Static dependency analysis confirms that `base.util`, `entity`, `service`, `security`, `webapp`, and `widget` are the principal shared hubs. In particular, nearly every business package depends on the Entity and Service Engines. The graph contains 71 top-level Java package nodes and 399 package dependencies. Some dependencies are cyclic at package level—for example, shared base utilities reference higher-level facilities that also use base utilities—so this is not a strictly layered codebase.

### Business application components

The `applications/` directory organizes ERP capabilities into coarse-grained domains:

| Component | Main capability |
| --- | --- |
| `datamodel` | Shared core ERP entity definitions for accounting, content, HR, manufacturing, marketing, orders, parties, products, shipments, and work effort. |
| `party` | People, organizations, roles, contact mechanisms, relationships, and party management. |
| `product` | Catalog, categories, products, features, pricing, inventory/facilities, suppliers, stores, subscriptions, and promotions. |
| `order` | Sales and purchase order lifecycle and order-management UI. |
| `accounting` | Invoices, payments, agreements, budgets, fixed assets, taxation, and general ledger; includes accounting, receivables, and payables webapps. |
| `content` | Content metadata, storage, and content-management UI. |
| `workeffort` | Work efforts, calendars, tasks, and iCalendar exposure. |
| `humanres` | Human resources functionality. |
| `manufacturing` | Bills of material, routing, production runs, job-shop management, and MRP. |
| `marketing` | Marketing and sales-force automation. |
| `securityext` | Login, certificates, migration, and additional security behavior. |
| `commonext` | Setup and shared JavaScript resources used by application UIs. |

These domain boundaries are permeable. Business logic routinely crosses components through shared entity models and synchronous service calls. For example, accounting depends on order, party, product, security, entity, and service packages; shipment-related code depends on party and product; work-effort code depends on content. There is one shared transactional model rather than a database or schema owned independently by each domain.

### Internal shape of a component

A typical application component contains:

```text
<component>/
  ofbiz-component.xml     registration and runtime assembly
  src/                    Java/Groovy implementation
  config/                 component properties and classpath resources
  entitydef/              entities, views, and entity ECAs
  servicedef/             service contracts, groups, and service ECAs
  data/                   seed, initial, demo, and migration data
  minilang/               legacy XML business logic
  widget/                 screens, forms, menus, and trees
  template/               FreeMarker and other rendering templates
  webapp/                  one or more servlet applications
  testdef/                 component integration tests
```

Not every component has every directory. This organization intentionally colocates a business capability's metadata, implementation, UI, and bootstrap data.

## Runtime architecture and interaction paths

### Bootstrap and lifecycle

The Gradle application main class is `org.apache.ofbiz.base.start.Start`. A normal `gradlew ofbiz` or distribution-script launch performs the following logical sequence:

1. Parse startup commands and select loader groups such as `main`, `rmi`, `load-data`, or `test`.
2. Create a `ContainerLoader` and first initialize the mandatory component container.
3. Discover active component descriptors and collect container configurations matching the requested loader group.
4. Instantiate and initialize those containers, then start them in configured order.
5. On shutdown, stop loaded containers in reverse order.

The main runtime container set includes:

- `delegator-container`, which initializes Entity Engine delegators;
- `service-container`, which initializes dispatchers and service engines;
- `javamail-container` for mail sessions;
- `admin-container` for process administration/shutdown commands;
- `secret-audit-container` for secret auditing;
- `catalina-container`, which starts embedded Tomcat and deploys registered webapps.

Separate loader modes add data-loading, RMI, or testing containers. These are modes within the same executable architecture, not separate production services.

### Web request flow

Each component can register one or more webapps with a context root. Embedded Tomcat creates those applications using the component descriptor and the webapp's `WEB-INF/web.xml`. The repository declares administrative and business webapps such as `webtools`, `ordermgr`, `accounting`, `ap`, `ar`, `catalog`, `facility`, `partymgr`, `content`, `humanres`, `manufacturing`, `marketing`, `sfa`, `workeffort`, `rest-api`, and API documentation, plus theme/static-resource webapps.

A conventional UI request passes through servlet filters and an OFBiz controller, then dispatches an event or renders a view. Events may be Java, Groovy, service, or other supported handlers. Views commonly resolve metadata-driven widgets, which compose screens, forms, menus, templates, labels, and theme assets. Authentication and authorization use the shared security layer and database-backed user/security entities.

Webapps have separate servlet contexts and URL roots, but they coexist in the same Tomcat and JVM and use the same component registry, Entity Engine, Service Engine, and database. A failure or resource shortage in one can therefore affect all others.

### Service interaction

The Service Engine is the main business-operation boundary. XML service definitions declare names, parameters, authentication requirements, transaction behavior, and implementation engines. Implementations can be Java, Groovy, MiniLang, entity-auto operations, or other registered engines.

Callers obtain a local dispatcher associated with an Entity Engine delegator and invoke services synchronously or asynchronously. The dispatcher resolves the service globally from all active component resources. Service ECAs can trigger additional services around another service's lifecycle, while scheduled-service entities arrange deferred or recurring execution. This provides loose coupling by service name, but the default calls still execute in-process and often share a database transaction.

Optional RMI containers can expose dispatch remotely, but RMI is not part of the default `main` loader. The REST component exposes selected behavior through HTTP; it is an adapter over the same in-process service and entity facilities, not an independent backend.

### Persistence and data ownership

The Entity Engine loads entity definitions contributed by framework and application components into a shared model. Code uses a named `Delegator` rather than direct component-owned repositories. The delegator maps entities to entity groups and data sources configured in `framework/entity/config/entityengine.xml`; it supplies query construction, relationships, view entities, sequencing, caching, transaction integration, and database portability.

This has several consequences:

- Data is logically grouped by entity metadata, but domains do not own isolated databases.
- Components can query and mutate entities declared by other components.
- Service transactions can span multiple business domains and tables.
- Entity and service ECAs introduce implicit reactions that are not visible from a direct Java call graph.
- Data loading is assembled from component-declared reader classes such as `seed`, `seed-initial`, and `demo`.

The checked-in Entity Engine configuration includes multiple delegator/data-source choices and is intended to be overridden for deployments. The supplied Docker PostgreSQL example replaces the entity-engine configuration with a template wired to the PostgreSQL service.

### Cross-cutting mechanisms

- **Security:** shared user-login, permission, and security-group models are consumed by services and webapps. Component services may declare authentication and authorization behavior.
- **Transactions:** service execution and Entity Engine operations integrate with the shared transaction manager; transaction boundaries are typically specified in service metadata.
- **Caching:** Entity Engine and utility caches are process-local and configured centrally, reinforcing the single-runtime model.
- **Presentation:** widgets and themes are shared across domain webapps. Theme components are runtime components, not merely build-time CSS packages.
- **Configuration:** properties and XML configuration are found through component resource loaders. Deployment configuration remains closely tied to the distribution, although container volumes provide overrides.
- **Extensibility:** a plugin can contribute the same resource types as a built-in component and joins the same build, classpath, registries, database, and process.

## Build and packaging architecture

Gradle treats the repository as one root application with dynamically included component subprojects. Java 17 is the source and target baseline. Root source sets aggregate active component sources and resources; component-specific Gradle files can add libraries or frontend build steps. The distribution contains `framework`, `applications`, `themes`, and `plugins` alongside scripts and the runtime classpath.

The normal packaging outputs are `distTar` and `distZip`. After unpacking, `bin/ofbiz` or `bin/ofbiz.bat` starts the same main class with the assembled classpath. There is no repository-defined decomposition into independently versioned or deployed domain artifacts.

Data preparation is a separate operational phase. Tasks such as `loadAll` start OFBiz in data-load mode and import component-declared seed/demo datasets through the Entity Engine. Production initialization can select only `seed` and `seed-initial` readers and then create an administrator account.

## Deployment architecture

### Archive deployment

The simplest production model is:

1. Build a `distTar` or `distZip` archive.
2. Copy and unpack it on a host with a Java 17 runtime.
3. Provide production database, secrets, certificates, and other configuration.
4. run `bin/ofbiz` under the operating system's service manager.

This is a single-host, single-JVM application instance connected to an external or embedded relational database. Horizontal replication is not defined by the repository and requires deployment-specific treatment of sessions, caches, scheduled jobs, shared files, and database coordination.

### Container image

The root `Dockerfile` is a multi-stage Java 17 build:

- The builder installs the Gradle wrapper dependencies, copies framework/application/theme/plugin sources, generates secret keys, and produces `distTar`.
- A runtime base extracts that distribution under `/ofbiz`, installs `xsltproc` for first-run component disabling, creates an unprivileged `ofbiz` user, and adds the container entrypoint.
- Final image variants support initialization and runtime use while exposing configuration and mutable-state volumes.

The image runs as the non-root `ofbiz` user, uses `/ofbiz/docker-entrypoint.sh`, and defaults to `bin/ofbiz`. It exposes:

- `8443` for HTTPS;
- `8009` for the AJP connector when configured;
- `5005` for remote JVM debugging when enabled.

Persistent/customizable locations include `/ofbiz/config`, `/ofbiz/runtime`, and `/ofbiz/lib-extra`. `/docker-entrypoint-hooks` supports ordered lifecycle hooks. The entrypoint applies configuration overlays, manages first-run setup, can disable selected components, runs initialization hooks, and finally executes OFBiz.

### Supplied PostgreSQL composition

`docker/examples/postgres-demo/docker-compose.yml` defines two containers:

- `db`: PostgreSQL 13 with a persistent database volume and initialization environment;
- `ofbiz`: the OFBiz image, configured for that database, publishing the HTTPS endpoint and mounting configuration/hook material.

The database is therefore a separate infrastructure process, but all OFBiz framework and ERP domains remain in one application container. The example is a demonstration topology, not a complete high-availability production design.

### Deployment-unit summary

| Concern | Actual deployment unit |
| --- | --- |
| Framework runtime | OFBiz JVM/application container |
| All ERP domains | Same OFBiz JVM/application container |
| UI webapps and REST API | Multiple servlet contexts in the same embedded Tomcat |
| Background/scheduled services | Same JVM and shared database |
| Themes and plugins | Loaded into the same runtime |
| Relational data store | Separate database process in external-database deployments |
| Build/data-load/test modes | Alternative invocations of the same application and component set |

## Architectural characteristics and constraints

### Strengths

- Component descriptors provide a consistent extension model across code, services, entities, data, webapps, and startup behavior.
- Metadata-driven entities, services, and widgets allow substantial functionality to be assembled without hard-wired Java composition.
- One process and one transaction model make cross-domain workflows straightforward and atomic.
- Shared engines centralize security, persistence, dispatch, scheduling, rendering, and operational tooling.
- Archive and container packaging produce a self-contained runtime with optional plugins.

### Coupling and migration-relevant constraints

- **Shared classpath:** components can directly reference one another, and the observed package graph is dense and partly cyclic.
- **Shared schema and transaction model:** domain data is not isolated, and workflows can depend on cross-domain database transactions.
- **Global registries:** services, entities, resources, and webapps are discovered by name from all active components; names and load order are architectural contracts.
- **Implicit control flow:** service ECAs, entity ECAs, scheduled jobs, controller mappings, and widget metadata can connect modules without direct source-code calls.
- **Single failure and scaling boundary:** webapps and background work compete for the same heap, threads, caches, and process availability.
- **Configuration coupled to packaging:** many defaults live inside the source/distribution; Docker improves override mechanics but does not change the runtime decomposition.
- **Optional plugin variability:** the runtime architecture can change materially depending on the contents of `plugins/`, which is not present in this checkout.
- **No enforced intended architecture:** the current SonarQube intended model contains no constraints, so dependency direction is not checked against a formal target architecture.

Any migration or decomposition effort should therefore inventory metadata references and database usage in addition to Java calls. Treating directories or Gradle subprojects as if they were already independent services would miss the system's global registries, shared transactions, ECAs, and shared web runtime.

## Principal evidence in the repository

The following files are the primary sources for this description:

- `framework/base/config/component-load.xml` — component discovery roots.
- `common.gradle`, `settings.gradle`, and `build.gradle` — dynamic Gradle projects, aggregated source sets, main class, distribution contents, and Java baseline.
- `framework/*/ofbiz-component.xml`, `applications/*/ofbiz-component.xml`, and `themes/*/ofbiz-component.xml` — registered containers, entity/service resources, webapps, and data.
- `framework/base/src/main/java/org/apache/ofbiz/base/container/ContainerLoader.java` and `framework/start/` — startup and lifecycle behavior.
- `framework/entity/config/entityengine.xml` — delegators, entity groups, and data sources.
- `framework/service/config/serviceengine.xml` — service execution engines.
- `framework/catalina/ofbiz-component.xml` and webapp `WEB-INF/web.xml` files — embedded Tomcat and servlet applications.
- `Dockerfile`, `docker/docker-entrypoint.sh`, and `docker/examples/postgres-demo/docker-compose.yml` — image and example deployment topology.
- `README.md`, `INSTALL`, and `DOCKER.adoc` — supported build, initialization, archive, and container operations.

This inventory describes only the checked-out framework repository. Optional external plugins, environment-specific configuration overlays, reverse proxies, database clustering, orchestration manifests, and infrastructure maintained in other repositories are outside its observable scope.
