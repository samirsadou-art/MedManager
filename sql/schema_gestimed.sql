-- =====================================================================
-- GestiMed : schéma de la base RECONSTITUÉ à partir des requêtes du code.
-- Le script d'origine n'était pas dans l'archive : comparez avec votre
-- base « medicale » avant de l'utiliser autrement que pour les tests.
--
-- Utilisé par le test d'intégration MySqlIntegrationTest, uniquement sur
-- une base dont le nom se termine par « _test » (les tables y sont recréées).
--
-- IMPORTANT : ENGINE=InnoDB. Avec MyISAM, MySQL ignore les transactions :
-- le ROLLBACK de DossierService n'annulerait rien.
-- =====================================================================

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

CREATE TABLE patient (
  idPatient            INT AUTO_INCREMENT PRIMARY KEY,
  nomPatient           VARCHAR(60)  NOT NULL,
  prenomPatient        VARCHAR(60)  NOT NULL,
  date_Nais_Patient    DATE,
  sexePatient          TINYINT(1),
  num_sec_soc_Patient  VARCHAR(30),
  paysPatient          VARCHAR(40),
  wilayaPatient        VARCHAR(40),
  villePatient         VARCHAR(40),
  telPatient           VARCHAR(20),
  mailPatient          VARCHAR(80),
  localitePatient      VARCHAR(60),
  civilePatient        VARCHAR(20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE medecin (
  idMedcin      INT AUTO_INCREMENT PRIMARY KEY,
  ordreMedecin  VARCHAR(30),
  nomMedecin    VARCHAR(60),
  prenomMedcin  VARCHAR(60),
  titre         VARCHAR(20),
  specialite    VARCHAR(80),
  adresse       VARCHAR(150),
  telephone1    VARCHAR(20),
  telephone2    VARCHAR(20),
  email         VARCHAR(80),
  nomCabinet    VARCHAR(80)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE utilisateur (
  idUtilisateur INT AUTO_INCREMENT PRIMARY KEY,
  nom           VARCHAR(60),
  prenom        VARCHAR(60),
  qualite       VARCHAR(60)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE symptomes (
  idsymptome  INT AUTO_INCREMENT PRIMARY KEY,
  designation VARCHAR(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE pathologies (
  idpathologie INT AUTO_INCREMENT PRIMARY KEY,
  designation  VARCHAR(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE medicaments (
  idMedicament  INT AUTO_INCREMENT PRIMARY KEY,
  nomMedicament VARCHAR(200) NOT NULL,
  forme         VARCHAR(50),
  type          CHAR(1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE categanalyse (
  idcateganalyse INT AUTO_INCREMENT PRIMARY KEY,
  categorie      VARCHAR(80) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE analyse (
  idanalyse   INT AUTO_INCREMENT PRIMARY KEY,
  designation VARCHAR(150) NOT NULL,
  idcategorie INT,
  FOREIGN KEY (idcategorie) REFERENCES categanalyse(idcateganalyse)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE consultation (
  idConsultation    INT AUTO_INCREMENT PRIMARY KEY,
  dateConsultation  DATE NOT NULL,
  heureConsultation TIME NOT NULL,
  idMedecin         INT NOT NULL,
  idPatient         INT NOT NULL,
  temperature       DOUBLE,
  pulsation         INT,
  pression          VARCHAR(10),
  respiration       INT,
  poids             DOUBLE,
  FOREIGN KEY (idMedecin) REFERENCES medecin(idMedcin),
  FOREIGN KEY (idPatient) REFERENCES patient(idPatient)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE consultsymptome (
  idconsultation INT NOT NULL,
  idsymptome     INT NOT NULL,
  PRIMARY KEY (idconsultation, idsymptome),
  FOREIGN KEY (idconsultation) REFERENCES consultation(idConsultation),
  FOREIGN KEY (idsymptome) REFERENCES symptomes(idsymptome)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE consultpathologie (
  idconsultation INT NOT NULL,
  idpathologie   INT NOT NULL,
  PRIMARY KEY (idconsultation, idpathologie),
  FOREIGN KEY (idconsultation) REFERENCES consultation(idConsultation),
  FOREIGN KEY (idpathologie) REFERENCES pathologies(idpathologie)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE prescrire (
  idMed     INT NOT NULL,
  idConsul  INT NOT NULL,
  unite     INT,
  posologie INT,
  duree     INT,
  mode      INT,
  PRIMARY KEY (idMed, idConsul),
  FOREIGN KEY (idMed) REFERENCES medicaments(idMedicament),
  FOREIGN KEY (idConsul) REFERENCES consultation(idConsultation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE demandanalyse (
  idconsultation INT NOT NULL,
  idAnalyse      INT NOT NULL,
  observation    VARCHAR(255),
  resultat       TEXT,
  note           INT DEFAULT 0,
  PRIMARY KEY (idconsultation, idAnalyse),
  FOREIGN KEY (idconsultation) REFERENCES consultation(idConsultation),
  FOREIGN KEY (idAnalyse) REFERENCES analyse(idanalyse)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE rondezvous (
  idUser    INT NOT NULL,
  idPatient INT NOT NULL,
  date      DATE NOT NULL,
  heure     TIME NOT NULL,
  dateFix   DATE,
  heureFix  TIME,
  etat      TINYINT DEFAULT 0,
  PRIMARY KEY (date, heure),
  FOREIGN KEY (idPatient) REFERENCES patient(idPatient)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
