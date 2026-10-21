# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

GestiMed: a desktop app for running a medical practice (patients, consultations, prescriptions/ordonnances, lab analyses, appointments). It uses Java Swing with MySQL and is built with Maven. The project was migrated from NetBeans/Ant with hard-coded `.jar` paths. Code, comments, identifiers and user-facing messages are in **French**, so keep new code consistent with that.

## Commands

```
mvn test                                   # compile + run all tests (headless, no MySQL needed)
mvn test -Dtest=PatientServiceTest         # single test class
mvn test -Dtest=PatientServiceTest#methode # single test method
mvn package                                # target/gestimed-3.5-jar-with-dependencies.jar (runnable)
mvn exec:java                              # run the app (main: My_application.PrincipalForm)
```

- Some passing tests print stack traces on purpose (logged `DataAccessException`s), so read the Surefire summary rather than the console noise.
- Target is Java 8 (`maven.compiler.release=8`), so don't use newer language/API features (no `var`, `List.of`, records, etc.). Any recent JDK can compile it.
- DB settings come from `src/main/resources/My_application/Files_Res/db.proprieties` (the misspelling is intentional and referenced in code). That file has no password and must never contain one. Values are overridden, in increasing order of priority, by a local `gestimed.properties` (path from `-Dgestimed.config`, otherwise the working directory, otherwise `user.home`; it is git-ignored), then by `-Dgestimed.db.url|user|password|driver=...`. `-Dgestimed.utilisateur` / `-Dgestimed.medecin` pick the logged-in user and doctor IDs (default 1).
- **MySQL integration test** (`MySqlIntegrationTest`) is skipped unless you pass `-Dgestimed.test.db.url=jdbc:mysql://localhost:3306/<name>_test -Dgestimed.test.db.user=... -Dgestimed.test.db.password=...`. The DB name must end in `_test` because it runs `sql/schema_gestimed.sql`, which DROPs and recreates every table.
- `sql/schema_gestimed.sql` was *reconstructed* from the queries in the code, not taken from the original. Tables must be InnoDB or transactions silently do nothing.
- SQL scripts in `sql/`: `medicale.sql` creates the real `medicale` database non-destructively (`IF NOT EXISTS` only, so an existing table is never altered: schema changes need a dedicated `ALTER TABLE` script). It holds the same definitions as `schema_gestimed.sql` minus the DROPs, so keep the two in sync. Then `donnees_initiales.sql` (reference data, `INSERT IGNORE`, re-runnable), `donnees_test.sql` (fictitious patients/consultations with IDs 9001–9999) and `supprimer_donnees_test.sql` (removes only those). Run them with the MySQL client, e.g. `mysql -u root --default-character-set=utf8mb4 < sql/medicale.sql`.
- `.claude/agents/mysql-architecte.md` is a project subagent for schema design, SQL scripts and schema/DAO consistency checks.

## Architecture

Layering, top to bottom (all under `src/main/java/My_application/`):

1. **Swing views**: NetBeans GUI-builder forms (`*.java` + matching `*.form`) in the root package and feature packages (`Patient/`, `Ordonnance/`, `Analyse/`, `rendezvous/`, `Chat/`, …). Code inside `// <editor-fold>` / `initComponents()` regions is generated from the `.form` XML. Keep the `.form` file in sync if you change those regions. Custom `TableModel`/renderer classes are in `modelTableau/`, `TableModelPrincipale/` and the feature packages ("Rondu" = renderer). Shared UI helpers are in `vue/`.
2. **Controllers** (`controller/`): each window implements a `Vue` sub-interface (e.g. `AccueilVue`) and gets its controller in its constructor. Controllers extend `ControleurBase`, whose `tenter(Runnable)` shows `MetierException` messages as-is and logs `DataAccessException`/other runtime errors before showing a generic message. Route UI actions through `tenter` rather than catching exceptions in views.
3. **Services** (`service/`): business rules. They throw `MetierException` for validation failures. `Session` holds the current user/doctor. `My_application/report/RapportService` (its own package, next to `service/`) builds ordonnance and analysis-request PDFs with OpenPDF (`com.lowagie.*`), which replaced JasperReports.
4. **DAO** (`dao/`): one interface per aggregate, plus the `DAOFactory` abstract factory. `DAOFactory.enTransaction(UniteDeTravail)` runs work atomically. For example, saving a consultation writes to ~6 tables in a single transaction (`DossierService`).
5. **JDBC implementation** (`dao/jdbc/`): `JdbcDAOFactory`, `Jdbc*Dao`, `JdbcHelper` (prepared-statement helpers, `insertAndGetKey`), `RowMapper`/`Mappers`, `ConnectionProvider` (DAOs borrow the connection and must never close it), and `TransactionManager`, which supports nesting (only the outermost call commits or rolls back, tracked per thread). `Database` is a single shared connection that reopens itself when it goes stale. It never shows dialogs; errors propagate as `DataAccessException`.

**Composition root:** `AppContext` is the only place that picks concrete implementations. Views get services with `AppContext.get().getXxxService()`. Tests call `AppContext.initialiser(DAOFactory)` to swap in another factory. UI or service code must not open JDBC connections directly.

**Look and feel:** FlatLaf with a teal "medical" palette. `vue/Theme.installer()` (called from `PrincipalForm.main`) loads `src/main/resources/My_application/theme/FlatLightLaf.properties`. Every window calls `Theme.harmoniser(this)` right after `initComponents()`. That call rewrites at runtime the fonts, colours, borders and PNG icons hard-coded by the NetBeans GUI builder, so generated code and `.form` files don't need hand-editing for styling. New windows must make the same call. In hand-written code (renderers, dialogs), use the palette constants and `Theme.police()` / `Theme.cellule(...)` instead of literal colours and fonts. For interface icons, use `Icones.svg(name, size, colour)` with the Tabler SVGs in `res/svg/`. Legacy PNG → SVG mappings live in `Icones`. Patient avatars stay PNG.

`entities/` holds plain domain objects with legacy lowercase class names (`patient`, `consultation`, `medecin`, `Rendez_Vous`, …). Keep those names as they are.

## Tests

JUnit 4, under `src/test/java`. Most tests don't need a database:
- `fake/MemoireDAOFactory`: an in-memory implementation of the whole DAO layer, used for service and controller tests.
- `fake/FauxJdbc`: a fake JDBC driver built on dynamic proxies. It records the SQL and bound parameters, returns scripted rows, simulates generated keys and checks that statements and result sets get closed. Use it to test `Jdbc*Dao` classes.
- `fake/FausseVue`: records the messages a controller shows. `fake/Donnees` provides sample entities.

## Notes

- Legacy or prototype classes (`AddPatient`, `AjouterPat*`, `Password`, `ImagerieForms/Imagerie` with NetBeans AbsoluteLayout, `ServiceReseau`, `my_model`, `SalleAtten*`) are unused but intentionally kept.
- `Chat/`: `Tchat` starts a TCP server on port 7777 when `PrincipalForm` opens, and `Descussion` is the client. Network threads must update Swing through `SwingUtilities.invokeLater`.
- Database calls run synchronously on the Swing EDT (there is no `SwingWorker`).
