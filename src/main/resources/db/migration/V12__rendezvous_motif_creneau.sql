-- =====================================================================
-- GestiMed, migration V12 : motif des rendez-vous, et un créneau annulé
-- peut être repris.
--
-- * motif : texte court facultatif (contrôle, première visite, résultats…)
--   affiché sur la carte de l'agenda.
-- * Depuis cette version, annuler un rendez-vous ou le marquer absent ne
--   supprime plus la ligne : etat passe à 4 (annulé) ou 5 (absent), ce qui
--   garde l'historique. La clé unique (idMedecin, date, heure) de V9
--   bloquerait alors le créneau pour toujours : elle porte maintenant
--   aussi sur la colonne calculée occupe, qui vaut 1 pour un rendez-vous
--   qui occupe son créneau et NULL sinon (un index unique accepte
--   plusieurs NULL).
-- * La recherche des rendez-vous d'un patient s'appuie sur
--   idx_rendezvous_patient (idPatient, date), créé par V6.
--
-- MariaDB 10.4 : colonne PERSISTENT (stockée) pour pouvoir l'indexer.
-- =====================================================================

ALTER TABLE rendezvous
  ADD COLUMN motif VARCHAR(80) NULL AFTER heure,
  ADD COLUMN occupe TINYINT AS (IF(etat IN (4, 5), NULL, 1)) PERSISTENT;

-- La clé étrangère vers medecin s'appuie sur l'index unique : on la
-- retire le temps de remplacer l'index, comme V9.
ALTER TABLE rendezvous
  DROP FOREIGN KEY fk_rendezvous_medecin;

ALTER TABLE rendezvous
  DROP KEY uk_rendezvous_creneau,
  ADD UNIQUE KEY uk_rendezvous_creneau (idMedecin, `date`, heure, occupe),
  ADD CONSTRAINT fk_rendezvous_medecin FOREIGN KEY (idMedecin)
    REFERENCES medecin (idMedecin) ON DELETE RESTRICT ON UPDATE CASCADE;
