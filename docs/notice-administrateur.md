# GestiMed : notice de l'administrateur

Le compte `admin` gère les membres du cabinet et consulte les statistiques. Il ne voit ni les dossiers médicaux ni
les consultations. Son mot de passe n'est connu que de l'administrateur.

Après la connexion, le menu de gauche propose **Membres** et **Statistiques**.

## Membres du cabinet

La carte **Comptes de connexion** liste chaque membre avec :
- son rôle (Secrétaire, Médecin, Administrateur) et son identifiant ;
- le médecin rattaché ;
- sa dernière connexion ;
- son mot de passe : « À changer » tant qu'il n'a pas remplacé le mot de passe provisoire ;
- son état : actif ou désactivé.

### Ajouter un membre

1. Cliquez sur **Ajouter un membre…**.
2. Saisissez le nom, le prénom et le rôle.
3. **Pour un médecin**, choisissez sa fiche médecin. S'il arrive au cabinet, choisissez **Nouvelle fiche** et
   indiquez son numéro d'ordre et sa spécialité : sa fiche est créée en même temps que son compte.
4. Choisissez son identifiant (lettres sans accent, chiffres, point, tiret) et un **mot de passe provisoire**
   (8 caractères au moins, dont une lettre et un chiffre).
5. Communiquez-lui le mot de passe de vive voix, pas par écrit. Il en choisira un autre à sa première connexion.

### Les autres actions

Sélectionnez d'abord la ligne du membre.

- **Modifier…** change son nom, son prénom, son rôle ou sa fiche médecin. L'identifiant ne change pas.
- **Mot de passe provisoire…** sert quand un membre a oublié son mot de passe : il devra en choisir un nouveau à sa
  prochaine connexion.
- **Désactiver** bloque la connexion d'un membre qui quitte le cabinet. Son historique est conservé, et
  **Activer** le rétablit.
- Le cabinet garde toujours **au moins un administrateur actif** : GestiMed refuse de désactiver ou de changer le
  rôle du dernier.

### Fiches médecin

La carte **Fiches médecin** liste les médecins du cabinet.

- **Modifier la fiche…** change le titre, le nom, le numéro d'ordre, la spécialité, l'adresse, les téléphones,
  l'e-mail et le nom du cabinet. Ces informations sont imprimées en tête des ordonnances et des demandes
  d'analyses.
- **Désactiver** retire de l'agenda un médecin qui quitte le cabinet. Ses consultations et son historique restent.
  GestiMed refuse tant qu'il a des **rendez-vous à venir** (déplacez-les ou annulez-les d'abord) ou un **compte
  actif** (désactivez-le d'abord).

Chaque action sur les comptes et les fiches est inscrite au journal des accès.

## Statistiques

1. Choisissez une **période** (aujourd'hui, cette semaine, ce mois-ci, le mois dernier, les 3 derniers mois, cette
   année, l'année dernière), ou des dates précises dans les champs **du** et **au**.
2. Choisissez un **médecin**, ou **Tous les médecins**.

La page affiche :
- **les chiffres clés :**
  - les consultations ;
  - les patients vus, dont les nouveaux, c'est-à-dire ceux qui consultent pour la première fois au cabinet ;
  - le taux d'absence : absents parmi les rendez-vous passés ;
  - l'attente moyenne en salle, entre l'arrivée et l'appel ;
- **les consultations dans le temps**, par jour, par semaine ou par mois selon la longueur de la période. Survolez
  une barre pour voir le détail ;
- **les rendez-vous** honorés, absents, annulés et encore prévus ;
- **les consultations par médecin.**

**Exporter en PDF** ouvre un document imprimable. **Exporter pour Excel (CSV)** enregistre un fichier qui s'ouvre
directement dans Excel.

Les statistiques ne contiennent que des nombres, jamais de nom de patient.

## Sauvegardes

Le PC de la secrétaire contient **toute** la base du cabinet : une sauvegarde quotidienne est indispensable.

- **Sauvegarde manuelle :** lancez `sql\sauvegarder_base.bat E:\Sauvegardes` (adaptez la lettre du disque externe).
- **Sauvegarde automatique :** suivez la section « Sauvegarde quotidienne » de `docs/installation-reseau.md`.
- **Restauration :**
  ```
  <xampp>\mysql\bin\mysql.exe -u root -p medicale < E:\Sauvegardes\sauvegarde_medicale_AAAA-MM-JJ_HHMM.sql
  ```

N'utilisez pas l'export de phpMyAdmin : il perd des contraintes de la base.

## Mises à jour de GestiMed

1. Faites une sauvegarde.
2. Sur le PC de la secrétaire, mettez la base à jour :
   ```
   java -cp gestimed-3.5-jar-with-dependencies.jar My_application.outils.MigrationBase migrer
   ```
3. Copiez le nouveau jar sur **tous les postes le même jour**. Un poste resté sur l'ancien jar refuse de démarrer.
4. Lancez `My_application.outils.InstallationPoste` sur chaque poste : il doit afficher « poste prêt ».

## Journal des accès

Chaque ouverture ou modification d'un dossier patient est notée : qui, quand, depuis quel poste. Les connexions,
les échecs de connexion et les changements de comptes sont notés aussi, dans la table `journal_acces`.

Pour savoir qui a consulté le dossier d'un patient, indiquez son identifiant interne (`idPatient` dans la base) :
```
java -cp gestimed-3.5-jar-with-dependencies.jar My_application.outils.GestionComptes journal <idPatient>
```
Pour les dossiers inscrits avant la version 15 de la base, l'identifiant interne est le numéro de dossier. Pour les
autres, il peut en différer : retrouvez-le avec
`SELECT idPatient FROM patient WHERE numeroDossier = '<n° de dossier>'`.
