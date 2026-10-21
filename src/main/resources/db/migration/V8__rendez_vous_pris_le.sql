-- =====================================================================
-- GestiMed, migration V8 : moment de la prise d'un rendez-vous en une
-- seule colonne.
--
-- dateFix + heureFix (DATE et TIME séparés, NULL possibles) deviennent
-- prisLe DATETIME, rempli par le serveur si l'application ne le donne pas.
-- Un ancien rendez-vous sans date de prise reçoit celle du rendez-vous.
-- L'entité Rendez_Vous garde dateFix / heureFix : JdbcRendezVousDao fait
-- la conversion.
-- =====================================================================

ALTER TABLE rendezvous ADD COLUMN prisLe DATETIME NULL AFTER heure;

UPDATE rendezvous
   SET prisLe = COALESCE(TIMESTAMP(dateFix, COALESCE(heureFix, '00:00:00')), TIMESTAMP(`date`, heure));

ALTER TABLE rendezvous
  MODIFY prisLe DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  DROP COLUMN dateFix,
  DROP COLUMN heureFix;
