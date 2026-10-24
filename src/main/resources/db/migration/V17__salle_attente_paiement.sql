-- =====================================================================
-- GestiMed, migration V17 : frais de consultation et sortie du cabinet.
--
-- Avant, enregistrer la consultation faisait sortir le patient de la
-- salle d'attente. Désormais :
-- 1. le médecin indique les frais en terminant la consultation ; le
--    passage prend l'état 4 « consultation terminée, à régler » ;
-- 2. la secrétaire encaisse et libère le patient : état 2 « terminé »,
--    heure de sortie, montant reçu.
--
-- * frais : montant demandé par le médecin, en dinars (NULL : non précisé).
-- * montantRegle / regleLe : ce que la secrétaire a encaissé et quand
--   (NULL : rien encaissé, par exemple une consultation gratuite).
-- * present vaut aussi 1 dans l'état 4 : le patient est encore au cabinet,
--   il ne peut pas être pointé une seconde fois avant d'être libéré.
--   La colonne calculée est recréée (MariaDB 10.4 ne modifie pas son
--   expression en place quand un index l'utilise). La clé étrangère vers
--   patient s'appuyait sur cet index unique : elle reçoit d'abord son
--   propre index.
-- =====================================================================

ALTER TABLE salle_attente
  ADD COLUMN frais        INT      NULL DEFAULT NULL AFTER priorite,
  ADD COLUMN montantRegle INT      NULL DEFAULT NULL AFTER frais,
  ADD COLUMN regleLe      DATETIME NULL DEFAULT NULL AFTER montantRegle;

ALTER TABLE salle_attente DROP CONSTRAINT IF EXISTS ck_salle_attente_etat;
ALTER TABLE salle_attente
  ADD CONSTRAINT ck_salle_attente_etat CHECK (etat BETWEEN 0 AND 4),
  ADD CONSTRAINT ck_salle_attente_frais CHECK (frais IS NULL OR frais BETWEEN 0 AND 1000000),
  ADD CONSTRAINT ck_salle_attente_regle CHECK (montantRegle IS NULL OR montantRegle BETWEEN 0 AND 1000000);

ALTER TABLE salle_attente ADD KEY idx_salle_attente_patient (idPatient);
ALTER TABLE salle_attente DROP INDEX uk_salle_attente_present;
ALTER TABLE salle_attente DROP COLUMN present;
ALTER TABLE salle_attente
  ADD COLUMN present TINYINT AS (IF(etat IN (0, 1, 4), 1, NULL)) PERSISTENT AFTER etat,
  ADD UNIQUE KEY uk_salle_attente_present (idPatient, jour, present);
