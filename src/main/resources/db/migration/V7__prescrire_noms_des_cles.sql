-- =====================================================================
-- GestiMed, migration V7 : la table prescrire nomme ses clés comme le
-- reste de la base.
--
--   prescrire.idMed    -> idMedicament   (comme medicaments.idMedicament)
--   prescrire.idConsul -> idConsultation (comme consultation.idConsultation)
--
-- Clé primaire, index et clés étrangères suivent le renommage (InnoDB).
-- MariaDB 10.4 n'a pas RENAME COLUMN : CHANGE reprend la définition.
-- =====================================================================

ALTER TABLE prescrire
  CHANGE idMed    idMedicament   INT NOT NULL,
  CHANGE idConsul idConsultation INT NOT NULL;
