-- =====================================================================
-- GestiMed, migration V4 : une constante vitale non mesurée vaut NULL.
--
-- Avant : -1 (consultation.NON_RENSEIGNE) et '' pour la pression. Une
-- valeur sentinelle fausse tout calcul fait en SQL (AVG(poids) compte les
-- -1, MIN(temperature) renvoie -1) et oblige chaque requête à l'exclure.
-- NULL est ignoré par les fonctions d'agrégat et dit « non mesuré ».
--
-- Côté Java, l'entité garde NON_RENSEIGNE : JdbcConsultationDao convertit
-- NULL <-> -1 à la lecture et à l'écriture.
-- MariaDB 10.2+ (DROP CONSTRAINT IF EXISTS), voir V3.
-- =====================================================================

-- 1. Retirer les CHECK qui exigent -1 / '' (absents si la base a été
--    restaurée depuis un export phpMyAdmin) et autoriser NULL.
ALTER TABLE consultation
  DROP CONSTRAINT IF EXISTS ck_consultation_temperature,
  DROP CONSTRAINT IF EXISTS ck_consultation_pulsation,
  DROP CONSTRAINT IF EXISTS ck_consultation_respiration,
  DROP CONSTRAINT IF EXISTS ck_consultation_poids,
  DROP CONSTRAINT IF EXISTS ck_consultation_pression,
  MODIFY temperature DECIMAL(4,2) NULL DEFAULT NULL,
  MODIFY pulsation   SMALLINT     NULL DEFAULT NULL,
  MODIFY pression    VARCHAR(10)  NULL DEFAULT NULL,
  MODIFY respiration SMALLINT     NULL DEFAULT NULL,
  MODIFY poids       DECIMAL(5,2) NULL DEFAULT NULL;

-- 2. Convertir les valeurs sentinelles existantes.
UPDATE consultation SET temperature = NULL WHERE temperature = -1;
UPDATE consultation SET pulsation   = NULL WHERE pulsation   = -1;
UPDATE consultation SET respiration = NULL WHERE respiration = -1;
UPDATE consultation SET poids       = NULL WHERE poids       = -1;
UPDATE consultation SET pression    = NULL WHERE TRIM(pression) = '';

-- 3. Bornes de ConstantesVitales ; NULL passe toujours un CHECK.
ALTER TABLE consultation
  ADD CONSTRAINT ck_consultation_temperature CHECK (temperature BETWEEN 25 AND 45),
  ADD CONSTRAINT ck_consultation_pulsation   CHECK (pulsation BETWEEN 1 AND 300),
  ADD CONSTRAINT ck_consultation_respiration CHECK (respiration BETWEEN 1 AND 100),
  ADD CONSTRAINT ck_consultation_poids       CHECK (poids > 0 AND poids < 1000),
  ADD CONSTRAINT ck_consultation_pression    CHECK (pression REGEXP '^[0-9]{1,3}/[0-9]{1,3}$');
