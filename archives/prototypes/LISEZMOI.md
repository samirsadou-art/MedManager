# Prototypes archivés

Ces classes ne sont appelées nulle part dans l'application. Elles ont été
retirées de `src/main/java` pour ne plus être compilées ni livrées dans le jar
(environ 2 000 lignes), mais elles restent dans le dépôt pour mémoire.

| Classe | Rôle d'origine |
|---|---|
| `AddPatient`, `AjouterPat`, `AjouterPatFrame` | anciens formulaires d'ajout de patient, remplacés par `Patient/FichePatient` |
| `Password` | ancien écran de mot de passe, remplacé par `ConnexionForm` |
| `ImagerieForms/Imagerie` | maquette de demande d'imagerie (NetBeans AbsoluteLayout) |
| `Chat/ServiceReseau` | première version du serveur de chat (écoutait sur toutes les interfaces) |
| `modelTableau/my_model` | classe vide générée par NetBeans |
| `TableModelPrincipale/SalleAtten*` | salle d'attente de l'accueil, jamais branchée |

Pour reprendre l'une d'elles, remettez-la à sa place sous
`src/main/java/My_application/`. `Imagerie` a alors besoin de la dépendance
`org.netbeans.external:AbsoluteLayout:RELEASE180` dans le `pom.xml`.
