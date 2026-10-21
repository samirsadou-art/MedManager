-- =====================================================================
-- GestiMed : schéma de la base, RECONSTITUÉ à partir des requêtes du code
-- (dao/jdbc/Jdbc*Dao.java, Mappers.java) et des règles des services.
--
-- Ce script SUPPRIME puis recrée les tables. Il est exécuté par le test
-- d'intégration MySqlIntegrationTest, uniquement sur une base dont le nom
-- se termine par « _test ». Pour la base réelle, utiliser sql/medicale.sql
-- (mêmes définitions, sans DROP).
--
-- Contraintes de format imposées par MySqlIntegrationTest, qui découpe ce
-- fichier lui-même :
--   les commentaires tiennent sur des lignes entières commençant par deux tirets,
--   aucun point-virgule dans un commentaire ni dans une chaîne,
--   un point-virgule termine chaque ordre SQL.
--
-- IMPORTANT : ENGINE=InnoDB. Avec MyISAM, MySQL ignore les transactions et
-- le ROLLBACK de DossierService n'annulerait rien.
-- Compatible MySQL 8 et MariaDB 10.4+ (les CHECK sont ignorés avant 10.2).
-- Noms de tables et colonnes imposés par le code : ne pas les renommer
-- (rondezvous, demandanalyse, idMedcin, prenomMedcin, ...).
-- =====================================================================

DROP TABLE IF EXISTS journal_acces;
DROP TABLE IF EXISTS demandanalyse;
DROP TABLE IF EXISTS prescrire;
DROP TABLE IF EXISTS consultpathologie;
DROP TABLE IF EXISTS consultsymptome;
DROP TABLE IF EXISTS rondezvous;
DROP TABLE IF EXISTS consultation;
DROP TABLE IF EXISTS analyse;
DROP TABLE IF EXISTS categanalyse;
DROP TABLE IF EXISTS medicaments;
DROP TABLE IF EXISTS pathologies;
DROP TABLE IF EXISTS symptomes;
DROP TABLE IF EXISTS utilisateur;
DROP TABLE IF EXISTS medecin;
DROP TABLE IF EXISTS patient;

-- ---------------------------------------------------------------------
-- Patients. idPatient = numéro de dossier : auto-incrémenté, ou saisi
-- par l'utilisateur (JdbcPatientDao.insert avec idPatient explicite).
-- Nom, prénom, date de naissance et sexe sont obligatoires (PatientService).
-- sexePatient : 1 = masculin, 0 = féminin (patient.isMasculin).
-- ---------------------------------------------------------------------
CREATE TABLE patient (
  idPatient            INT          NOT NULL AUTO_INCREMENT,
  nomPatient           VARCHAR(60)  NOT NULL,
  prenomPatient        VARCHAR(60)  NOT NULL,
  date_Nais_Patient    DATE         NOT NULL,
  sexePatient          TINYINT(1)   NOT NULL,
  num_sec_soc_Patient  VARCHAR(30)  NULL,
  paysPatient          VARCHAR(50)  NULL,
  wilayaPatient        VARCHAR(50)  NULL,
  villePatient         VARCHAR(60)  NULL,
  telPatient           VARCHAR(30)  NULL,
  mailPatient          VARCHAR(100) NULL,
  localitePatient      VARCHAR(80)  NULL,
  civilePatient        VARCHAR(30)  NULL,
  PRIMARY KEY (idPatient),
  KEY idx_patient_nom (nomPatient, prenomPatient),
  CONSTRAINT ck_patient_sexe CHECK (sexePatient IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Médecins. Le médecin n° 1 est chargé au démarrage (Session) : il doit
-- exister. Identifiant mal orthographié (idMedcin, prenomMedcin) imposé
-- par Mappers.MEDECIN.
-- ---------------------------------------------------------------------
CREATE TABLE medecin (
  idMedcin      INT          NOT NULL AUTO_INCREMENT,
  ordreMedecin  VARCHAR(30)  NULL,
  nomMedecin    VARCHAR(60)  NOT NULL,
  prenomMedcin  VARCHAR(60)  NOT NULL,
  titre         VARCHAR(20)  NULL,
  specialite    VARCHAR(80)  NULL,
  adresse       VARCHAR(200) NULL,
  telephone1    VARCHAR(30)  NULL,
  telephone2    VARCHAR(30)  NULL,
  email         VARCHAR(100) NULL,
  nomCabinet    VARCHAR(100) NULL,
  PRIMARY KEY (idMedcin),
  UNIQUE KEY uk_medecin_ordre (ordreMedecin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Utilisateurs (personnel qui prend les rendez-vous).
-- login / motDePasse : identifiants de l'écran de connexion. motDePasse
-- contient une empreinte PBKDF2 (service.MotDePasse), jamais le mot de
-- passe en clair. NULL tant que le compte n'a pas été activé.
-- idMedecin : médecin traitant de la session quand cet utilisateur se connecte.
-- ---------------------------------------------------------------------
CREATE TABLE utilisateur (
  idUtilisateur INT          NOT NULL AUTO_INCREMENT,
  nom           VARCHAR(60)  NOT NULL,
  prenom        VARCHAR(60)  NOT NULL,
  qualite       VARCHAR(60)  NULL,
  login         VARCHAR(40)  NULL,
  motDePasse    VARCHAR(200) NULL,
  actif         TINYINT(1)   NOT NULL DEFAULT 1,
  idMedecin     INT          NULL,
  PRIMARY KEY (idUtilisateur),
  UNIQUE KEY uk_utilisateur_login (login),
  KEY idx_utilisateur_medecin (idMedecin),
  CONSTRAINT fk_utilisateur_medecin FOREIGN KEY (idMedecin)
    REFERENCES medecin (idMedcin) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Référentiels. La désignation est unique : CatalogueService cherche
-- d'abord une entrée existante (findByDesignation, findByNom) avant d'en
-- créer une. L'index unique sert aussi les recherches LIKE 'préfixe%'
-- et les ORDER BY designation des DAO.
-- ---------------------------------------------------------------------
CREATE TABLE symptomes (
  idsymptome  INT          NOT NULL AUTO_INCREMENT,
  designation VARCHAR(150) NOT NULL,
  PRIMARY KEY (idsymptome),
  UNIQUE KEY uk_symptomes_designation (designation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pathologies (
  idpathologie INT          NOT NULL AUTO_INCREMENT,
  designation  VARCHAR(150) NOT NULL,
  PRIMARY KEY (idpathologie),
  UNIQUE KEY uk_pathologies_designation (designation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- nomMedicament = désignation complète « NOM FORME DOSAGE ».
-- type : 'P' = ajouté par le praticien (JdbcMedicamentDao.insert),
--        'R' = référentiel livré avec l'application (donnees_initiales.sql).
CREATE TABLE medicaments (
  idMedicament  INT          NOT NULL AUTO_INCREMENT,
  nomMedicament VARCHAR(200) NOT NULL,
  forme         VARCHAR(60)  NULL,
  type          CHAR(1)      NOT NULL DEFAULT 'P',
  PRIMARY KEY (idMedicament),
  UNIQUE KEY uk_medicaments_nom (nomMedicament),
  KEY idx_medicaments_forme (forme)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categanalyse (
  idcateganalyse INT          NOT NULL AUTO_INCREMENT,
  categorie      VARCHAR(80)  NOT NULL,
  PRIMARY KEY (idcateganalyse),
  UNIQUE KEY uk_categanalyse_categorie (categorie)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Une analyse appartient obligatoirement à une catégorie (CatalogueService.creerAnalyse).
CREATE TABLE analyse (
  idanalyse   INT          NOT NULL AUTO_INCREMENT,
  designation VARCHAR(150) NOT NULL,
  idcategorie INT          NOT NULL,
  PRIMARY KEY (idanalyse),
  UNIQUE KEY uk_analyse_designation (designation),
  KEY idx_analyse_categorie (idcategorie, designation),
  CONSTRAINT fk_analyse_categanalyse FOREIGN KEY (idcategorie)
    REFERENCES categanalyse (idcateganalyse) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Consultations. Les constantes vitales valent -1 quand elles ne sont pas
-- renseignées (consultation.NON_RENSEIGNE) et la pression vaut une chaîne
-- vide. Bornes reprises de ConstantesVitales.
-- RESTRICT : on ne supprime ni un patient ni un médecin qui a un historique.
-- ---------------------------------------------------------------------
CREATE TABLE consultation (
  idConsultation    INT          NOT NULL AUTO_INCREMENT,
  dateConsultation  DATE         NOT NULL,
  heureConsultation TIME         NOT NULL,
  idMedecin         INT          NOT NULL,
  idPatient         INT          NOT NULL,
  temperature       DECIMAL(4,2) NOT NULL DEFAULT -1,
  pulsation         SMALLINT     NOT NULL DEFAULT -1,
  pression          VARCHAR(10)  NOT NULL DEFAULT '',
  respiration       SMALLINT     NOT NULL DEFAULT -1,
  poids             DECIMAL(5,2) NOT NULL DEFAULT -1,
  PRIMARY KEY (idConsultation),
  KEY idx_consultation_patient (idPatient, dateConsultation, heureConsultation),
  KEY idx_consultation_date (dateConsultation, heureConsultation),
  KEY idx_consultation_medecin (idMedecin),
  CONSTRAINT fk_consultation_medecin FOREIGN KEY (idMedecin)
    REFERENCES medecin (idMedcin) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_consultation_patient FOREIGN KEY (idPatient)
    REFERENCES patient (idPatient) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT ck_consultation_temperature CHECK (temperature = -1 OR temperature BETWEEN 25 AND 45),
  CONSTRAINT ck_consultation_pulsation CHECK (pulsation = -1 OR pulsation BETWEEN 1 AND 300),
  CONSTRAINT ck_consultation_respiration CHECK (respiration = -1 OR respiration BETWEEN 1 AND 100),
  CONSTRAINT ck_consultation_poids CHECK (poids = -1 OR (poids > 0 AND poids < 1000)),
  CONSTRAINT ck_consultation_pression CHECK (pression = '' OR pression REGEXP '^[0-9]{1,3}/[0-9]{1,3}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Tables d'association d'une consultation : CASCADE vers la consultation,
-- RESTRICT vers le référentiel (on ne supprime pas un symptôme utilisé).
-- Une même entrée ne figure qu'une fois par consultation (addSymptome,
-- addPathologie, ordonnance.ajouter, my_modelAnalyse refusent les doublons).
-- ---------------------------------------------------------------------
CREATE TABLE consultsymptome (
  idconsultation INT NOT NULL,
  idsymptome     INT NOT NULL,
  PRIMARY KEY (idconsultation, idsymptome),
  KEY idx_consultsymptome_symptome (idsymptome),
  CONSTRAINT fk_consultsymptome_consultation FOREIGN KEY (idconsultation)
    REFERENCES consultation (idConsultation) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_consultsymptome_symptome FOREIGN KEY (idsymptome)
    REFERENCES symptomes (idsymptome) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE consultpathologie (
  idconsultation INT NOT NULL,
  idpathologie   INT NOT NULL,
  PRIMARY KEY (idconsultation, idpathologie),
  KEY idx_consultpathologie_pathologie (idpathologie),
  CONSTRAINT fk_consultpathologie_consultation FOREIGN KEY (idconsultation)
    REFERENCES consultation (idConsultation) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_consultpathologie_pathologie FOREIGN KEY (idpathologie)
    REFERENCES pathologies (idpathologie) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ordonnance : une ligne par médicament prescrit.
-- mode : 0 = soir, 1 = matin, 2 = indifférent (medicament.MODE_*).
CREATE TABLE prescrire (
  idMed     INT      NOT NULL,
  idConsul  INT      NOT NULL,
  unite     SMALLINT NOT NULL DEFAULT 0,
  posologie SMALLINT NOT NULL DEFAULT 0,
  duree     SMALLINT NOT NULL DEFAULT 0,
  mode      TINYINT  NOT NULL DEFAULT 2,
  PRIMARY KEY (idConsul, idMed),
  KEY idx_prescrire_medicament (idMed),
  CONSTRAINT fk_prescrire_consultation FOREIGN KEY (idConsul)
    REFERENCES consultation (idConsultation) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_prescrire_medicament FOREIGN KEY (idMed)
    REFERENCES medicaments (idMedicament) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT ck_prescrire_quantites CHECK (unite >= 0 AND posologie >= 0 AND duree >= 0),
  CONSTRAINT ck_prescrire_mode CHECK (mode IN (0, 1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Analyses demandées. resultat et note sont saisis plus tard
-- (DemandeAnalyseDao.updateResultats). note : interprétation de 0 à 4.
CREATE TABLE demandanalyse (
  idconsultation INT          NOT NULL,
  idAnalyse      INT          NOT NULL,
  observation    VARCHAR(500) NULL,
  resultat       TEXT         NULL,
  note           TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (idconsultation, idAnalyse),
  KEY idx_demandanalyse_analyse (idAnalyse),
  CONSTRAINT fk_demandanalyse_consultation FOREIGN KEY (idconsultation)
    REFERENCES consultation (idConsultation) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_demandanalyse_analyse FOREIGN KEY (idAnalyse)
    REFERENCES analyse (idanalyse) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT ck_demandanalyse_note CHECK (note BETWEEN 0 AND 4)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Rendez-vous (nom de table imposé par le code). Un seul rendez-vous par
-- créneau (date, heure) : RendezVousService.confirmer le vérifie, la clé
-- primaire le garantit même si deux postes réservent en même temps.
-- etat n'est pas écrit par le code : 0 par défaut.
-- dateFix / heureFix : date et heure de la prise du rendez-vous.
-- ---------------------------------------------------------------------
CREATE TABLE rondezvous (
  idUser    INT     NOT NULL,
  idPatient INT     NOT NULL,
  `date`    DATE    NOT NULL,
  heure     TIME    NOT NULL,
  dateFix   DATE    NULL,
  heureFix  TIME    NULL,
  etat      TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`date`, heure),
  KEY idx_rondezvous_patient (idPatient, `date`),
  KEY idx_rondezvous_utilisateur (idUser),
  CONSTRAINT fk_rondezvous_patient FOREIGN KEY (idPatient)
    REFERENCES patient (idPatient) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_rondezvous_utilisateur FOREIGN KEY (idUser)
    REFERENCES utilisateur (idUtilisateur) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Journal des accès (traçabilité du dossier médical) : qui a ouvert ou
-- modifié quel dossier, et quand (service.JournalAcces). L'application
-- n'y fait que des INSERT et des SELECT.
-- idUtilisateur est NULL pour une tentative de connexion échouée.
-- Pas de clé étrangère vers patient : la trace doit survivre au dossier.
-- ---------------------------------------------------------------------
CREATE TABLE journal_acces (
  idJournal     BIGINT       NOT NULL AUTO_INCREMENT,
  dateHeure     DATETIME     NOT NULL,
  idUtilisateur INT          NULL,
  action        VARCHAR(40)  NOT NULL,
  idPatient     INT          NULL,
  detail        VARCHAR(255) NULL,
  poste         VARCHAR(100) NULL,
  PRIMARY KEY (idJournal),
  KEY idx_journal_patient (idPatient, dateHeure),
  KEY idx_journal_utilisateur (idUtilisateur, dateHeure),
  CONSTRAINT fk_journal_utilisateur FOREIGN KEY (idUtilisateur)
    REFERENCES utilisateur (idUtilisateur) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
