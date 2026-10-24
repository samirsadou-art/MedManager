-- =====================================================================
-- GestiMed : compte MySQL réservé aux sauvegardes (sql/sauvegarder_base.bat).
--
-- À exécuter UNE fois, en root, sur le PC de la secrétaire :
--     mysql -u root -p < sql/creer_compte_sauvegarde.sql
--
-- Remplacer CHANGER_CE_MOT_DE_PASSE avant l'exécution (ne pas l'enregistrer
-- dans git), puis créer à côté du script un fichier sauvegarde.cnf, lisible
-- par le seul compte Windows qui lance la sauvegarde :
--     [client]
--     user=sauvegarde
--     password=<le mot de passe choisi>
-- La sauvegarde planifiée tourne alors sans demander de mot de passe et
-- sans utiliser root.
--
-- Le compte peut seulement lire la base medicale (pas l'écrire), et
-- seulement depuis ce PC.
-- =====================================================================

CREATE USER IF NOT EXISTS 'sauvegarde'@'localhost'
  IDENTIFIED BY 'CHANGER_CE_MOT_DE_PASSE';

GRANT SELECT, SHOW VIEW, TRIGGER, LOCK TABLES, EVENT ON medicale.* TO 'sauvegarde'@'localhost';

FLUSH PRIVILEGES;
