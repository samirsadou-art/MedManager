-- =====================================================================
-- GestiMed, migration V11 : inscription des patients par la secrétaire.
--
-- * Téléphones au format national, chiffres seuls (PatientService
--   .normaliserTelephone) : la recherche par téléphone et la détection des
--   doublons comparent des chiffres. Les espaces, points, tirets et
--   parenthèses déjà saisis sont retirés ; un numéro vide devient NULL.
--   (L'indicatif +213 éventuel est laissé tel quel : la fiche le corrige à
--   sa prochaine modification.)
-- * Index pour la détection des doublons (même nom et même date de
--   naissance : idx_patient_nom couvre déjà le nom) et pour la recherche par
--   téléphone.
-- =====================================================================

UPDATE patient
   SET telPatient = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
         telPatient, ' ', ''), '.', ''), '-', ''), '(', ''), ')', ''), '/', '')
 WHERE telPatient IS NOT NULL;

UPDATE patient
   SET telPatient = NULL
 WHERE telPatient = '';

ALTER TABLE patient
  ADD KEY idx_patient_naissance (date_Nais_Patient),
  ADD KEY idx_patient_telephone (telPatient);
