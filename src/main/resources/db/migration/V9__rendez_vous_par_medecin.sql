-- =====================================================================
-- GestiMed, migration V9 : un créneau est réservé par médecin.
--
-- Depuis cette version, l'application enregistre le médecin de chaque
-- rendez-vous (celui du poste, Session.getIdMedecin). Le médecin devient
-- obligatoire et l'unicité passe de (date, heure) à (idMedecin, date,
-- heure) : deux médecins du cabinet peuvent recevoir au même moment.
--
-- Les rendez-vous encore sans médecin (pris par un utilisateur sans
-- médecin, dans un cabinet qui en a plusieurs) sont rattachés au premier
-- médecin enregistré.
-- =====================================================================

UPDATE rendezvous
   SET idMedecin = (SELECT MIN(idMedecin) FROM medecin)
 WHERE idMedecin IS NULL;

-- L'index (idMedecin, date) de V5 sert la clé étrangère ; le nouvel index
-- unique commence aussi par idMedecin et le remplace.
ALTER TABLE rendezvous
  DROP FOREIGN KEY fk_rendezvous_medecin;

ALTER TABLE rendezvous
  MODIFY idMedecin INT NOT NULL,
  DROP KEY uk_rendezvous_creneau,
  DROP KEY idx_rendezvous_medecin,
  ADD UNIQUE KEY uk_rendezvous_creneau (idMedecin, `date`, heure),
  ADD KEY idx_rendezvous_date (`date`, heure),
  ADD CONSTRAINT fk_rendezvous_medecin FOREIGN KEY (idMedecin)
    REFERENCES medecin (idMedecin) ON DELETE RESTRICT ON UPDATE CASCADE;
