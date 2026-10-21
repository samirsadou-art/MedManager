-- =====================================================================
-- GestiMed : migration d'une base existante vers l'écran de connexion
-- et le journal des accès.
--
-- À exécuter UNE fois, en administrateur, sur une base créée avant cette
-- version (une base neuve créée avec sql/medicale.sql n'en a pas besoin) :
--     mysql -u root -p medicale < sql/migration_002_authentification.sql
--   (XAMPP : D:/xampp/mysql/bin/mysql.exe -u root medicale < sql/migration_002_authentification.sql)
--
-- Une seconde exécution échoue sur « Duplicate column name 'login' » sans
-- rien abîmer. Compatible MySQL 8 et MariaDB 10.4+.
--
-- Après la migration, aucun utilisateur n'a encore de mot de passe : au
-- premier lancement, GestiMed propose de créer le compte de l'utilisateur
-- n° 1 (ou de celui donné par -Dgestimed.utilisateur). Les comptes suivants
-- se créent avec l'outil en ligne de commande My_application.outils.GestionComptes.
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
