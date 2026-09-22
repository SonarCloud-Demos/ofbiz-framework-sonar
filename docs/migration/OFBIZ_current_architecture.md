# Apache OFBiz current architecture

## Scope and executive summary

This document describes the architecture represented by the current repository: its source organization, runtime collaboration model, and supported deployment topology. It describes the existing OFBiz system, not a proposed migration target.

Apache OFBiz is a metadata-driven Java ERP suite and application framework. Architecturally it is a **modular monolith**. Framework and business capabilities are divided into components, but the active components are assembled into one Gradle distribution, loaded into one JVM, hosted by one embedded servlet container, and normally share one logical database. Component boundaries organize code and configuration; they are not process, transaction, or data-ownership boundaries.

The runtime has two particularly important integration backbones:

- the **Entity Engine**, which supplies a shared metadata-defined persistence model and transaction-aware JDBC access; and
- the **Service Engine**, which supplies named business operations, dispatch, authorization, transactions, asynchronous jobs, scheduling, and optional remote transports.

The web layer, service layer, and persistence layer are all extensible through component metadata. XML descriptors are therefore part of the executable architecture, not merely deployment configuration.

## System context

```text
                         External systems
            SMTP / LDAP / payment / shipping / JMS / RMI
                              ^       |
                              |       v
Browser or API client -> Embedded Tomcat / OFBiz web applications
                              |
                 controllers, events, REST resources
                              |
                     OFBiz Service Engine
               sync | async | scheduled | ECA hooks
                              |
                      OFBiz Entity Engine
                              |
               H2 by default, or external RDBMS
```

Users primarily reach OFBiz through server-rendered business applications over HTTPS. API clients can use the REST component, and integrations can also use email, JMS, RMI, or application-specific adapters. All of these entry points converge on shared framework services and entities inside the same runtime.

## Repository and module structure

### Top-level layout

| Path | Architectural role |
| --- | --- |
| `framework/` | The application platform: startup, component loading, containers, persistence, services, web MVC, widgets, security, REST, and test infrastructure. |
| `applications/` | ERP business components: accounting, order, party, product, shipment, manufacturing, marketing, content, human resources, work effort, and extensions. |
| `themes/` | Shared UI resources and selectable server-side themes. Themes are OFBiz components and can register web applications and seed data. |
| `plugins/` | Optional extension components, when installed. The component loader and distribution support this directory even when it is absent from a checkout. |
| `buildSrc/`, `gradle/`, root Gradle files | Build conventions, dependency declarations, tests, distributions, data-loading tasks, and launch configuration. |
| `docker/`, `Dockerfile`, `DOCKER.adoc` | Container image, entrypoint, initialization hooks, database templates, and example Compose deployment. |
| `config/` | Site-level overrides included in the runtime configuration search path. |
| `runtime/` | Generated state such as database files, logs, temporary files, container initialization markers, and deployed runtime artifacts. It is not a source module. |

The root `settings.gradle` discovers active components and includes each as a Gradle subproject. The build is consequently component-aware, but produces a single OFBiz application distribution rather than independently versioned application services.

### Component model

The coarse-grained module is an **OFBiz component**, declared by an `ofbiz-component.xml` file. At startup, `framework/base/config/component-load.xml` loads components below `framework`, `themes`, `applications`, and `plugins`. A component can contribute:

- Java, Groovy, and resource classpaths;
- entity models, entity groups, Entity ECA rules, and seed/demo data;
- service models, service groups, Service ECA rules, and message rules;
- one or more servlet web applications and mount points;
- widget screens, forms, menus, FreeMarker templates, labels, and static assets;
- tests and component-specific build dependencies.

This is a plugin architecture inside a single process. Components discover shared resources through component URLs and global model readers. They are not isolated class loaders or separately deployable units.

### Framework modules

The main framework responsibilities are:

- **`base`**: JVM entry point, configuration, component discovery, lifecycle containers, utilities, concurrency, cryptography, secrets, logging, and resource location.
- **`entity`**: metadata models, `Delegator` persistence façade, `GenericValue` records, querying, JDBC helpers and pools, caching, transactions, and data import/export.
- **`entityext`**: higher-level entity services, synchronization, sequencing, tenants, and entity maintenance.
- **`service`**: service metadata, dispatch contexts, synchronous and asynchronous invocation, scheduling, job persistence, transactions, semaphores, ECA processing, and transport engines.
- **`webapp`**: servlet front controller, request events, view handlers, sessions, web security, URL handling, and application registration.
- **`widget`**: metadata-driven screens, forms, menus, trees, and rendering support.
- **`security`**: authentication and permission abstractions used by web applications and services.
- **`catalina`**: the embedded Tomcat integration and deployment of component web applications.
- **`rest-api`**: REST endpoints and API documentation mounted at `/rest` and `/docs`.
- **`common`, `datafile`, `minilang`, `webtools`, and `testtools`**: shared domain utilities, structured-file processing, legacy MiniLang execution, administration tools, and OFBiz-aware testing.

The observed Java package graph contains 71 top-level package nodes and 399 dependencies. The most broadly shared dependencies are `org.apache.ofbiz.base.util`, `org.apache.ofbiz.entity`, `org.apache.ofbiz.service`, `org.apache.ofbiz.security`, and `org.apache.ofbiz.webapp`. This confirms that framework modules are shared foundations and that application-component boundaries are permeable.

### Business application modules

| Component | Principal responsibility and common collaborations |
| --- | --- |
| `party` | People, organizations, roles, relationships, contact mechanisms, and communications; foundational to most business modules. |
| `product` | Catalogs, categories, products, features, pricing, promotions, inventory, facilities, suppliers, configurations, subscriptions, and shipment-related functions. |
| `order` | Sales and purchase order capture and lifecycle; collaborates heavily with party, product, inventory, shipment, payment, and accounting concepts. |
| `accounting` | Invoices, payments, financial accounts, agreements, tax, fixed assets, budgets, and general ledger; depends on shared party/product/order information. |
| `shipment` | Picking, packing, verification, packages, and shipment processing, using order and product/facility data. |
| `manufacturing` | Bills of material, routing, production runs, MRP, and job-shop management, based on product, inventory, and work-effort data. |
| `marketing` | Campaigns, contact lists, tracking, reports, and sales-force automation, centered on party, product, order, and communication data. |
| `content` | Content and data-resource management, CMS-like facilities, templates, and publishing. |
| `workeffort` and `humanres` | Work, calendars, tasks, employment, positions, skills, and related organizational processes. |
| `commonext` and `securityext` | Application setup and higher-level login, certificate, permission, and migration functions. |
| `datamodel` | Shared business entity definitions that underpin the integrated ERP data model. |

These are recognizable functional areas, but they are not strict bounded contexts. They can call one another's named services, query one another's entities, share transactions, and reference common Java packages and UI resources.

## Runtime architecture and module interaction

### Startup and assembly

The Gradle application entry point is `org.apache.ofbiz.base.start.Start`. Startup creates the OFBiz container environment, reads the component loader, adds component resources, and starts configured containers. The Catalina container embeds Tomcat and deploys every enabled component web application against the configured server. Entity and service metadata from all enabled components is merged into shared readers/registries.

The result is one assembled runtime with common caches, thread pools, security infrastructure, model registries, and database access. Disabling a component changes what is registered, but does not turn the remaining components into independent processes.

### Web request flow

The traditional UI follows a metadata-driven Front Controller/MVC model:

1. Tomcat receives an HTTP(S) request for a component mount point.
2. OFBiz filters and `ControlServlet` establish request, session, security, and application context.
3. the web application's `WEB-INF/controller.xml` maps a request name to an event and a response;
4. an event invokes Java, Groovy/script, or a named service through the dispatcher;
5. the response selects another request or a view;
6. a view handler renders an OFBiz screen, FreeMarker template, JSP, redirect, or serialized response.

Screen/widget XML composes menus, forms, trees, and templates. Themes supply shared decoration and assets. The normal frontend is therefore server-rendered and session-aware, with JavaScript as enhancement rather than a separately deployed SPA.

The REST component is another web application in the same Tomcat/JVM. It adapts HTTP requests to OFBiz security, service, and entity facilities and publishes API documentation. It does not establish a separate service tier.

### Service interaction

Business operations are registered by name in component service descriptors. Callers generally use a `LocalDispatcher`/`ServiceDispatcher`, so invocation is mediated by service metadata rather than direct construction of implementation objects. A service definition describes input/output attributes and execution policies such as authentication and transaction handling.

The configured engines support entity-auto, Java, Groovy, scripts, MiniLang, service groups, interfaces, routes, JMS, and RMI. The dispatcher can execute work synchronously, asynchronously, or as a persisted scheduled job. The default job poller checks the database and runs jobs through a bounded thread pool.

Service ECA rules can run additional services before, after, or in response to another service. Entity ECA rules similarly react to persistence operations. These hooks create important cross-component interactions that are visible in XML even when there is no direct Java call.

Although JMS and RMI engines exist, the default architectural path is local in-process dispatch. Remote engines are optional integration mechanisms, not evidence that application modules are deployed independently.

### Persistence and transactions

The Entity Engine is the shared data-access layer. Components register XML entity definitions into common model readers. Code uses a `Delegator`, `EntityQuery`, entity conditions, and generic `GenericValue` records instead of JPA/Hibernate entities. View entities define joins and projections in metadata.

The default delegator maps the main, OLAP, and tenant entity groups to configured data sources. The repository defaults to file-backed H2 schemas for local operation and can be configured for PostgreSQL and other JDBC databases. DBCP supplies connection pooling and the Geronimo transaction factory supplies transaction coordination. Production deployments are expected to externalize database configuration.

Transactions can span services and entity operations across several business components because they share the same dispatcher, delegator, transaction manager, and database. This enables integrated ERP workflows but tightly couples extraction and independent scaling: there is no enforced per-component schema ownership.

Seed, seed-initial, tenant, demo, and external data readers load XML data through the Entity Engine. Entity and service ECA rules may add synchronous side effects, while scheduled services persist deferred work in the same database.

### Dependency direction in practice

The broad dependency shape is:

```text
business webapps and REST adapters
              |
              v
business services and ECA rules <----> other business components
              |
              v
service + security + entity frameworks
              |
              v
base/container infrastructure and relational database
```

This direction is conceptual rather than strictly enforced. Some base utilities reference higher framework facilities, web and widget packages collaborate bidirectionally, and business modules directly share entity and service contracts. XML component resources also create dependencies that a Java-only graph cannot fully represent.

## Cross-cutting concerns

- **Security:** centralized user login, sessions, permission checks, service authentication metadata, CSRF/CORS and security-header handling, with application-specific permissions stored as business data.
- **Configuration:** properties and XML are read from the distribution and optional site overrides. Component URLs make resources relocatable inside the OFBiz distribution.
- **Logging and operations:** shared logging configuration writes runtime logs; JSON logging can be enabled through startup properties. Webtools provides operational and data-administration functions.
- **Caching:** entity and framework caches are in-process. Multi-instance deployments therefore require careful cache invalidation and shared-database behavior; the default delegator configuration does not enable distributed cache clearing.
- **Background work:** scheduled/asynchronous services use database-backed jobs plus an in-process poller. Every replicated application node must use compatible job-leasing configuration.
- **Extensibility:** plugins use the same component contract as built-in modules and can add code, entities, services, webapps, and data, but consequently have access to the same shared internals.
- **Testing:** unit tests coexist with container-aware and integration tests that initialize OFBiz facilities such as the dispatcher and delegator. Gradle orchestrates component tests and aggregated reports.

## Build and deployment architecture

### Build outputs

Gradle compiles Java 17 and Groovy sources across active component subprojects. The application plugin defines `org.apache.ofbiz.base.start.Start` as the main class. The distribution includes `framework/`, `applications/`, `themes/`, and installed `plugins/`, plus launch scripts and runtime dependencies. Common operational tasks load seed/demo data, create an administrator, run tests, or launch OFBiz.

This produces one deployable unit. There is no current build pipeline that emits accounting, order, product, or other business components as separately runnable services.

### Direct JVM deployment

A non-container deployment runs the generated OFBiz script on a Java 17-compatible JVM. The JVM starts the OFBiz lifecycle and embedded Tomcat. Configuration, secrets, database drivers, logs, and writable runtime data must be supplied to the installation. The application typically exposes HTTPS on port 8443; connector settings can be changed and AJP can be enabled when placing OFBiz behind a reverse proxy.

A production topology commonly consists of a reverse proxy/load balancer, one or more complete OFBiz JVMs, and a shared external relational database. SMTP, LDAP, payment gateways, carrier systems, or a JMS broker are attached only when the selected features require them.

### Container image

The root `Dockerfile` is a multi-stage build:

1. a Java 17 builder downloads Gradle, copies the OFBiz sources, generates secret keys, and creates the tar distribution;
2. a Java 17 runtime base extracts the distribution under `/ofbiz`, creates a non-root `ofbiz` user, and installs the entrypoint;
3. the default `runtime` target starts uninitialized, while the optional `demo` target preloads demo data.

The image exposes:

- `8443` for HTTPS;
- `8009` for optional AJP; and
- `5005` for optional remote debugging.

The entrypoint initializes a new runtime only once and records markers under `/ofbiz/runtime/container_state`. It can load seed or demo data, create an administrator, configure the allowed host/content URL, disable components, and select/configure an external database. Hook directories allow operators to run scripts before or after configuration and data loading, and an additional-data directory imports XML through the Entity Engine.

Persistent or externally supplied paths are:

- `/ofbiz/runtime` for mutable state, embedded database files, and logs;
- `/ofbiz/config` for configuration overrides;
- `/ofbiz/lib-extra` for additional libraries such as database drivers; and
- `/docker-entrypoint-hooks` for initialization customization.

With no external database variables, the container uses H2 stored in the runtime volume. The supplied PostgreSQL example uses Docker Compose to run one PostgreSQL container and one complete OFBiz container, publishes 8443, mounts logs and a post-configuration hook, and loads demo data. It illustrates the supported external-database topology; it is not a horizontally scaled production reference architecture.

### Scaling and availability characteristics

The unit of horizontal scaling is the whole OFBiz runtime. Multiple identical JVMs/containers can sit behind a load balancer and share a database, but operators must account for HTTP session strategy, in-process caches, scheduled-job leases, shared files/content, and initialization being performed exactly once. Individual business modules cannot be scaled independently without changing their shared-service and shared-data assumptions.

The repository provides image construction and a local PostgreSQL example, but it does not define a Kubernetes deployment, service mesh, mandatory message broker, or cloud-specific control plane. Those are deployment-environment choices outside the current source architecture.

## Architectural characteristics and constraints

### Strengths

- A consistent service, security, transaction, and persistence model spans a large ERP domain.
- Metadata makes components highly configurable and enables plugins without rebuilding the core concepts.
- In-process calls and shared transactions simplify cross-domain workflows.
- One distribution is straightforward to run for development and smaller installations.
- The database and many integrations are replaceable through configuration and adapters.

### Consequences for change and migration

- Component descriptors, controller files, service definitions, entity definitions, and ECA rules must be included in impact analysis alongside Java/Groovy code.
- Shared entity access means an apparent module boundary does not imply data ownership.
- Cross-component local service calls assume shared security, transaction, and execution context.
- Framework changes have a wide blast radius because most application packages depend on base, entity, service, security, and web infrastructure.
- Extracting a business capability requires explicit API contracts, independently owned data, replacement of shared transactions, reliable inter-process messaging, and separate operational concerns.

## Practical end-to-end example

For a typical business request, such as changing an invoice:

1. the accounting web application's controller resolves the request;
2. a request event performs authentication/permission checks and invokes a named accounting service;
3. the Service Engine validates inputs, establishes a transaction, and selects the Java/Groovy/entity-auto implementation;
4. the implementation reads and writes accounting, party, order, or product records through the shared `Delegator`;
5. service or entity ECA rules may invoke additional services, and deferred work may be persisted for the job poller;
6. the transaction commits through the shared transaction manager;
7. a widget/FreeMarker screen renders the updated view, or the REST adapter serializes a response.

That path captures the defining property of the current architecture: functionally separated ERP components collaborate through common metadata-driven engines inside one deployable Java runtime and one integrated data model.

## Evidence and limitations

This description is based on the current component descriptors, root Gradle build, component loader, Entity Engine and Service Engine configuration, web controllers and webapp descriptors, Docker image and entrypoint documentation, PostgreSQL Compose example, and the current Java architecture dependency graph. Generated build artifacts under `services/` are not treated as deployable source modules because they are not active tracked Gradle components in the current checkout. Runtime customization and optional plugins can alter an installation, so a particular deployed instance may enable fewer components or add site-specific ones.
