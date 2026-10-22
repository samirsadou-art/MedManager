-- =====================================================================
-- GestiMed, migration V13 : salle d'attente et événements entre postes.
--
-- * salle_attente : un passage par patient arrivé au cabinet, avec ou
--   sans rendez-vous. etat : 0 en attente, 1 en consultation, 2 terminé,
--   3 parti sans consultation (constantes Passage.*). priorite : 1 pour
--   une urgence remontée par la secrétaire.
--   Un patient n'est qu'une fois à la fois dans la salle : la colonne
--   calculée present vaut 1 tant qu'il attend ou est en consultation, NULL
--   ensuite (un index unique accepte plusieurs NULL). Un rendez-vous ne
--   donne qu'un passage.
-- * evenement : une ligne par changement (arrivée, appel, rendez-vous…).
--   Chaque poste relit toutes les 3 secondes le dernier identifiant et ne
--   recharge ses écrans que s'il a changé. Les lignes de plus de 7 jours
--   sont supprimées par l'application.
-- =====================================================================

CREATE TABLE salle_attente (
  idPassage     INT          NOT NULL AUTO_INCREMENT,
  idPatient     INT          NOT NULL,
  idMedecin     INT          NOT NULL,
  idRendezVous  INT          NULL,
  jour          DATE         NOT NULL,
  arriveeLe     DATETIME     NOT NULL,
  appeleLe      DATETIME     NULL,
  sortieLe      DATETIME     NULL,
  priorite      TINYINT      NOT NULL DEFAULT 0,
  etat          TINYINT      NOT NULL DEFAULT 0,
  present       TINYINT      AS (IF(etat IN (0, 1), 1, NULL)) PERSISTENT,
  PRIMARY KEY (idPassage),
  UNIQUE KEY uk_salle_attente_present (idPatient, jour, present),
  UNIQUE KEY uk_salle_attente_rendezvous (idRendezVous),
  KEY idx_salle_attente_jour (jour, idMedecin),
  KEY idx_salle_attente_medecin (idMedecin),
  CONSTRAINT fk_salle_attente_patient FOREIGN KEY (idPatient)
    REFERENCES patient (idPatient) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_salle_attente_medecin FOREIGN KEY (idMedecin)
    REFERENCES medecin (idMedecin) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_salle_attente_rendezvous FOREIGN KEY (idRendezVous)
    REFERENCES rendezvous (idRendezVous) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT ck_salle_attente_etat CHECK (etat BETWEEN 0 AND 3),
  CONSTRAINT ck_salle_attente_priorite CHECK (priorite IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE evenement (
  idEvenement   BIGINT       NOT NULL AUTO_INCREMENT,
  type          VARCHAR(30)  NOT NULL,
  idObjet       INT          NULL,
  creeLe        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (idEvenement),
  KEY idx_evenement_cree (creeLe)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
