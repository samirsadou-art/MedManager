-- =====================================================================
-- GestiMed : DONNÉES DE TEST TEMPORAIRES pour la base « medicale ».
--
-- À exécuter après la création du schéma (MigrationBase migrer) et
-- sql/donnees_initiales.sql :
--   D:/xampp/mysql/bin/mysql.exe -u root --default-character-set=utf8mb4 < sql/donnees_test.sql
--
-- Toutes les personnes sont FICTIVES.
--
-- Les lignes de test sont isolées par leurs identifiants :
--   patients       9001 à 9999
--   consultations  9001 à 9999
-- Le script commence par supprimer ces lignes, puis les recrée : on peut
-- le relancer autant de fois qu'on veut. Les dates sont calculées à partir
-- du jour d'exécution (CURDATE()) : l'écran d'accueil montre donc
-- toujours des rendez-vous et des consultations « du jour ».
--
-- Pour tout retirer : sql/supprimer_donnees_test.sql
-- =====================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

USE medicale;

START TRANSACTION;

-- ---------------------------------------------------------------------
-- Nettoyage des données de test précédentes. Les symptômes, pathologies,
-- prescriptions et demandes d'analyses des consultations partent par
-- ON DELETE CASCADE.
-- ---------------------------------------------------------------------
DELETE FROM rendezvous   WHERE idPatient BETWEEN 9001 AND 9999;
DELETE FROM consultation WHERE idConsultation BETWEEN 9001 AND 9999
                            OR idPatient BETWEEN 9001 AND 9999;
DELETE FROM patient      WHERE idPatient BETWEEN 9001 AND 9999;

-- ---------------------------------------------------------------------
-- Patients (sexePatient : 1 = homme, 0 = femme)
-- ---------------------------------------------------------------------
INSERT INTO patient
  (idPatient, numeroDossier, nomPatient, prenomPatient, date_Nais_Patient, sexePatient, num_sec_soc_Patient,
   paysPatient, wilayaPatient, villePatient, telPatient, mailPatient, localitePatient, civilePatient)
VALUES
  (9001, '9001', 'AMRANI',   'Yacine',  '1978-03-14', 1, '178031400101', 'Algérie', 'Tizi Ouzou', 'Tizi Ouzou',   '0550 11 22 01', 'y.amrani@exemple.dz',   'Nouvelle Ville', 'Marié'),
  (9002, '9002', 'BELKACEM', 'Nadia',   '1985-07-22', 0, '285072200202', 'Algérie', 'Tizi Ouzou', 'Azazga',       '0661 22 33 02', 'n.belkacem@exemple.dz', 'Centre',         'Mariée'),
  (9003, '9003', 'CHERIFI',  'Mohamed', '1952-11-05', 1, '152110500303', 'Algérie', 'Béjaïa',     'Akbou',        '0770 33 44 03', NULL,                    'Guendouza',      'Marié'),
  (9004, '9004', 'DJABRI',   'Lina',    '2019-02-10', 0, NULL,           'Algérie', 'Tizi Ouzou', 'Draâ Ben Khedda','0555 44 55 04', NULL,                  'Cité 500 logts', 'Célibataire'),
  (9005, '9005', 'FERHAT',   'Karima',  '1990-09-30', 0, '290093000505', 'Algérie', 'Alger',      'Bab Ezzouar',  '0662 55 66 05', 'k.ferhat@exemple.dz',   'Cité Smail Yefsah','Célibataire'),
  (9006, '9006', 'HADDAD',   'Rachid',  '1965-01-18', 1, '165011800606', 'Algérie', 'Tizi Ouzou', 'Larbaâ Nath Irathen','0771 66 77 06', NULL,              'Centre',         'Marié'),
  (9007, '9007', 'IDIR',     'Sonia',   '1972-05-03', 0, '272050300707', 'Algérie', 'Boumerdès',  'Bordj Menaïel','0556 77 88 07', 'sonia.idir@exemple.dz', 'Village agricole','Divorcée'),
  (9008, '9008', 'KACI',     'Amine',   '2001-12-25', 1, '201122500808', 'Algérie', 'Tizi Ouzou', 'Tizi Ouzou',   '0663 88 99 08', 'amine.kaci@exemple.dz', 'Hasnaoua',       'Célibataire'),
  (9009, '9009', 'MEZIANI',  'Fatima',  '1948-08-08', 0, '148080800909', 'Algérie', 'Tizi Ouzou', 'Ain El Hammam','0772 99 00 09', NULL,                    'Taourirt',       'Veuve'),
  (9010, '9010', 'OUALI',    'Samir',   '1995-04-16', 1, '195041601010', 'Algérie', 'Bouira',     'Lakhdaria',    '0557 00 11 10', NULL,                    'Centre',         'Célibataire');

-- ---------------------------------------------------------------------
-- Consultations (médecin n° 1). Constantes non mesurées : NULL.
-- ---------------------------------------------------------------------
INSERT INTO consultation
  (idConsultation, dateConsultation, heureConsultation, idMedecin, idPatient,
   temperature, pulsation, pressionSystolique, pressionDiastolique, respiration, poids)
VALUES
  -- Historique (plusieurs visites pour certains patients)
  (9001, DATE_SUB(CURDATE(), INTERVAL 120 DAY), '09:15:00', 1, 9001, 37.00, 78, 150, 90,  16, 92.50),
  (9002, DATE_SUB(CURDATE(), INTERVAL 60 DAY),  '10:00:00', 1, 9001, 36.80, 74, 140, 80,  16, 91.00),
  (9003, DATE_SUB(CURDATE(), INTERVAL 45 DAY),  '11:30:00', 1, 9002, 38.90, 96, 120, 70,  20, 63.00),
  (9004, DATE_SUB(CURDATE(), INTERVAL 90 DAY),  '08:45:00', 1, 9003, 36.60, 70, 160, 90,  18, 78.00),
  (9005, DATE_SUB(CURDATE(), INTERVAL 30 DAY),  '09:30:00', 1, 9003, 36.70, 72, 140, 80,  18, 77.20),
  (9006, DATE_SUB(CURDATE(), INTERVAL 20 DAY),  '14:15:00', 1, 9004, 39.20, 120, NULL, NULL,  28, 18.40),
  (9007, DATE_SUB(CURDATE(), INTERVAL 15 DAY),  '15:00:00', 1, 9005, 37.10, 82, 110, 70,  16, 58.00),
  (9008, DATE_SUB(CURDATE(), INTERVAL 10 DAY),  '10:45:00', 1, 9006, 36.90, 88, 130, 80,  22, 84.00),
  (9009, DATE_SUB(CURDATE(), INTERVAL 7 DAY),   '16:00:00', 1, 9007, 36.50, 76, 120, 80,  16, 70.50),
  (9010, DATE_SUB(CURDATE(), INTERVAL 3 DAY),   '11:00:00', 1, 9009, 37.30, 90, 170, 100, 20, 66.00),
  (9011, DATE_SUB(CURDATE(), INTERVAL 1 DAY),   '09:00:00', 1, 9008, NULL,  NULL, NULL, NULL,  NULL, NULL),
  -- Consultations du jour (écran d'accueil)
  (9012, CURDATE(), '08:30:00', 1, 9002, 37.60, 84, 120, 70,  18, 62.50),
  (9013, CURDATE(), '09:45:00', 1, 9010, 38.40, 92, 120, 80,  18, 74.00),
  (9014, CURDATE(), '11:15:00', 1, 9006, 36.80, 80, 130, 80,  20, 83.50);

-- Symptômes relevés
INSERT INTO consultsymptome (idconsultation, idsymptome) VALUES
  (9001, 5), (9001, 6),                 -- céphalées, vertiges
  (9002, 5),
  (9003, 1), (9003, 2), (9003, 25),     -- fièvre, frissons, odynophagie
  (9004, 35), (9004, 17),               -- polyurie-polydipsie, asthénie
  (9005, 17),
  (9006, 1), (9006, 3), (9006, 23),     -- fièvre, toux sèche, rhinorrhée
  (9007, 29), (9007, 30),               -- brûlures mictionnelles, pollakiurie
  (9008, 15), (9008, 4),                -- dyspnée, toux grasse
  (9009, 13), (9009, 12),               -- brûlures et maux d'estomac
  (9010, 31), (9010, 15), (9010, 17),   -- œdèmes, dyspnée, asthénie
  (9011, 22),                           -- lombalgie
  (9012, 5), (9012, 7),                 -- céphalées, nausées
  (9013, 1), (9013, 20), (9013, 3),     -- fièvre, courbatures, toux sèche
  (9014, 15);

-- Diagnostics
INSERT INTO consultpathologie (idconsultation, idpathologie) VALUES
  (9001, 1),            -- HTA
  (9002, 1),
  (9003, 15),           -- angine
  (9004, 3), (9004, 1), -- diabète type 2, HTA
  (9005, 3),
  (9006, 14),           -- rhinopharyngite
  (9007, 22),           -- infection urinaire
  (9008, 9),            -- BPCO
  (9009, 20),           -- RGO
  (9010, 6), (9010, 1), -- insuffisance cardiaque, HTA
  (9011, 30),           -- lombalgie commune
  (9012, 29),           -- migraine
  (9013, 12),           -- grippe
  (9014, 9);

-- Ordonnances (mode : 0 = soir, 1 = matin, 2 = indifférent ; durée en jours)
INSERT INTO prescrire (idMedicament, idConsultation, unite, posologie, duree, mode) VALUES
  (51, 9001, 1, 1, 30, 1),   -- amlodipine 5 mg
  (52, 9002, 1, 1, 90, 1),   -- amlodipine 10 mg
  (38, 9003, 1, 2, 8, 2),    -- amoxicilline/ac. clavulanique
  (27, 9003, 1, 3, 5, 2),    -- paracétamol 1 g
  (48, 9004, 1, 2, 90, 2),   -- metformine 850
  (54, 9004, 1, 1, 90, 1),   -- ramipril
  (48, 9005, 1, 3, 90, 2),
  (58, 9005, 1, 1, 90, 0),   -- atorvastatine le soir
  (28, 9006, 1, 3, 5, 2),    -- paracétamol sol buv
  (40, 9007, 1, 2, 7, 2),    -- ciprofloxacine
  (61, 9008, 2, 3, 30, 2),   -- salbutamol
  (60, 9008, 1, 1, 5, 1),    -- prednisolone
  (43, 9009, 1, 1, 28, 1),   -- ésoméprazole
  (57, 9010, 1, 1, 30, 1),   -- furosémide
  (53, 9010, 1, 1, 30, 1),   -- bisoprolol
  (32, 9011, 1, 2, 5, 2),    -- diclofénac
  (42, 9011, 1, 1, 5, 1),    -- oméprazole
  (27, 9012, 1, 3, 3, 2),
  (45, 9012, 1, 3, 3, 2),    -- métoclopramide
  (27, 9013, 1, 3, 5, 2),
  (62, 9013, 1, 1, 5, 0);    -- loratadine
-- (9014 : pas d'ordonnance, pour tester ce cas)

-- Demandes d'analyses (note d'interprétation 0..4 ; 0 = pas encore de résultat)
INSERT INTO demandeanalyse (idconsultation, idAnalyse, observation, resultat, note) VALUES
  (9001, 17, 'Bilan HTA',            '9,8 mg/l',            1),
  (9001, 7,  'Bilan HTA',            'Na 139 / K 4,1',      1),
  (9001, 15, NULL,                   '2,35 g/l',            3),
  (9003, 25, 'Syndrome infectieux',  'GB 14 500/mm3',       3),
  (9003, 3,  NULL,                   '48 mg/l',             3),
  (9004, 20, 'Suspicion de diabète', '1,62 g/l',            4),
  (9004, 19, NULL,                   '8,1 %',               4),
  (9005, 19, 'Contrôle',             '7,0 %',               2),
  (9005, 13, NULL,                   '1,05 g/l',            1),
  (9007, 52, 'Signes urinaires',     'E. coli > 10^5/ml',   4),
  (9010, 17, 'Œdèmes',               '14 mg/l',             2),
  (9010, 7,  NULL,                   'Na 134 / K 3,4',      3),
  (9013, 25, 'Syndrome grippal',     NULL,                  0),
  (9013, 3,  NULL,                   NULL,                  0),
  (9014, 25, 'Contrôle BPCO',        NULL,                  0);

-- ---------------------------------------------------------------------
-- Rendez-vous (utilisateur n° 1, médecin n° 1). Créneaux de 8 h à 17 h, par quarts
-- d'heure. prisLe : moment où le rendez-vous a été pris.
-- INSERT IGNORE : un créneau déjà occupé par un vrai rendez-vous est
-- laissé tel quel.
-- ---------------------------------------------------------------------
INSERT IGNORE INTO rendezvous (idUser, idPatient, idMedecin, `date`, heure, prisLe, etat) VALUES
  -- Passé
  (1, 9001, 1, DATE_SUB(CURDATE(), INTERVAL 60 DAY), '10:00:00', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 120 DAY), '09:40:00'), 0),
  (1, 9003, 1, DATE_SUB(CURDATE(), INTERVAL 30 DAY), '09:30:00', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 90 DAY),  '09:10:00'), 0),
  -- Aujourd'hui
  (1, 9005, 1, CURDATE(), '10:30:00', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 15 DAY), '15:20:00'), 0),
  (1, 9009, 1, CURDATE(), '14:00:00', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY),  '11:30:00'), 0),
  (1, 9007, 1, CURDATE(), '15:15:00', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY),  '16:20:00'), 0),
  (1, 9008, 1, CURDATE(), '16:45:00', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY),  '09:20:00'), 0),
  -- À venir
  (1, 9001, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY),  '09:00:00', TIMESTAMP(CURDATE(), '08:00:00'), 0),
  (1, 9004, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY),  '09:15:00', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 20 DAY), '14:40:00'), 0),
  (1, 9006, 1, DATE_ADD(CURDATE(), INTERVAL 2 DAY),  '11:00:00', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 10 DAY), '11:10:00'), 0),
  (1, 9002, 1, DATE_ADD(CURDATE(), INTERVAL 3 DAY),  '08:30:00', TIMESTAMP(CURDATE(), '08:50:00'), 0),
  (1, 9010, 1, DATE_ADD(CURDATE(), INTERVAL 5 DAY),  '13:45:00', TIMESTAMP(CURDATE(), '10:05:00'), 0),
  (1, 9003, 1, DATE_ADD(CURDATE(), INTERVAL 8 DAY),  '10:15:00', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 30 DAY), '09:50:00'), 0),
  (1, 9009, 1, DATE_ADD(CURDATE(), INTERVAL 14 DAY), '15:30:00', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY),  '11:35:00'), 0);

COMMIT;

-- Récapitulatif
SELECT 'patients de test'      AS donnees, COUNT(*) AS lignes FROM patient      WHERE idPatient BETWEEN 9001 AND 9999
UNION ALL SELECT 'consultations',          COUNT(*) FROM consultation WHERE idConsultation BETWEEN 9001 AND 9999
UNION ALL SELECT 'consultations du jour',  COUNT(*) FROM consultation WHERE idConsultation BETWEEN 9001 AND 9999 AND dateConsultation = CURDATE()
UNION ALL SELECT 'prescriptions',          COUNT(*) FROM prescrire    WHERE idConsultation BETWEEN 9001 AND 9999
UNION ALL SELECT 'demandes d''analyses',   COUNT(*) FROM demandeanalyse WHERE idconsultation BETWEEN 9001 AND 9999
UNION ALL SELECT 'rendez-vous',            COUNT(*) FROM rendezvous   WHERE idPatient BETWEEN 9001 AND 9999
UNION ALL SELECT 'rendez-vous du jour',    COUNT(*) FROM rendezvous   WHERE idPatient BETWEEN 9001 AND 9999 AND `date` = CURDATE();
