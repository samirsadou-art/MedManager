-- =====================================================================
-- GestiMed, migration V14 : messagerie interne du cabinet.
--
-- Remplace l'ancien chat TCP (port 7777, sans authentification) : les
-- messages ne passent que par la base, avec le compte gestimed.
--
-- * message_interne : idExpediteur NULL pour un message du système,
--   idDestinataire NULL pour le fil « Tout le cabinet ». Un message peut
--   citer un patient (lien vers sa fiche administrative) ; si la fiche est
--   supprimée, le message reste sans le lien.
-- * message_lu : qui a lu quel message (accusés de lecture, non-lus).
--   Supprimé avec le message : l'application efface les messages de plus
--   de 12 mois.
-- * utilisateur_presence : dernier signe de vie de chaque membre connecté
--   (toutes les 30 s), à l'heure du serveur pour ne pas dépendre de
--   l'horloge des postes.
-- =====================================================================

CREATE TABLE message_interne (
  idMessage       INT            NOT NULL AUTO_INCREMENT,
  idExpediteur    INT            NULL,
  idDestinataire  INT            NULL,
  texte           VARCHAR(2000)  NOT NULL,
  idPatient       INT            NULL,
  envoyeLe        DATETIME       NOT NULL,
  PRIMARY KEY (idMessage),
  KEY idx_message_destinataire (idDestinataire, idMessage),
  KEY idx_message_expediteur (idExpediteur, idMessage),
  KEY idx_message_envoye (envoyeLe),
  CONSTRAINT fk_message_expediteur FOREIGN KEY (idExpediteur)
    REFERENCES utilisateur (idUtilisateur) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_message_destinataire FOREIGN KEY (idDestinataire)
    REFERENCES utilisateur (idUtilisateur) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_message_patient FOREIGN KEY (idPatient)
    REFERENCES patient (idPatient) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT ck_message_texte CHECK (CHAR_LENGTH(TRIM(texte)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE message_lu (
  idMessage       INT            NOT NULL,
  idUtilisateur   INT            NOT NULL,
  luLe            DATETIME       NOT NULL,
  PRIMARY KEY (idMessage, idUtilisateur),
  KEY idx_message_lu_utilisateur (idUtilisateur),
  CONSTRAINT fk_message_lu_message FOREIGN KEY (idMessage)
    REFERENCES message_interne (idMessage) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_message_lu_utilisateur FOREIGN KEY (idUtilisateur)
    REFERENCES utilisateur (idUtilisateur) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE utilisateur_presence (
  idUtilisateur   INT            NOT NULL,
  poste           VARCHAR(60)    NULL,
  vuLe            DATETIME       NOT NULL,
  PRIMARY KEY (idUtilisateur),
  CONSTRAINT fk_presence_utilisateur FOREIGN KEY (idUtilisateur)
    REFERENCES utilisateur (idUtilisateur) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
