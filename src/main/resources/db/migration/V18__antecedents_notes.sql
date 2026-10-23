-- =====================================================================
-- GestiMed, migration V18 : antécédents et notes du dossier médical.
--
-- * antecedent : ce que le médecin note une fois pour toutes dans le
--   dossier, hors consultation : antécédents médicaux, chirurgicaux,
--   familiaux, allergies, habitudes de vie, traitement au long cours.
--   « complement » : date, dosage ou commentaire libre (facultatif).
-- * note_dossier : notes libres du médecin, datées.
--
-- Les deux sont rattachées au patient (supprimées avec sa fiche) et au
-- médecin qui les a saisies (gardées si sa fiche disparaît).
-- =====================================================================

CREATE TABLE antecedent (
  idAntecedent    INT            NOT NULL AUTO_INCREMENT,
  idPatient       INT            NOT NULL,
  categorie       VARCHAR(20)    NOT NULL,
  libelle         VARCHAR(200)   NOT NULL,
  complement      VARCHAR(255)   NULL,
  idMedecin       INT            NULL,
  saisiLe         DATETIME       NOT NULL,
  PRIMARY KEY (idAntecedent),
  KEY idx_antecedent_patient (idPatient, categorie),
  KEY idx_antecedent_medecin (idMedecin),
  CONSTRAINT fk_antecedent_patient FOREIGN KEY (idPatient)
    REFERENCES patient (idPatient) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_antecedent_medecin FOREIGN KEY (idMedecin)
    REFERENCES medecin (idMedecin) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT ck_antecedent_categorie CHECK (categorie IN
    ('MEDICAL', 'CHIRURGICAL', 'FAMILIAL', 'ALLERGIE', 'HABITUDE', 'TRAITEMENT')),
  CONSTRAINT ck_antecedent_libelle CHECK (CHAR_LENGTH(TRIM(libelle)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE note_dossier (
  idNote          INT            NOT NULL AUTO_INCREMENT,
  idPatient       INT            NOT NULL,
  texte           VARCHAR(4000)  NOT NULL,
  idMedecin       INT            NULL,
  ecriteLe        DATETIME       NOT NULL,
  PRIMARY KEY (idNote),
  KEY idx_note_patient (idPatient, ecriteLe),
  KEY idx_note_medecin (idMedecin),
  CONSTRAINT fk_note_patient FOREIGN KEY (idPatient)
    REFERENCES patient (idPatient) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_note_medecin FOREIGN KEY (idMedecin)
    REFERENCES medecin (idMedecin) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT ck_note_texte CHECK (CHAR_LENGTH(TRIM(texte)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
