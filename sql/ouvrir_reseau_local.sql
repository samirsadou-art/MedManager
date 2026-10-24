-- =====================================================================
-- GestiMed : comptes MySQL des postes médecins (base sur le PC de la
-- secrétaire, accessible par le réseau local du cabinet).
--
-- À exécuter UNE fois sur le PC de la secrétaire, en administrateur (root),
-- après sql/creer_compte_gestimed.sql :
--     mysql -u root -p < sql/ouvrir_reseau_local.sql
-- puis une fois de plus pour chaque nouveau poste médecin.
--
-- Avant l'exécution :
--   * remplacer 192.168.1.21 / 192.168.1.22 par les adresses IP FIXES des
--     postes médecins (réservées dans la box), et retirer les lignes en trop ;
--   * remplacer CHANGER_CE_MOT_DE_PASSE (ne pas l'enregistrer dans git) et
--     le reporter dans le gestimed.properties de chaque poste médecin.
--
-- Un compte par poste, jamais 'gestimed'@'%' : un appareil inconnu branché
-- sur le réseau du cabinet (Wi-Fi invités, téléphone) ne peut pas se
-- connecter. Mêmes droits que le compte local : lecture et écriture des
-- données de la base medicale, rien d'autre.
--
-- Chiffrement : la MariaDB de XAMPP n'a pas de TLS ; les échanges circulent
-- en clair sur le réseau du cabinet. Le jour où TLS est configuré, ajouter
-- REQUIRE SSL ici et sslMode=REQUIRED dans gestimed.properties.
-- Voir docs/installation-reseau.md.
-- =====================================================================

CREATE USER IF NOT EXISTS 'gestimed'@'192.168.1.21'
  IDENTIFIED BY 'CHANGER_CE_MOT_DE_PASSE';
GRANT SELECT, INSERT, UPDATE, DELETE ON medicale.* TO 'gestimed'@'192.168.1.21';

CREATE USER IF NOT EXISTS 'gestimed'@'192.168.1.22'
  IDENTIFIED BY 'CHANGER_CE_MOT_DE_PASSE';
GRANT SELECT, INSERT, UPDATE, DELETE ON medicale.* TO 'gestimed'@'192.168.1.22';

FLUSH PRIVILEGES;

-- Contrôle : aucun compte ne doit être ouvert à tout le réseau ('%'), et
-- root ne doit exister que pour localhost / 127.0.0.1 / ::1.
SELECT user, host FROM mysql.user ORDER BY user, host;
