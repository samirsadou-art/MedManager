-- =====================================================================
-- GestiMed, migration V3 : moteur, jeu de caractères et contraintes CHECK
-- identiques sur toutes les bases.
--
-- Pourquoi : une sauvegarde faite avec l'export de phpMyAdmin 5.2 perd,
-- pour chaque table qui a une contrainte CHECK (patient, consultation,
-- prescrire, demandanalyse), le moteur, le jeu de caractères et les CHECK
-- eux-mêmes (« CREATE TABLE ... ) ; »). Une base restaurée depuis un tel
-- export prend les réglages par défaut du serveur et n'a plus de CHECK.
-- Cette migration remet tout en état, que la base vienne de Flyway ou
-- d'une restauration.
--
-- MariaDB 10.2+ (DROP CONSTRAINT IF EXISTS) : c'est le serveur du cabinet
-- (XAMPP, MariaDB 10.4) et de l'intégration continue.
-- Les CHECK de consultation sont refaits par V4.
-- =====================================================================

-- Tables créées plus tard sans précision : utf8mb4 par défaut.
ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- InnoDB obligatoire (transactions de DossierService) ; même collation
-- partout pour que les comparaisons de chaînes entre tables restent possibles.
ALTER TABLE patient           ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE medecin           ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE utilisateur       ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE symptomes         ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE pathologies       ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE medicaments       ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE categanalyse      ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE analyse           ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE consultation      ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE consultsymptome   ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE consultpathologie ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE prescrire         ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE demandanalyse     ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE rondezvous        ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE journal_acces     ENGINE=InnoDB, CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Venant d'un autre jeu de caractères (latin1 par exemple), CONVERT agrandit
-- TEXT en MEDIUMTEXT pour garder la même capacité : on revient au type de V1.
ALTER TABLE demandanalyse MODIFY resultat TEXT NULL;

-- CHECK de V1, recréés s'ils ont été perdus.
ALTER TABLE patient
  DROP CONSTRAINT IF EXISTS ck_patient_sexe,
  ADD CONSTRAINT ck_patient_sexe CHECK (sexePatient IN (0, 1));

ALTER TABLE prescrire
  DROP CONSTRAINT IF EXISTS ck_prescrire_quantites,
  DROP CONSTRAINT IF EXISTS ck_prescrire_mode,
  ADD CONSTRAINT ck_prescrire_quantites CHECK (unite >= 0 AND posologie >= 0 AND duree >= 0),
  ADD CONSTRAINT ck_prescrire_mode CHECK (mode IN (0, 1, 2));

ALTER TABLE demandanalyse
  DROP CONSTRAINT IF EXISTS ck_demandanalyse_note,
  ADD CONSTRAINT ck_demandanalyse_note CHECK (note BETWEEN 0 AND 4);

-- Nouveaux CHECK sur des valeurs codées que seul le code contrôlait.
-- medicaments.type : 'P' = ajouté par le praticien, 'R' = référentiel.
ALTER TABLE medicaments
  DROP CONSTRAINT IF EXISTS ck_medicaments_type,
  ADD CONSTRAINT ck_medicaments_type CHECK (type IN ('P', 'R'));

ALTER TABLE utilisateur
  DROP CONSTRAINT IF EXISTS ck_utilisateur_actif,
  ADD CONSTRAINT ck_utilisateur_actif CHECK (actif IN (0, 1));

-- Une désignation vide passerait l'index unique une fois, puis bloquerait
-- toute autre saisie vide : on la refuse dès la base.
ALTER TABLE symptomes
  DROP CONSTRAINT IF EXISTS ck_symptomes_designation,
  ADD CONSTRAINT ck_symptomes_designation CHECK (TRIM(designation) <> '');

ALTER TABLE pathologies
  DROP CONSTRAINT IF EXISTS ck_pathologies_designation,
  ADD CONSTRAINT ck_pathologies_designation CHECK (TRIM(designation) <> '');

ALTER TABLE analyse
  DROP CONSTRAINT IF EXISTS ck_analyse_designation,
  ADD CONSTRAINT ck_analyse_designation CHECK (TRIM(designation) <> '');

ALTER TABLE medicaments
  DROP CONSTRAINT IF EXISTS ck_medicaments_nom,
  ADD CONSTRAINT ck_medicaments_nom CHECK (TRIM(nomMedicament) <> '');
