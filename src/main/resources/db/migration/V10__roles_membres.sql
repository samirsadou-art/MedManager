-- =====================================================================
-- GestiMed, migration V10 : rôle de chaque compte (espace secrétaire).
--
-- * utilisateur.role : SECRETAIRE, MEDECIN ou ADMIN (entities.Role). Il
--   décide de l'écran d'accueil ; les services refusent le dossier médical
--   à la secrétaire et à l'administrateur.
--   Reprise des comptes existants : jusqu'ici tout le monde avait accès à
--   tout, donc chacun devient MEDECIN, sauf un compte sans médecin rattaché
--   dont la qualité contient « secr » (secrétaire). Le compte administrateur
--   se crée ensuite avec l'outil GestionComptes (commande ajouter).
-- * utilisateur.doitChanger : mot de passe provisoire à changer à la
--   prochaine connexion (gestion des comptes par l'administrateur).
-- * utilisateur.derniereConnexion : notée à chaque connexion réussie.
-- * medecin.actif : un médecin qui quitte le cabinet est désactivé, jamais
--   supprimé (ses rendez-vous et consultations le référencent).
-- =====================================================================

ALTER TABLE utilisateur
  ADD COLUMN role              VARCHAR(20) NOT NULL DEFAULT 'MEDECIN' AFTER qualite,
  ADD COLUMN doitChanger       TINYINT(1)  NOT NULL DEFAULT 0 AFTER motDePasse,
  ADD COLUMN derniereConnexion DATETIME    NULL AFTER actif;

UPDATE utilisateur
   SET role = 'SECRETAIRE'
 WHERE idMedecin IS NULL
   AND LOWER(qualite) LIKE '%secr%';

ALTER TABLE utilisateur
  DROP CONSTRAINT IF EXISTS ck_utilisateur_role,
  ADD CONSTRAINT ck_utilisateur_role CHECK (role IN ('SECRETAIRE', 'MEDECIN', 'ADMIN'));

ALTER TABLE medecin
  ADD COLUMN actif TINYINT(1) NOT NULL DEFAULT 1 AFTER nomCabinet;
