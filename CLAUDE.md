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
mvn verify                                 # tests + JaCoCo coverage check + SpotBugs (what CI runs)
```

- Target is Java 8 (`maven.compiler.release=8`), so don't use newer language/API features (no `var`, `List.of`, records, etc.). Any recent JDK can compile it.
- DB settings come from `src/main/resources/My_application/Files_Res/db.proprieties` (the misspelling is intentional and referenced in code). That file has no password and must never contain one. Values are overridden, in increasing order of priority, by a local `gestimed.properties` (path from `-Dgestimed.config`, otherwise the working directory, otherwise `user.home`; it is git-ignored), then by `-Dgestimed.db.url|user|password|driver=...`. `-Dgestimed.utilisateur` / `-Dgestimed.medecin` pick the user offered at first use and the default doctor (default 1).
- The default DB account is `gestimed` (not `root`), created by `sql/creer_compte_gestimed.sql` with SELECT/INSERT/UPDATE/DELETE only. The connection uses `sslMode=PREFERRED`. `Database` logs a warning if it connects as `root`.
- Error messages shown to the user never include SQL or stack traces: `ControleurBase` displays a generic message and the detail goes to the log file set up by `Journal.installer()` (`~/GestiMed/gestimed0.log`, folder overridable with `-Dgestimed.journal`). A duplicate key (MySQL 1062) surfaces as `DoublonException`, which services translate into a `MetierException`.
- **Login and audit**: `PrincipalForm.main` shows `ConnexionForm` first. `AuthentificationService` checks PBKDF2 hashes (`service/MotDePasse`, stored in `utilisateur.motDePasse`), locks a login for 30 s after 5 failures and opens `Session`. While no user has credentials, the login screen creates the first account instead. Further accounts: `My_application.outils.GestionComptes` (command line). `JournalAcces` writes to `journal_acces` (who opened or modified which patient file). Services take it as an optional constructor argument (`JournalAcces.AUCUN` in tests that don't care). Writes go in the same transaction as the action, and a failed audit write makes the action fail.
- **MySQL integration test** (`MySqlIntegrationTest`) is skipped unless you pass `-Dgestimed.test.db.url=jdbc:mysql://localhost:3306/<name>_test -Dgestimed.test.db.user=... -Dgestimed.test.db.password=...`. The DB name must end in `_test` because before each test it runs Flyway `clean` (drops every table) then all migrations.
- **Schema = Flyway migrations** in `src/main/resources/db/migration/` (`V1__schema_initial.sql`, reconstructed from the code's queries, then `V2__authentification.sql`, …). Never edit an applied migration; add `V<n>__description.sql`. `dao/jdbc/Migrations` wraps Flyway 9 (last line supporting Java 8; `flyway-mysql` needed for MySQL/MariaDB). Migrations are applied with an admin account by `My_application.outils.MigrationBase migrer [compte]` (creates the DB if missing; adopts a pre-Flyway DB as version 1 or 2 depending on whether `journal_acces` exists). The app account `gestimed` cannot alter tables, so at startup `PrincipalForm` only calls `Migrations.etat()` and refuses to start if a tracked DB has pending migrations. Fresh install: `MigrationBase migrer`, then `sql/creer_compte_gestimed.sql`, then `sql/donnees_initiales.sql`. Tables must be InnoDB or transactions silently do nothing.
- The runnable jar is built with `src/assembly/jar-executable.xml`, which merges `META-INF/services` (otherwise Flyway loses its MySQL plugin).
- **Quality gates** (`mvn verify`, run by `.github/workflows/ci.yml` with a MariaDB 10.4 service): JaCoCo requires ≥ `couverture.minimum` line coverage in each of `service`, `dao.jdbc` and `controller`; SpotBugs (threshold Medium) fails the build on any finding not excluded with a reason in `spotbugs-exclusions.xml`.

## Architecture

Layering, top to bottom (all under `src/main/java/My_application/`):

1. **Swing views**: NetBeans GUI-builder forms (`*.java` + matching `*.form`) in the root package and feature packages (`Patient/`, `Ordonnance/`, `Analyse/`, `rendezvous/`, `Chat/`, …). Code inside `// <editor-fold>` / `initComponents()` regions is generated from the `.form` XML. Keep the `.form` file in sync if you change those regions. Custom `TableModel`/renderer classes are in `modelTableau/`, `TableModelPrincipale/` and the feature packages ("Rondu" = renderer). Shared UI helpers are in `vue/`.
2. **Controllers** (`controller/`): each window implements a `Vue` sub-interface (e.g. `AccueilVue`) and gets its controller in its constructor. Controllers extend `ControleurBase`, whose `tenter(Runnable)` / `tenter(Supplier<T>, T siEchec)` (use lambdas) show `MetierException` messages as-is and logs `DataAccessException`/other runtime errors before showing a generic message. Route UI actions through `tenter` rather than catching exceptions in views.
3. **Services** (`service/`): business rules. They throw `MetierException` for validation failures. `Session` holds the current user/doctor. `report/RapportService` builds ordonnance and analysis-request PDFs with OpenPDF (`com.lowagie.*`), which replaced JasperReports.
4. **DAO** (`dao/`): one interface per aggregate, plus the `DAOFactory` abstract factory. `DAOFactory.enTransaction(UniteDeTravail)` runs work atomically. For example, saving a consultation writes to ~6 tables in a single transaction (`DossierService`).
5. **JDBC implementation** (`dao/jdbc/`): `JdbcDAOFactory`, `Jdbc*Dao`, `JdbcHelper` (prepared-statement helpers, `insertAndGetKey`, `queryParCle` to group rows by consultation id), `RowMapper`/`Mappers`, and `TransactionManager`, which supports nesting (only the outermost call commits or rolls back, tracked per thread). During a transaction the connection is pinned in a `ThreadLocal`: if it drops mid-transaction the next statement fails instead of silently reopening a fresh autocommit connection. One `ReentrantLock` (owned by `TransactionManager`, shared with `JdbcHelper`) serializes all DB access, so a background read waits for a running transaction. `Database` is a single shared connection that reopens itself when it goes stale; it only calls `isValid` after 30 s idle. It never shows dialogs; errors propagate as `DataAccessException`. Load aggregates per patient, not per consultation (`DossierService.ouvrirDossier` uses 6 queries whatever the number of consultations).

**Composition root:** `AppContext` is the only place that picks concrete implementations. There is no global instance: `PrincipalForm.main` creates it once with `AppContext.mysql()` (command-line tools do the same, tests use `new AppContext(daoFactory)`) and it is passed by constructor. Windows that open other windows (`PrincipalForm`, `ListePatient`, `DossierPatientForm`) receive the `AppContext`; the others receive only the services they need (e.g. `OrdonnanceForm(consultation, CatalogueService)`). UI or service code must not open JDBC connections directly.

**Look and feel:** FlatLaf with a teal "medical" palette. `vue/Theme.installer()` (called from `PrincipalForm.main`) loads `src/main/resources/My_application/theme/FlatLightLaf.properties`. Every window calls `Theme.harmoniser(this)` right after `initComponents()`. That call rewrites at runtime the fonts, colours, borders and PNG icons hard-coded by the NetBeans GUI builder, so generated code and `.form` files don't need hand-editing for styling. New windows must make the same call. In hand-written code (renderers, dialogs), use the palette constants and `Theme.police()` / `Theme.cellule(...)` instead of literal colours and fonts. For interface icons, use `Icones.svg(name, size, colour)` with the Tabler SVGs in `res/svg/`. Legacy PNG → SVG mappings live in `Icones`. Patient avatars stay PNG.

`entities/` holds plain domain objects with legacy lowercase class names (`patient`, `consultation`, `medecin`, `Rendez_Vous`, …). Keep those names as they are. Their fields are private and dates/times are `java.time` (`LocalDate`, `LocalTime`): `java.sql.Date`/`Time` only appear in `dao/jdbc` (`Mappers.jour/heure` when reading, `JdbcHelper` converts `java.time` parameters when binding). Format dates with `DateTimeFormatter` and `Locale.FRENCH`, not `SimpleDateFormat`.

## Tests

JUnit 4, under `src/test/java`. Most tests don't need a database:
- `fake/MemoireDAOFactory`: an in-memory implementation of the whole DAO layer, used for service and controller tests.
- `fake/FauxJdbc`: a fake JDBC driver built on dynamic proxies. It records the SQL and bound parameters, returns scripted rows, simulates generated keys and checks that statements and result sets get closed. Use it to test `Jdbc*Dao` classes.
- `fake/FausseVue`: records the messages a controller shows. `fake/Donnees` provides sample entities.

## Notes

- Legacy or prototype classes (`AddPatient`, `AjouterPat*`, `Password`, `ImagerieForms/Imagerie` with NetBeans AbsoluteLayout, `ServiceReseau`, `my_model`, `SalleAtten*`) are unused but intentionally kept.
- `Chat/`: `Tchat` starts a TCP server on port 7777 when `PrincipalForm` opens, bound to the loopback interface only (no auth, no encryption), and `Descussion` is the client. Network threads must update Swing through `SwingUtilities.invokeLater`.
- Most database calls still run synchronously on the Swing EDT. Slow reads go through `ControleurBase.enArrierePlan(Lecture, Affichage)`: the read runs in a `SwingWorker` (`Executeur.SWING`), the display and error reporting happen on the EDT. Controllers take an optional `Executeur`; tests pass `Executeur.IMMEDIAT`. So far only `AccueilController.rafraichir` uses it.
- `vue/AutoCompletion` listens to document changes (not key codes) and debounces lookups with a 250 ms `javax.swing.Timer`.
