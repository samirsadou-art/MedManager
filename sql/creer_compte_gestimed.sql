-- =====================================================================
-- GestiMed : compte MySQL dédié à l'application.
--
-- À exécuter UNE fois, en administrateur (root), après la création du
-- schéma (outil My_application.outils.MigrationBase migrer) :
--     mysql -u root -p < sql/creer_compte_gestimed.sql
--
-- Remplacer CHANGER_CE_MOT_DE_PASSE avant l'exécution (ne pas enregistrer
-- le vrai mot de passe dans git), puis le reporter dans le fichier local
-- gestimed.properties (git-ignoré) :
--     password = <le mot de passe choisi>
--
-- Le compte ne peut que lire et écrire les données de la base medicale :
-- ni DROP, ni ALTER, ni CREATE, ni accès aux autres bases. Les évolutions
-- du schéma passent donc par MigrationBase, lancé avec le compte root.
-- Il n'est utilisable que depuis le poste du serveur (localhost).
--
-- Postes médecins qui se connectent à cette base par le réseau local :
-- un compte par poste, créé avec sql/ouvrir_reseau_local.sql (jamais
-- 'gestimed'@'%'). Voir docs/installation-reseau.md.
-- =====================================================================

CREATE USER IF NOT EXISTS 'gestimed'@'localhost'
  IDENTIFIED BY 'CHANGER_CE_MOT_DE_PASSE';

GRANT SELECT, INSERT, UPDATE, DELETE ON medicale.* TO 'gestimed'@'localhost';

FLUSH PRIVILEGES;
