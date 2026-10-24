-- =====================================================================
-- GestiMed, migration V5 : chaque rendez-vous a un identifiant, un médecin
-- et un état utilisable par l'agenda.
--
-- * idRendezVous : clé technique. L'ancienne clé (date, heure) devient une
--   contrainte UNIQUE : toujours un seul rendez-vous par créneau.
--   Quand l'application renseignera le médecin à chaque prise de
--   rendez-vous, une migration pourra remplacer cette contrainte par
--   (idMedecin, date, heure) pour que deux médecins reçoivent en même temps.
-- * idMedecin : médecin qui reçoit le patient. NULL tant que l'application
--   ne le renseigne pas. Les rendez-vous existants reçoivent le médecin de
--   l'utilisateur qui les a pris, sinon le médecin du cabinet s'il n'y en a
--   qu'un.
-- * etat (déjà présent, toujours 0 jusqu'ici) : codes de Rendez_Vous.ETAT_*
--   0 prévu, 1 arrivé (salle d'attente), 2 en consultation, 3 terminé,
--   4 annulé, 5 absent.
-- * nom de table : la faute « rondezvous » est corrigée par V6.
-- =====================================================================

ALTER TABLE rondezvous
  DROP PRIMARY KEY,
  ADD COLUMN idRendezVous INT NOT NULL AUTO_INCREMENT FIRST,
  ADD PRIMARY KEY (idRendezVous),
  ADD UNIQUE KEY uk_rendezvous_creneau (`date`, heure),
  ADD COLUMN idMedecin INT NULL AFTER idPatient,
  ADD KEY idx_rendezvous_medecin (idMedecin, `date`),
  ADD CONSTRAINT fk_rendezvous_medecin FOREIGN KEY (idMedecin)
    REFERENCES medecin (idMedcin) ON DELETE RESTRICT ON UPDATE CASCADE;

UPDATE rondezvous r JOIN utilisateur u ON u.idUtilisateur = r.idUser
   SET r.idMedecin = u.idMedecin
 WHERE r.idMedecin IS NULL;

UPDATE rondezvous
   SET idMedecin = (SELECT MIN(idMedcin) FROM medecin)
 WHERE idMedecin IS NULL
   AND (SELECT COUNT(*) FROM medecin) = 1;

ALTER TABLE rondezvous
  DROP CONSTRAINT IF EXISTS ck_rendezvous_etat,
  ADD CONSTRAINT ck_rendezvous_etat CHECK (etat BETWEEN 0 AND 5);
