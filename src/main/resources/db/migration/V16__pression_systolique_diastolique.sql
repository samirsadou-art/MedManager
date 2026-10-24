-- =====================================================================
-- GestiMed, migration V16 : pression artérielle en deux colonnes.
--
-- consultation.pression était un texte « systolique/diastolique » saisi
-- tantôt en cmHg (12/8), tantôt en mmHg (120/80) : impossible de trier ou
-- de repérer une hypertension en SQL.
--
-- * pressionSystolique et pressionDiastolique : entiers en mmHg, NULL si
--   non mesurée (les deux ensemble).
-- * Conversion : chaque valeur de 30 ou moins est en cmHg et multipliée
--   par 10 (12/8 -> 120/80, 12/80 -> 120/80).
-- * Une valeur qui reste invraisemblable après conversion (8/12, 400/90…)
--   n'est pas perdue : elle est recopiée telle quelle dans
--   consultation_pression_ancienne, et la consultation passe à NULL.
-- * Le CHECK teste IS NOT NULL : sans cela, une seule valeur NULL rendrait
--   la condition inconnue, ce qu'un CHECK accepte.
-- * Bornes (ConstantesVitales.pression) : systolique 40 à 300,
--   diastolique 20 à 200, systolique > diastolique.
-- =====================================================================

ALTER TABLE consultation
  ADD COLUMN pressionSystolique  SMALLINT NULL DEFAULT NULL AFTER pulsation,
  ADD COLUMN pressionDiastolique SMALLINT NULL DEFAULT NULL AFTER pressionSystolique;

-- 1. Conversion des valeurs au format attendu.
UPDATE consultation
   SET pressionSystolique  = IF(CAST(SUBSTRING_INDEX(pression, '/', 1) AS UNSIGNED) <= 30,
                                10 * CAST(SUBSTRING_INDEX(pression, '/', 1) AS UNSIGNED),
                                CAST(SUBSTRING_INDEX(pression, '/', 1) AS UNSIGNED)),
       pressionDiastolique = IF(CAST(SUBSTRING_INDEX(pression, '/', -1) AS UNSIGNED) <= 30,
                                10 * CAST(SUBSTRING_INDEX(pression, '/', -1) AS UNSIGNED),
                                CAST(SUBSTRING_INDEX(pression, '/', -1) AS UNSIGNED))
 WHERE pression REGEXP '^[0-9]{1,3}/[0-9]{1,3}$';

-- 2. Saisies invraisemblables ou hors format : gardées à part.
CREATE TABLE consultation_pression_ancienne (
  idConsultation INT         NOT NULL,
  pression       VARCHAR(10) NOT NULL,
  PRIMARY KEY (idConsultation),
  CONSTRAINT fk_pression_ancienne_consultation FOREIGN KEY (idConsultation)
    REFERENCES consultation (idConsultation) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO consultation_pression_ancienne (idConsultation, pression)
SELECT idConsultation, pression
  FROM consultation
 WHERE pression IS NOT NULL
   AND NOT (COALESCE(pressionSystolique BETWEEN 40 AND 300, FALSE)
            AND COALESCE(pressionDiastolique BETWEEN 20 AND 200, FALSE)
            AND COALESCE(pressionSystolique > pressionDiastolique, FALSE));

UPDATE consultation
   SET pressionSystolique = NULL, pressionDiastolique = NULL
 WHERE idConsultation IN (SELECT idConsultation FROM consultation_pression_ancienne);

-- 3. L'ancienne colonne disparaît ; les deux nouvelles sont contrôlées.
ALTER TABLE consultation
  DROP CONSTRAINT IF EXISTS ck_consultation_pression;

ALTER TABLE consultation
  DROP COLUMN pression,
  ADD CONSTRAINT ck_consultation_pression CHECK (
    (pressionSystolique IS NULL AND pressionDiastolique IS NULL)
    OR (pressionSystolique IS NOT NULL AND pressionDiastolique IS NOT NULL
        AND pressionSystolique BETWEEN 40 AND 300
        AND pressionDiastolique BETWEEN 20 AND 200
        AND pressionSystolique > pressionDiastolique));
