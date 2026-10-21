-- =====================================================================
-- GestiMed, migration V6 : correction des noms mal orthographiés.
--
--   medecin.idMedcin     -> idMedecin   (comme consultation.idMedecin)
--   medecin.prenomMedcin -> prenomMedecin
--   rondezvous           -> rendezvous
--   demandanalyse        -> demandeanalyse
--
-- ATTENTION : après cette migration, une version de GestiMed antérieure ne
-- trouve plus ces tables et colonnes. Mettre à jour l'application sur tous
-- les postes du cabinet en même temps que la base.
--
-- MariaDB 10.4 n'a pas RENAME COLUMN ni RENAME INDEX (10.5+) : CHANGE
-- reprend la définition complète, et les index sont recréés.
-- Les clés étrangères qui pointent vers medecin suivent le renommage.
-- =====================================================================

ALTER TABLE medecin
  CHANGE idMedcin     idMedecin     INT         NOT NULL AUTO_INCREMENT,
  CHANGE prenomMedcin prenomMedecin VARCHAR(60) NOT NULL;

RENAME TABLE rondezvous TO rendezvous, demandanalyse TO demandeanalyse;

-- Noms des contraintes et index alignés sur les nouveaux noms de tables.
ALTER TABLE rendezvous
  DROP FOREIGN KEY fk_rondezvous_patient,
  DROP FOREIGN KEY fk_rondezvous_utilisateur;

ALTER TABLE rendezvous
  DROP KEY idx_rondezvous_patient,
  DROP KEY idx_rondezvous_utilisateur,
  ADD KEY idx_rendezvous_patient (idPatient, `date`),
  ADD KEY idx_rendezvous_utilisateur (idUser),
  ADD CONSTRAINT fk_rendezvous_patient FOREIGN KEY (idPatient)
    REFERENCES patient (idPatient) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_rendezvous_utilisateur FOREIGN KEY (idUser)
    REFERENCES utilisateur (idUtilisateur) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE demandeanalyse
  DROP FOREIGN KEY fk_demandanalyse_consultation,
  DROP FOREIGN KEY fk_demandanalyse_analyse,
  DROP CONSTRAINT IF EXISTS ck_demandanalyse_note;

ALTER TABLE demandeanalyse
  DROP KEY idx_demandanalyse_analyse,
  ADD KEY idx_demandeanalyse_analyse (idAnalyse),
  ADD CONSTRAINT fk_demandeanalyse_consultation FOREIGN KEY (idconsultation)
    REFERENCES consultation (idConsultation) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT fk_demandeanalyse_analyse FOREIGN KEY (idAnalyse)
    REFERENCES analyse (idanalyse) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT ck_demandeanalyse_note CHECK (note BETWEEN 0 AND 4);
