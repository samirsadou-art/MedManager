-- =====================================================================
-- GestiMed, migration V15 : numéro de dossier séparé de idPatient.
--
-- Jusqu'ici le numéro de dossier saisi à l'inscription était écrit dans
-- idPatient, la clé auto-incrémentée : un numéro saisi à la main (par
-- exemple celui d'un ancien dossier papier) pouvait prendre la place d'un
-- numéro que l'auto-incrément attribuerait plus tard, ou le faire sauter.
--
-- * patient.numeroDossier : le numéro connu du cabinet, unique, chiffres,
--   lettres et / - . (ex. 1254, 2019/37, A-12). idPatient reste la clé
--   technique, référencée par les autres tables, et n'est plus jamais saisi.
-- * Les dossiers existants gardent leur numéro : numeroDossier = idPatient.
-- * Un dossier inscrit sans numéro reçoit le plus grand numéro purement
--   numérique + 1 (JdbcPatientDao.prochainNumeroDossier).
-- * La collation utf8mb4_unicode_ci rend l'unicité insensible à la casse ;
--   PatientService enregistre de toute façon le numéro en majuscules.
-- =====================================================================

ALTER TABLE patient
  ADD COLUMN numeroDossier VARCHAR(20) NULL AFTER idPatient;

UPDATE patient SET numeroDossier = CAST(idPatient AS CHAR);

ALTER TABLE patient
  MODIFY numeroDossier VARCHAR(20) NOT NULL,
  ADD CONSTRAINT uk_patient_numero_dossier UNIQUE (numeroDossier),
  ADD CONSTRAINT ck_patient_numero_dossier CHECK (numeroDossier REGEXP '^[0-9A-Za-z/.-]{1,20}$');
