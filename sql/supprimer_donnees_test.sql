-- =====================================================================
-- GestiMed : suppression des données de test créées par sql/donnees_test.sql
-- (patients et consultations 9001 à 9999). Les autres données ne sont pas
-- touchées.
--   D:/xampp/mysql/bin/mysql.exe -u root --default-character-set=utf8mb4 < sql/supprimer_donnees_test.sql
-- =====================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

USE medicale;

START TRANSACTION;

DELETE FROM rendezvous   WHERE idPatient BETWEEN 9001 AND 9999;
DELETE FROM consultation WHERE idConsultation BETWEEN 9001 AND 9999
                            OR idPatient BETWEEN 9001 AND 9999;
DELETE FROM patient      WHERE idPatient BETWEEN 9001 AND 9999;

COMMIT;

-- Ramène les compteurs au plus grand identifiant restant + 1, pour que les
-- vrais patients et consultations ne continuent pas après 9014.
ALTER TABLE patient      AUTO_INCREMENT = 1;
ALTER TABLE consultation AUTO_INCREMENT = 1;
