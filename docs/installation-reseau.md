# Installer GestiMed sur plusieurs postes

Le **PC de la secrétaire** est le serveur du cabinet : il héberge MariaDB (XAMPP) et la base `medicale`.
Deux comptes GestiMed y servent : `secretaire` au quotidien et `admin` pour les comptes et les statistiques.
Les **postes médecins** lancent le même GestiMed et se connectent à la base du PC de la secrétaire par le réseau local.

Comptez une heure la première fois. Faites d'abord une sauvegarde (`sql/sauvegarder_base.bat`).

## 1. PC de la secrétaire (serveur)

### 1.1 Adresse IP fixe
Dans l'interface de la box, réservez une adresse pour ce PC (par exemple `192.168.1.10`).
Les postes médecins s'y connecteront ; si elle change, plus rien ne marche.
Réservez aussi une adresse pour chaque poste médecin (par exemple `192.168.1.21`, `192.168.1.22`).

### 1.2 MariaDB démarre avec Windows
Dans le panneau XAMPP lancé **en administrateur**, cochez la case « Svc » à gauche de MySQL :
MariaDB devient un service Windows et tourne dès l'allumage du PC, même si personne n'ouvre XAMPP ni GestiMed.
Désactivez la mise en veille de ce PC (Paramètres > Alimentation) pendant les heures d'ouverture.

### 1.3 Ouvrir MariaDB au réseau du cabinet
Dans `D:\xampp\mysql\bin\my.ini`, section `[mysqld]` : si une ligne `bind-address=127.0.0.1` est active,
remplacez-la par `bind-address=0.0.0.0` (ou mettez-la en commentaire avec `#`). Redémarrez MariaDB.

Autorisez le port 3306 **pour le seul réseau du cabinet** (invite de commandes en administrateur,
adaptez `192.168.1.0/24` à votre réseau) :

```
netsh advfirewall firewall add rule name="GestiMed MariaDB" dir=in action=allow protocol=TCP localport=3306 remoteip=192.168.1.0/24
```

### 1.4 Base et comptes MySQL
1. Mettez la base à jour : `java -cp gestimed-3.5-jar-with-dependencies.jar My_application.outils.MigrationBase migrer`.
2. Si ce n'est pas déjà fait, compte local : `sql/creer_compte_gestimed.sql`.
3. Un compte par poste médecin : modifiez puis exécutez `sql/ouvrir_reseau_local.sql`.
4. Donnez un mot de passe à `root` s'il n'en a pas (XAMPP le laisse vide) : avec la base ouverte au réseau, c'est indispensable.

### 1.5 Comptes GestiMed
```
java -cp gestimed-3.5-jar-with-dependencies.jar My_application.outils.GestionComptes ajouter secretaire secretaire NOM Prenom
java -cp gestimed-3.5-jar-with-dependencies.jar My_application.outils.GestionComptes ajouter admin admin NOM Prenom
java -cp gestimed-3.5-jar-with-dependencies.jar My_application.outils.GestionComptes liste
```
Chaque commande demande le mot de passe au clavier. Un compte médecin indique sa fiche médecin :
`ajouter medecin dr.nom NOM Prenom <idMedecin>`. Pour changer le rôle d'un compte existant :
`GestionComptes role <idUtilisateur> secretaire|medecin|admin [idMedecin]`.

La secrétaire ne voit ni les consultations ni les dossiers médicaux ; l'administrateur voit les membres du cabinet
et ne voit pas les dossiers non plus.

## 2. Chaque poste médecin

1. Installez Java et copiez le jar GestiMed (la même version que sur le PC de la secrétaire).
2. Dans le dossier du jar, créez `gestimed.properties` (ce fichier contient un mot de passe : ne le partagez pas) :

```
url = jdbc:mysql://192.168.1.10:3306/
password = <mot de passe du compte gestimed de ce poste>
```

3. Vérifiez le poste :

```
java -cp gestimed-3.5-jar-with-dependencies.jar My_application.outils.InstallationPoste
```

L'outil affiche l'adresse du serveur, le délai de connexion, la version de la base et les comptes par rôle,
puis « poste prêt » ou la liste des points à corriger. Lancez-le aussi sur le PC de la secrétaire.

## 3. Mises à jour

Après une nouvelle version : sauvegarde, `MigrationBase migrer` sur le PC de la secrétaire, puis **le nouveau jar
sur tous les postes le même jour**. Un poste resté sur l'ancien jar refuse de démarrer tant que sa version ne
correspond pas à la base.

## 4. Sécurité

- Les échanges avec la base ne sont pas chiffrés (MariaDB de XAMPP sans TLS) : gardez les patients et visiteurs
  sur un Wi-Fi invités séparé du réseau des postes.
- Verrouillez la session Windows du PC de la secrétaire en cas d'absence (Windows + L).
- Le mot de passe du compte `admin` n'est connu que de l'administrateur.
- Sauvegarde quotidienne : planifiez `sql/sauvegarder_base.bat` (Planificateur de tâches) vers un disque externe.
