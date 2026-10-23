# GestiMed : notice de la secrétaire

Cette notice suit une journée au cabinet. Elle tient sur deux pages : imprimez-la et gardez-la près du poste.

## Se connecter

- Ouvrez GestiMed et saisissez votre identifiant (par exemple `secretaire`) et votre mot de passe.
- **Premier jour, ou après un oubli :** l'administrateur vous donne un mot de passe *provisoire*. Juste après la
  connexion, GestiMed vous demande d'en choisir un nouveau : au moins 8 caractères, dont une lettre et un chiffre.
  Si vous annulez, vous n'êtes pas connectée.
- **Changer votre mot de passe :** menu **Fichier > Changer mon mot de passe…**
- **Après 5 erreurs de mot de passe**, attendez 30 secondes avant de réessayer.
- **Quand vous vous absentez**, verrouillez la session Windows (touches **Windows + L**). Ce PC est aussi le serveur
  du cabinet : **ne l'éteignez pas** pendant les heures d'ouverture.

## L'écran d'accueil

Le menu de gauche donne accès à **Accueil**, **Patients**, **Rendez-vous** et **Messagerie**.

L'accueil montre :
- en haut, les **rendez-vous du jour** de tous les médecins, avec leur statut ;
- en bas, la **salle d'attente**, une carte par patient dans l'ordre d'appel, avec le nombre de minutes d'attente
  (en orange au-delà de 30 minutes).

L'accueil se met à jour tout seul toutes les 3 secondes, avec ce que font les médecins sur leur poste. Vous ne
voyez ni les consultations ni les dossiers médicaux.

Un **double-clic** sur un patient de la salle d'attente ou des rendez-vous du jour ouvre ses **détails**
(état civil et coordonnées), avec **Modifier la fiche**.

## Inscrire un patient

1. Ouvrez **Patients**, puis **Nouveau patient**.
2. Remplissez la fiche. Le **téléphone est obligatoire** ; vous pouvez le saisir avec `+213`, des espaces ou des
   points, GestiMed le remet en forme.
3. Si un patient a le même nom et la même date de naissance, ou le même téléphone, GestiMed vous le montre :
   choisissez la fiche existante plutôt que de créer un doublon.
4. Après l'enregistrement, choisissez la suite : **Prendre un rendez-vous**, **Mettre en salle d'attente** ou
   **Terminer**.

**Retrouver un patient :** dans **Patients**, tapez le début du nom ou du numéro de téléphone. Un double-clic ouvre
sa fiche, et **Modifier la fiche** permet de corriger ses coordonnées.

## Rendez-vous

**Ouvrir l'agenda :** depuis le menu **Rendez-vous**, ou depuis **Patients** avec le bouton
**Prendre un rendez-vous** pour le patient sélectionné.

**Prendre un rendez-vous :**
1. Choisissez le **médecin** dans la liste en haut de l'agenda. C'est obligatoire si le cabinet a plusieurs
   médecins.
2. Cliquez sur une heure libre : GestiMed propose le premier créneau libre de cette heure.
3. Indiquez le motif si besoin, puis confirmez.

**Modifier un rendez-vous :** cliquez sur sa carte dans l'agenda.
- **Déplacer**, puis cliquez sur la nouvelle heure. **Abandonner le déplacement** remet le rendez-vous à sa place.
- **Annuler le rendez-vous** quand le patient prévient. Le créneau redevient libre.
- **Absent** quand le patient n'est pas venu.

Les rendez-vous ne sont jamais effacés : l'historique du patient les garde avec leur statut.

**Liste du jour** (en haut de l'agenda) imprime la liste des rendez-vous, une page par médecin : pratique pour
appeler les patients la veille.

Le champ de recherche de l'agenda retrouve les rendez-vous d'un patient. Appuyez sur Entrée pour chercher dans tout
l'agenda.

## Salle d'attente

- **Patient arrivé :** sélectionnez son rendez-vous dans la liste du jour, puis **Patient arrivé**. Il entre dans la
  file de son médecin.
- **Patient sans rendez-vous :** cliquez sur **Patient sans rendez-vous…**, choisissez ou inscrivez le patient, puis
  le médecin.
- **Urgence :** sélectionnez la carte du patient, puis **Urgence**. Il passe en tête de file.
- **Parti sans consultation :** sélectionnez la carte du patient, puis **Parti sans consultation**. Son rendez-vous
  est marqué absent.
- **Appel du médecin :** quand un médecin appelle un patient depuis son poste, un bandeau vous l'annonce. Cliquez
  sur le bandeau pour le masquer.
- **Consultation terminée, paiement :** quand le médecin termine la consultation, il indique les frais. Un bandeau
  vous l'annonce et la carte du patient passe en tête, en vert, avec « À régler : 2 000 DA ». Quand le patient
  passe à l'accueil, sélectionnez sa carte puis **Encaisser et libérer…** :
  le montant demandé par le médecin est déjà saisi, corrigez-le si le patient paie autre chose, laissez-le vide
  s'il ne paie rien, puis **Libérer le patient**. Il quitte la salle d'attente. Tant qu'il n'est pas libéré, il
  ne peut pas être pointé une seconde fois.
- **Fin de journée…** marque absents les rendez-vous du jour encore prévus dont l'heure est passée, puis affiche le
  bilan de la journée, avec le total encaissé et les patients consultés pas encore libérés.

## Messagerie

- Le menu **Messagerie** ouvre les discussions. Un chiffre sur l'entrée du menu indique les messages non lus, et un
  son signale chaque nouveau message.
- **Tout le cabinet** écrit à tout le monde ; choisissez une personne pour lui écrire en privé. Un point vert
  indique les personnes connectées.
- **Citer un patient…** joint un patient au message (par exemple « ses analyses sont arrivées »).
- Les messages de plus de 12 mois sont effacés automatiquement.

## En cas de problème

| Message | Que faire |
|---|---|
| « Erreur d'accès à la base de données » | Vérifiez que MariaDB tourne sur ce PC (panneau XAMPP : MySQL en vert), puis relancez GestiMed. |
| « … vient d'être modifié sur un autre poste » | Quelqu'un a changé ce rendez-vous ou la salle d'attente en même temps que vous. Cliquez sur **Actualiser** et recommencez. |
| GestiMed refuse de démarrer après une mise à jour | Le jar de ce poste n'a pas la même version que la base : prévenez l'administrateur. |

Le détail technique des erreurs est écrit dans `C:\Users\<votre nom>\GestiMed\gestimed0.log` :
l'administrateur peut le consulter.
