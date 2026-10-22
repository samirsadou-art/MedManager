# Recette de GestiMed au cabinet

Ce scénario vérifie la version secrétaire / médecin / administrateur sur les vrais postes du cabinet. Il prend
environ une heure à deux personnes : l'une au **PC de la secrétaire** (S), l'autre à un **poste médecin** (M).
Cochez chaque ligne ; notez à côté tout écart, avec l'heure, pour retrouver le détail dans
`C:\Users\<nom>\GestiMed\gestimed0.log`.

Faites-le de préférence **avant l'ouverture**, avec deux patients d'essai que vous supprimerez à la fin
(`sql/supprimer_donnees_test.sql` ne retire que les données de `sql/donnees_test.sql`).

## 0. Préparation (PC de la secrétaire)

- [ ] Sauvegarde : `sql\sauvegarder_base.bat E:\Sauvegardes` ; le fichier `.sql` fait plus de quelques Ko.
- [ ] Mise à jour de la base : `java -cp gestimed-3.5-jar-with-dependencies.jar My_application.outils.MigrationBase migrer`
      se termine par la version 14 (une base en version 9 reçoit V10 à V14).
- [ ] Le nouveau jar est copié sur le PC de la secrétaire **et** sur chaque poste médecin.
- [ ] `My_application.outils.InstallationPoste` affiche « poste prêt » sur chaque poste.
- [ ] Les comptes existent : un `admin`, un `secretaire`, un compte par médecin (`GestionComptes liste`).

## 1. Administrateur (S, compte `admin`)

- [ ] La connexion ouvre la page **Membres** ; le menu propose **Membres** et **Statistiques**, rien d'autre.
- [ ] **Ajouter un membre…** : une secrétaire d'essai `essai.sec` avec un mot de passe provisoire. Elle apparaît
      avec « À changer ».
- [ ] **Mot de passe provisoire…** sur un médecin : il apparaît « À changer ».
- [ ] **Désactiver** le compte `admin` lui-même alors qu'il est le seul administrateur : refusé (« dernier compte
      administrateur actif »).
- [ ] **Modifier la fiche…** d'un médecin : changez la spécialité, puis remettez-la.
- [ ] **Statistiques** : « Ce mois-ci » affiche des nombres cohérents avec l'activité du mois ; **Exporter en PDF**
      ouvre le document ; **Exporter pour Excel (CSV)** s'ouvre dans Excel avec les accents corrects.

## 2. Première connexion avec un mot de passe provisoire

- [ ] (S) Connexion avec `essai.sec` : GestiMed demande un nouveau mot de passe avant d'ouvrir l'accueil.
- [ ] Le même mot de passe que le provisoire est refusé ; un mot de passe de 5 caractères est refusé.
- [ ] **Annuler** ramène à l'écran de connexion sans connecter.
- [ ] Un nouveau mot de passe valide ouvre l'accueil. Dans la page Membres (admin), « À changer » est devenu
      « Choisi » et la dernière connexion est renseignée.
- [ ] **Fichier > Changer mon mot de passe…** : un mauvais mot de passe actuel est refusé ; un bon le change.

## 3. Inscription d'un patient (S, compte `secretaire`)

- [ ] L'accueil n'affiche pas les consultations du jour ; le menu propose Accueil, Patients, Rendez-vous,
      Messagerie.
- [ ] **Patients > Nouveau patient** sans téléphone : refusé. Avec `+213 555 12 34 56` : enregistré en `0555123456`.
- [ ] Une deuxième fiche avec le même nom et la même date de naissance : GestiMed propose la fiche existante.
- [ ] La recherche par `0555` retrouve le patient.
- [ ] Choix de la suite **Prendre un rendez-vous** : l'agenda s'ouvre pour ce patient.

## 4. Rendez-vous (S puis M)

- [ ] (S) Sans choisir de médecin (cabinet à plusieurs médecins), la prise de rendez-vous est refusée avec un
      message clair.
- [ ] (S) Rendez-vous aujourd'hui, à l'heure qui suit, avec le médecin du poste M.
- [ ] (M) Dans les 3 secondes, le rendez-vous apparaît dans la liste du jour du médecin.
- [ ] (S) **Déplacer** le rendez-vous d'une heure, puis le remettre.
- [ ] (S) Un deuxième rendez-vous d'essai : **Annuler le rendez-vous** ; le créneau redevient libre.
- [ ] (S) **Liste du jour** imprime ou ouvre le PDF, une page par médecin.
- [ ] (S et M) Les deux ouvrent le même rendez-vous ; M le déplace, puis S essaie de l'annuler : message « vient
      d'être modifié sur un autre poste ».

## 5. Salle d'attente (S puis M)

- [ ] (S) Le patient arrive : sélectionnez son rendez-vous, puis **Patient arrivé**. Sa carte apparaît dans la salle
      d'attente.
- [ ] (S) **Patient sans rendez-vous…** pour le deuxième patient d'essai, avec le même médecin.
- [ ] (M) Les deux patients apparaissent dans sa file ; le patient au rendez-vous est en premier.
- [ ] (S) **Urgence** sur le patient sans rendez-vous : il passe en tête, chez M aussi.
- [ ] (M) **Patient suivant** : (S) voit le bandeau « … appelé par Dr … ».
- [ ] (M) Le dossier s'ouvre ; enregistrez une consultation d'essai. Le patient sort de la salle et son rendez-vous
      passe à « Terminé » chez S.
- [ ] (S) **Parti sans consultation** sur l'autre patient : il quitte la salle.
- [ ] (S) **Fin de journée…** affiche le bilan (vus, partis, absents).
- [ ] Débranchez le câble réseau du poste M 10 secondes puis rebranchez : GestiMed reprend sans redémarrer
      (au pire un message d'erreur, puis l'actualisation repart).

## 6. Messagerie (S et M)

- [ ] (S) **Tout le cabinet** : « Essai de messagerie ». (M) le message arrive avec un son et un chiffre sur
      **Messagerie** dans les 3 secondes.
- [ ] (M) Réponse privée à la secrétaire ; (S) la reçoit, le chiffre disparaît à la lecture.
- [ ] (S) **Citer un patient…** joint le patient d'essai au message.
- [ ] Le point vert indique qui est connecté ; il s'éteint dans la minute qui suit la fermeture de GestiMed sur M.
- [ ] Un message contenant `<b>gras</b>` s'affiche tel quel, sans mise en gras.

## 7. Secret médical (S)

- [ ] Avec le compte `secretaire`, aucun écran ne montre de consultation, d'ordonnance ni de diagnostic.
- [ ] Avec le compte `admin`, les statistiques ne montrent aucun nom de patient (ni à l'écran, ni dans le PDF, ni
      dans le CSV).

## 8. Fin de la recette

- [ ] (admin) Désactivez `essai.sec`.
- [ ] Supprimez les rendez-vous et patients d'essai si vous en avez créé hors de `sql/donnees_test.sql`.
- [ ] Planifiez la sauvegarde quotidienne (`docs/installation-reseau.md`, section 5) et vérifiez qu'elle s'exécute
      le lendemain.
- [ ] Imprimez `docs/notice-secretaire.md` pour la secrétaire.

Résultat : ☐ accepté ☐ accepté avec réserves (lesquelles : …) ☐ refusé

Date : …………   Signatures : ……………………
