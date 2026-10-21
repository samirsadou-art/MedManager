-- =====================================================================
-- GestiMed : compte MySQL dédié à l'application.
--
-- À exécuter UNE fois, en administrateur (root), après sql/medicale.sql :
--     mysql -u root -p < sql/creer_compte_gestimed.sql
--
-- Remplacer CHANGER_CE_MOT_DE_PASSE avant l'exécution (ne pas enregistrer
-- le vrai mot de passe dans git), puis le reporter dans le fichier local
-- gestimed.properties (git-ignoré) :
--     password = <le mot de passe choisi>
--
-- Le compte ne peut que lire et écrire les données de la base medicale :
-- ni DROP, ni ALTER, ni CREATE, ni accès aux autres bases. Il n'est
-- utilisable que depuis le poste du serveur (localhost).
--
-- Base sur un autre poste du cabinet : créer plutôt un compte par poste
-- client, jamais 'gestimed'@'%', en exigeant le chiffrement, et passer
-- sslMode=REQUIRED dans gestimed.properties :
--     CREATE USER 'gestimed'@'192.168.1.20' IDENTIFIED BY '...' REQUIRE SSL;
--     GRANT SELECT, INSERT, UPDATE, DELETE ON medicale.* TO 'gestimed'@'192.168.1.20';
-- (REQUIRE SSL suppose un serveur configuré pour TLS : c'est automatique
-- avec MySQL 8, à configurer à la main avec MariaDB.)
-- =====================================================================

CREATE USER IF NOT EXISTS 'gestimed'@'localhost'
  IDENTIFIED BY 'CHANGER_CE_MOT_DE_PASSE';

GRANT SELECT, INSERT, UPDATE, DELETE ON medicale.* TO 'gestimed'@'localhost';

FLUSH PRIVILEGES;
