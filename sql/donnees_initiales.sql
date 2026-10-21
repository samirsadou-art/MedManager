-- =====================================================================
-- GestiMed : données de départ de la base « medicale ».
--
-- À exécuter après la création du schéma (outil MigrationBase migrer, qui
-- applique src/main/resources/db/migration) :
--   D:/xampp/mysql/bin/mysql.exe -u root --default-character-set=utf8mb4 < sql/donnees_initiales.sql
--
-- Relançable sans risque : INSERT IGNORE uniquement. Une ligne déjà
-- présente (même identifiant ou même désignation, clés uniques) est
-- laissée telle quelle, jamais écrasée.
--
-- Les personnes (médecin n° 1, utilisateur n° 1) sont FICTIVES : remplacez
-- leurs coordonnées par celles du cabinet (elles figurent sur les
-- ordonnances et les demandes d'analyses imprimées).
-- =====================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

USE medicale;

-- ---------------------------------------------------------------------
-- Médecin n° 1 et utilisateur n° 1 : chargés au démarrage par Session
-- (AppContext.ID_MEDECIN_DEFAUT / ID_UTILISATEUR_DEFAUT). Sans le médecin
-- n° 1, l'application lève « Le médecin n° 1 est introuvable dans la base ».
-- ---------------------------------------------------------------------
INSERT IGNORE INTO medecin
  (idMedcin, ordreMedecin, nomMedecin, prenomMedcin, titre, specialite, adresse,
   telephone1, telephone2, email, nomCabinet)
VALUES
  (1, NULL, 'BENALI', 'Karim', 'Dr', 'Médecine générale',
   'Adresse du cabinet à renseigner', NULL, NULL, NULL, 'Cabinet médical');

-- Sans identifiant ni mot de passe : GestiMed les fait choisir au premier lancement.
INSERT IGNORE INTO utilisateur (idUtilisateur, nom, prenom, qualite, idMedecin)
VALUES (1, 'BENALI', 'Karim', 'Médecin', 1);

-- ---------------------------------------------------------------------
-- Catégories d'analyses
-- ---------------------------------------------------------------------
INSERT IGNORE INTO categanalyse (categorie) VALUES
  ('Hématologie'),
  ('Hémostase'),
  ('Biochimie'),
  ('Hormonologie'),
  ('Sérologie - Immunologie'),
  ('Urines'),
  ('Microbiologie - Parasitologie');

-- ---------------------------------------------------------------------
-- Analyses courantes (désignations en majuscules, comme
-- CatalogueService.creerAnalyse). La catégorie est retrouvée par son nom.
-- ---------------------------------------------------------------------
INSERT IGNORE INTO analyse (designation, idcategorie)
SELECT a.designation, c.idcateganalyse
FROM (
  SELECT 'Hématologie' AS categorie, 'FNS (HEMOGRAMME)' AS designation
  UNION ALL SELECT 'Hématologie', 'VITESSE DE SEDIMENTATION (VS)'
  UNION ALL SELECT 'Hématologie', 'GROUPAGE SANGUIN ABO RHESUS'
  UNION ALL SELECT 'Hématologie', 'RETICULOCYTES'
  UNION ALL SELECT 'Hématologie', 'FROTTIS SANGUIN'
  UNION ALL SELECT 'Hémostase', 'TAUX DE PROTHROMBINE (TP)'
  UNION ALL SELECT 'Hémostase', 'INR'
  UNION ALL SELECT 'Hémostase', 'TCA'
  UNION ALL SELECT 'Hémostase', 'FIBRINOGENE'
  UNION ALL SELECT 'Hémostase', 'D-DIMERES'
  UNION ALL SELECT 'Biochimie', 'GLYCEMIE A JEUN'
  UNION ALL SELECT 'Biochimie', 'HEMOGLOBINE GLYQUEE (HBA1C)'
  UNION ALL SELECT 'Biochimie', 'UREE SANGUINE'
  UNION ALL SELECT 'Biochimie', 'CREATININEMIE'
  UNION ALL SELECT 'Biochimie', 'ACIDE URIQUE'
  UNION ALL SELECT 'Biochimie', 'CHOLESTEROL TOTAL'
  UNION ALL SELECT 'Biochimie', 'CHOLESTEROL HDL'
  UNION ALL SELECT 'Biochimie', 'CHOLESTEROL LDL'
  UNION ALL SELECT 'Biochimie', 'TRIGLYCERIDES'
  UNION ALL SELECT 'Biochimie', 'TRANSAMINASES (ASAT - ALAT)'
  UNION ALL SELECT 'Biochimie', 'GAMMA GT'
  UNION ALL SELECT 'Biochimie', 'PHOSPHATASES ALCALINES'
  UNION ALL SELECT 'Biochimie', 'BILIRUBINE TOTALE ET CONJUGUEE'
  UNION ALL SELECT 'Biochimie', 'IONOGRAMME SANGUIN'
  UNION ALL SELECT 'Biochimie', 'CALCEMIE'
  UNION ALL SELECT 'Biochimie', 'FER SERIQUE'
  UNION ALL SELECT 'Biochimie', 'FERRITINE'
  UNION ALL SELECT 'Biochimie', 'CRP'
  UNION ALL SELECT 'Biochimie', 'PROTIDEMIE'
  UNION ALL SELECT 'Biochimie', 'ALBUMINEMIE'
  UNION ALL SELECT 'Hormonologie', 'TSH'
  UNION ALL SELECT 'Hormonologie', 'T4 LIBRE'
  UNION ALL SELECT 'Hormonologie', 'T3 LIBRE'
  UNION ALL SELECT 'Hormonologie', 'BETA HCG'
  UNION ALL SELECT 'Hormonologie', 'PROLACTINE'
  UNION ALL SELECT 'Hormonologie', 'VITAMINE D (25-OH)'
  UNION ALL SELECT 'Sérologie - Immunologie', 'SEROLOGIE HEPATITE B (AG HBS)'
  UNION ALL SELECT 'Sérologie - Immunologie', 'SEROLOGIE HEPATITE C'
  UNION ALL SELECT 'Sérologie - Immunologie', 'SEROLOGIE VIH'
  UNION ALL SELECT 'Sérologie - Immunologie', 'SEROLOGIE TOXOPLASMOSE'
  UNION ALL SELECT 'Sérologie - Immunologie', 'SEROLOGIE RUBEOLE'
  UNION ALL SELECT 'Sérologie - Immunologie', 'FACTEUR RHUMATOIDE'
  UNION ALL SELECT 'Sérologie - Immunologie', 'ASLO'
  UNION ALL SELECT 'Sérologie - Immunologie', 'PSA TOTAL'
  UNION ALL SELECT 'Urines', 'ECBU'
  UNION ALL SELECT 'Urines', 'CHIMIE DES URINES (BANDELETTE)'
  UNION ALL SELECT 'Urines', 'PROTEINURIE DES 24 H'
  UNION ALL SELECT 'Urines', 'MICROALBUMINURIE'
  UNION ALL SELECT 'Microbiologie - Parasitologie', 'COPROCULTURE'
  UNION ALL SELECT 'Microbiologie - Parasitologie', 'EXAMEN PARASITOLOGIQUE DES SELLES'
  UNION ALL SELECT 'Microbiologie - Parasitologie', 'PRELEVEMENT DE GORGE'
  UNION ALL SELECT 'Microbiologie - Parasitologie', 'HEMOCULTURE'
) AS a
JOIN categanalyse c ON c.categorie = a.categorie;

-- ---------------------------------------------------------------------
-- Symptômes courants
-- ---------------------------------------------------------------------
INSERT IGNORE INTO symptomes (designation) VALUES
  ('Fièvre'),
  ('Frissons'),
  ('Toux sèche'),
  ('Toux grasse'),
  ('Céphalées'),
  ('Vertiges'),
  ('Nausées'),
  ('Vomissements'),
  ('Diarrhée'),
  ('Constipation'),
  ('Douleur abdominale'),
  ('Maux d''estomac'),
  ('Brûlures d''estomac'),
  ('Douleur thoracique'),
  ('Dyspnée'),
  ('Palpitations'),
  ('Asthénie'),
  ('Anorexie'),
  ('Perte de poids'),
  ('Courbatures'),
  ('Arthralgies'),
  ('Lombalgie'),
  ('Rhinorrhée'),
  ('Obstruction nasale'),
  ('Odynophagie'),
  ('Otalgie'),
  ('Éruption cutanée'),
  ('Prurit'),
  ('Brûlures mictionnelles'),
  ('Pollakiurie'),
  ('Œdème des membres inférieurs'),
  ('Insomnie'),
  ('Anxiété'),
  ('Sueurs nocturnes'),
  ('Polyurie - polydipsie');

-- ---------------------------------------------------------------------
-- Pathologies courantes
-- ---------------------------------------------------------------------
INSERT IGNORE INTO pathologies (designation) VALUES
  ('Hypertension artérielle'),
  ('Diabète de type 1'),
  ('Diabète de type 2'),
  ('Dyslipidémie'),
  ('Obésité'),
  ('Insuffisance cardiaque'),
  ('Cardiopathie ischémique'),
  ('Asthme'),
  ('BPCO'),
  ('Bronchite aiguë'),
  ('Pneumopathie'),
  ('Grippe'),
  ('COVID-19'),
  ('Rhinopharyngite'),
  ('Angine'),
  ('Sinusite'),
  ('Otite moyenne aiguë'),
  ('Conjonctivite'),
  ('Gastro-entérite'),
  ('Reflux gastro-œsophagien'),
  ('Ulcère gastroduodénal'),
  ('Infection urinaire'),
  ('Lithiase rénale'),
  ('Insuffisance rénale chronique'),
  ('Anémie ferriprive'),
  ('Hypothyroïdie'),
  ('Hyperthyroïdie'),
  ('Hépatite virale B'),
  ('Migraine'),
  ('Lombalgie commune'),
  ('Arthrose'),
  ('Polyarthrite rhumatoïde'),
  ('Dermatite atopique'),
  ('Dépression'),
  ('Trouble anxieux');

-- ---------------------------------------------------------------------
-- Médicaments (type 'R' = référentiel livré avec l'application).
-- 1) Les 25 entrées de src/main/resources/My_application/source/Medicament_A.txt
--    (désignations reprises telles quelles, forme développée en clair).
-- ---------------------------------------------------------------------
INSERT IGNORE INTO medicaments (nomMedicament, forme, type) VALUES
  ('A 313 200 000 UI/100 g pom', 'pommade', 'R'),
  ('ABBOTICINE 200 mg/5 ml glé p susp buv', 'granulés pour suspension buvable', 'R'),
  ('ACETATE DE CYPROTERONE ARROW 50 mg cp', 'comprimé', 'R'),
  ('ACETATE DE CYPROTERONE EG 50 mg cp séc', 'comprimé sécable', 'R'),
  ('ACETATE DE CYPROTERONE SANDOZ 50 mg cp séc', 'comprimé sécable', 'R'),
  ('ACETATE DE CYPROTERONE TEVA 50 mg cp', 'comprimé', 'R'),
  ('ACICLOVIR ARROW CONSEIL 5 % crème', 'crème', 'R'),
  ('ACICLOVIR BIOGARAN CONSEIL 5 % crème', 'crème', 'R'),
  ('ACICLOVIR CRISTERS CONSEIL 5 % crème', 'crème', 'R'),
  ('ACICLOVIR EG LABO 5 % crème', 'crème', 'R'),
  ('ACICLOVIR MYLAN PHARMA 5 % crème', 'crème', 'R'),
  ('ACICLOVIR RATIOPHARM CONSEIL 5 % crème', 'crème', 'R'),
  ('ACICLOVIR SANDOZ CONSEIL 5 % crème', 'crème', 'R'),
  ('ACICLOVIR TEVA CONSEIL 5 % crème', 'crème', 'R'),
  ('ACIDE FUSIDIQUE ARROW 2 % crème', 'crème', 'R'),
  ('ACIDE FUSIDIQUE BGR 2 % crème', 'crème', 'R'),
  ('ACIDE FUSIDIQUE BIOGARAN 2 % crème', 'crème', 'R'),
  ('ACIDE FUSIDIQUE EG 2 % crème', 'crème', 'R'),
  ('ACIDE FUSIDIQUE MYLAN 2 % crème', 'crème', 'R'),
  ('ACIDE FUSIDIQUE RATIOPHARM 2 % crème', 'crème', 'R'),
  ('ACIDE FUSIDIQUE SANDOZ 2 % crème', 'crème', 'R'),
  ('ACNETRAIT 5 mg caps molle', 'capsule molle', 'R'),
  ('ACNETRAIT 10 mg caps molle', 'capsule molle', 'R'),
  ('ACNETRAIT 20 mg caps molle', 'capsule molle', 'R'),
  ('ACNETRAIT 40 mg caps molle', 'capsule molle', 'R');

-- 2) Médicaments courants en dénomination commune (DCI), même format.
INSERT IGNORE INTO medicaments (nomMedicament, forme, type) VALUES
  ('PARACETAMOL 500 mg cp', 'comprimé', 'R'),
  ('PARACETAMOL 1 g cp', 'comprimé', 'R'),
  ('PARACETAMOL 2,4 % sol buv', 'solution buvable', 'R'),
  ('IBUPROFENE 400 mg cp', 'comprimé', 'R'),
  ('IBUPROFENE 20 mg/ml susp buv', 'suspension buvable', 'R'),
  ('ACIDE ACETYLSALICYLIQUE 100 mg cp', 'comprimé', 'R'),
  ('DICLOFENAC 50 mg cp', 'comprimé', 'R'),
  ('TRAMADOL 50 mg gél', 'gélule', 'R'),
  ('PHLOROGLUCINOL 80 mg cp', 'comprimé', 'R'),
  ('AMOXICILLINE 500 mg gél', 'gélule', 'R'),
  ('AMOXICILLINE 1 g cp', 'comprimé', 'R'),
  ('AMOXICILLINE 250 mg/5 ml pdre p susp buv', 'poudre pour suspension buvable', 'R'),
  ('AMOXICILLINE ACIDE CLAVULANIQUE 1 g/125 mg cp', 'comprimé', 'R'),
  ('AZITHROMYCINE 250 mg cp', 'comprimé', 'R'),
  ('CIPROFLOXACINE 500 mg cp', 'comprimé', 'R'),
  ('METRONIDAZOLE 500 mg cp', 'comprimé', 'R'),
  ('OMEPRAZOLE 20 mg gél', 'gélule', 'R'),
  ('ESOMEPRAZOLE 40 mg cp', 'comprimé', 'R'),
  ('DOMPERIDONE 10 mg cp', 'comprimé', 'R'),
  ('METOCLOPRAMIDE 10 mg cp', 'comprimé', 'R'),
  ('DIOSMECTITE 3 g pdre p susp buv', 'poudre pour suspension buvable', 'R'),
  ('SOLUTE DE REHYDRATATION ORALE pdre p sol buv', 'poudre pour solution buvable', 'R'),
  ('METFORMINE 850 mg cp', 'comprimé', 'R'),
  ('GLIBENCLAMIDE 5 mg cp', 'comprimé', 'R'),
  ('INSULINE GLARGINE 100 UI/ml sol inj', 'solution injectable', 'R'),
  ('AMLODIPINE 5 mg gél', 'gélule', 'R'),
  ('AMLODIPINE 10 mg gél', 'gélule', 'R'),
  ('BISOPROLOL 5 mg cp', 'comprimé', 'R'),
  ('RAMIPRIL 5 mg gél', 'gélule', 'R'),
  ('LOSARTAN 50 mg cp', 'comprimé', 'R'),
  ('HYDROCHLOROTHIAZIDE 25 mg cp', 'comprimé', 'R'),
  ('FUROSEMIDE 40 mg cp', 'comprimé', 'R'),
  ('ATORVASTATINE 20 mg cp', 'comprimé', 'R'),
  ('LEVOTHYROXINE 100 µg cp', 'comprimé', 'R'),
  ('PREDNISOLONE 20 mg cp', 'comprimé', 'R'),
  ('SALBUTAMOL 100 µg/dose susp p inhal', 'suspension pour inhalation', 'R'),
  ('LORATADINE 10 mg cp', 'comprimé', 'R'),
  ('CETIRIZINE 10 mg cp', 'comprimé', 'R'),
  ('SULFATE FERREUX 80 mg cp', 'comprimé', 'R'),
  ('ACIDE FOLIQUE 5 mg cp', 'comprimé', 'R'),
  ('VITAMINE D3 200 000 UI sol buv', 'solution buvable', 'R');
