-- =====================================================================
-- GestiMed, migration V2 : écran de connexion et journal des accès.
--
-- Identifiant, empreinte du mot de passe, état et médecin associé pour
-- chaque utilisateur ; table journal_acces (qui a ouvert ou modifié quel
-- dossier). Après cette migration, aucun utilisateur n'a de mot de passe :
-- au premier lancement, GestiMed propose de créer le premier compte.
-- =====================================================================
ALTER TABLE utilisateur
  ADD COLUMN login      VARCHAR(40)  NULL AFTER qualite,
  ADD COLUMN motDePasse VARCHAR(200) NULL AFTER login,
  ADD COLUMN actif      TINYINT(1)   NOT NULL DEFAULT 1 AFTER motDePasse,
  ADD COLUMN idMedecin  INT          NULL AFTER actif,
  ADD UNIQUE KEY uk_utilisateur_login (login),
  ADD KEY idx_utilisateur_medecin (idMedecin),
  ADD CONSTRAINT fk_utilisateur_medecin FOREIGN KEY (idMedecin)
    REFERENCES medecin (idMedcin) ON DELETE SET NULL ON UPDATE CASCADE;

CREATE TABLE IF NOT EXISTS journal_acces (
  idJournal     BIGINT       NOT NULL AUTO_INCREMENT,
  dateHeure     DATETIME     NOT NULL,
  idUtilisateur INT          NULL,
  action        VARCHAR(40)  NOT NULL,
  idPatient     INT          NULL,
  detail        VARCHAR(255) NULL,
  poste         VARCHAR(100) NULL,
  PRIMARY KEY (idJournal),
  KEY idx_journal_patient (idPatient, dateHeure),
  KEY idx_journal_utilisateur (idUtilisateur, dateHeure),
  CONSTRAINT fk_journal_utilisateur FOREIGN KEY (idUtilisateur)
    REFERENCES utilisateur (idUtilisateur) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
