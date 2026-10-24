package My_application.admin;

import My_application.controller.MembresController;
import My_application.controller.MembresVue;
import My_application.entities.Role;
import My_application.entities.User;
import My_application.entities.medecin;
import My_application.service.MembresService;
import My_application.service.MotDePasse;
import My_application.vue.Dialogues;
import My_application.vue.Formulaire;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Page « Membres du cabinet » du compte administrateur.
 * <ul>
 * <li>comptes de connexion : ajouter un membre (avec identifiant et mot de
 * passe provisoire), modifier son nom ou son rôle, lui donner un nouveau mot
 * de passe provisoire, l'activer ou le désactiver ;</li>
 * <li>fiches médecin : coordonnées imprimées sur les ordonnances, et
 * désactivation quand un médecin quitte le cabinet.</li>
 * </ul>
 */
public class PageMembres extends JPanel implements MembresVue {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter CONNEXION =
            DateTimeFormatter.ofPattern("d MMM yyyy 'à' HH:mm", Locale.FRENCH);
    /** Choix « nouvelle fiche » dans la liste des médecins d'un nouveau compte médecin. */
    private static final medecin NOUVELLE_FICHE = new medecin();

    private final transient MembresController controleur;
    private final ModeleMembres modele = new ModeleMembres();
    private final ModeleMedecins modeleMedecins = new ModeleMedecins();
    private final JTable table = new JTable(modele);
    private final JTable tableMedecins = new JTable(modeleMedecins);
    private final JButton modifierBtn = bouton("Modifier…", "pencil");
    private final JButton motDePasseBtn = bouton("Mot de passe provisoire…", "key");
    private final JButton actifBtn = bouton("Désactiver", "lock");
    private final JButton ficheBtn = bouton("Modifier la fiche…", "pencil");
    private final JButton medecinActifBtn = bouton("Désactiver", "lock");

    public PageMembres(MembresService membresService) {
        super(new BorderLayout(0, 12));
        setBackground(Theme.FOND);
        setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

        JButton ajouterBtn = bouton("Ajouter un membre…", "user-plus");
        ajouterBtn.addActionListener(e -> ajouter());
        modifierBtn.addActionListener(e -> modifier());
        motDePasseBtn.addActionListener(e -> motDePasseProvisoire());
        actifBtn.addActionListener(e -> basculerActif());
        add(carte("Comptes de connexion", table, ajouterBtn, modifierBtn, motDePasseBtn, actifBtn),
                BorderLayout.CENTER);

        ficheBtn.addActionListener(e -> modifierFiche());
        medecinActifBtn.addActionListener(e -> basculerMedecinActif());
        JPanel medecins = carte("Fiches médecin", tableMedecins, ficheBtn, medecinActifBtn);
        medecins.setPreferredSize(new Dimension(100, 230));
        add(medecins, BorderLayout.SOUTH);

        for (JTable t : new JTable[] {table, tableMedecins}) {
            t.setRowHeight(30);
            t.setFillsViewportHeight(true);
            t.setAutoCreateRowSorter(true);
            t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            t.setDefaultRenderer(Object.class, new Rendu());
            t.getSelectionModel().addListSelectionListener(e -> majBoutons());
        }
        Theme.harmoniser(this);
        controleur = new MembresController(this, membresService);
        majBoutons();
    }

    public void rafraichir() {
        controleur.rafraichir();
    }

    @Override
    public void afficherMembres(List<User> membres, List<medecin> medecins) {
        modele.setDonnees(membres, medecins);
        modeleMedecins.setDonnees(medecins);
        majBoutons();
    }

    @Override
    public void afficherErreur(String message) {
        Dialogues.erreur(this, message);
    }

    @Override
    public void afficherInformation(String message) {
        Dialogues.information(this, message);
    }

    // ----- actions -----

    private void ajouter() {
        JTextField nom = new JTextField(22);
        JTextField prenom = new JTextField(22);
        JComboBox<Role> role = listeRoles();
        role.setSelectedItem(Role.SECRETAIRE);
        JComboBox<medecin> medecin = listeMedecins(true);
        JTextField ordre = new JTextField(22);
        JTextField specialite = new JTextField(22);
        JTextField login = new JTextField(22);
        JPasswordField mdp = new JPasswordField(22);
        JPasswordField repete = new JPasswordField(22);
        Runnable maj = () -> {
            boolean estMedecin = role.getSelectedItem() == Role.MEDECIN;
            medecin.setEnabled(estMedecin);
            boolean fiche = estMedecin && medecin.getSelectedItem() == NOUVELLE_FICHE;
            ordre.setEnabled(fiche);
            specialite.setEnabled(fiche);
        };
        role.addActionListener(e -> maj.run());
        medecin.addActionListener(e -> maj.run());
        maj.run();
        Formulaire f = new Formulaire(this, "Ajouter un membre", "user-plus",
                "Le membre choisira son propre mot de passe à sa première connexion. Pour un médecin qui "
                + "arrive au cabinet, choisissez « Nouvelle fiche » : sa fiche (imprimée sur les ordonnances) "
                + "est créée en même temps.", "Créer le compte")
                .ligne("Nom", nom).ligne("Prénom", prenom).ligne("Rôle", role)
                .ligne("Fiche médecin", medecin).ligne("N° d'ordre", ordre).ligne("Spécialité", specialite)
                .ligne("Identifiant", login).ligne("Mot de passe provisoire", mdp).ligne("Confirmation", repete);
        f.afficher(() -> {
            Role r = (Role) role.getSelectedItem();
            medecin choisi = r == Role.MEDECIN ? (medecin) medecin.getSelectedItem() : null;
            medecin fiche = null;
            if (choisi == NOUVELLE_FICHE) {
                fiche = new medecin();
                fiche.setNom(nom.getText());
                fiche.setPrenom(prenom.getText());
                fiche.setOrdmedcin(ordre.getText());
                fiche.setSpecialite(specialite.getText());
            }
            Integer id = choisi == null || choisi == NOUVELLE_FICHE ? null : choisi.getIdmedecin();
            return controleur.creerCompte(nom.getText(), prenom.getText(), r, id, fiche, login.getText(),
                    mdp.getPassword(), repete.getPassword());
        });
    }

    private void modifier() {
        User u = membreSelectionne();
        if (u == null) {
            return;
        }
        JTextField nom = new JTextField(u.getNom(), 22);
        JTextField prenom = new JTextField(u.getPrenom(), 22);
        JComboBox<Role> role = listeRoles();
        role.setSelectedItem(u.getRole());
        JComboBox<medecin> medecin = listeMedecins(false);
        for (int i = 0; i < medecin.getItemCount(); i++) {
            if (u.getIdMedecin() != null && medecin.getItemAt(i).getIdmedecin() == u.getIdMedecin()) {
                medecin.setSelectedIndex(i);
            }
        }
        Runnable maj = () -> medecin.setEnabled(role.getSelectedItem() == Role.MEDECIN);
        role.addActionListener(e -> maj.run());
        maj.run();
        new Formulaire(this, "Modifier " + u, "pencil", "Identifiant : " + (u.getLogin() == null ? "aucun"
                : u.getLogin()) + ". L'identifiant ne change pas.", "Enregistrer")
                .ligne("Nom", nom).ligne("Prénom", prenom).ligne("Rôle", role).ligne("Fiche médecin", medecin)
                .afficher(() -> {
                    medecin m = (medecin) medecin.getSelectedItem();
                    return controleur.modifier(u.getIdent(), nom.getText(), prenom.getText(),
                            (Role) role.getSelectedItem(), m == null ? null : m.getIdmedecin());
                });
    }

    private void motDePasseProvisoire() {
        User u = membreSelectionne();
        if (u == null) {
            return;
        }
        JPasswordField mdp = new JPasswordField(22);
        JPasswordField repete = new JPasswordField(22);
        new Formulaire(this, "Mot de passe provisoire", "key", "Pour " + u + " (identifiant « " + u.getLogin()
                + " »), qui a oublié son mot de passe. Communiquez-le-lui de vive voix : il devra en choisir un "
                + "autre à sa prochaine connexion. Au moins " + MotDePasse.LONGUEUR_MIN
                + " caractères, dont une lettre et un chiffre.", "Enregistrer")
                .ligne("Mot de passe provisoire", mdp).ligne("Confirmation", repete)
                .afficher(() -> controleur.motDePasseProvisoire(u, mdp.getPassword(), repete.getPassword()));
    }

    private void basculerActif() {
        User u = membreSelectionne();
        if (u == null) {
            return;
        }
        boolean activer = !u.isActif();
        if (activer || Dialogues.confirmer(this, "Désactiver le compte de " + u + " ?\n"
                + "Il ne pourra plus se connecter ; son historique est conservé.")) {
            controleur.definirActif(u, activer);
        }
    }

    private void modifierFiche() {
        medecin m = medecinSelectionne();
        if (m == null) {
            return;
        }
        JTextField titre = new JTextField(m.getTitre(), 24);
        JTextField nom = new JTextField(m.getNom(), 24);
        JTextField prenom = new JTextField(m.getPrenom(), 24);
        JTextField ordre = new JTextField(m.getOrdmedcin(), 24);
        JTextField specialite = new JTextField(m.getSpecialite(), 24);
        JTextField adresse = new JTextField(m.getAdresse(), 24);
        JTextField tel1 = new JTextField(m.getTelephone1(), 24);
        JTextField tel2 = new JTextField(m.getTelephone2(), 24);
        JTextField email = new JTextField(m.getEmail(), 24);
        JTextField cabinet = new JTextField(m.getNom_cabinet(), 24);
        new Formulaire(this, "Fiche de " + m, "stethoscope",
                "Ces coordonnées sont imprimées en tête des ordonnances et des demandes d'analyses.", "Enregistrer")
                .ligne("Titre", titre).ligne("Nom", nom).ligne("Prénom", prenom).ligne("N° d'ordre", ordre)
                .ligne("Spécialité", specialite).ligne("Adresse", adresse).ligne("Téléphone", tel1)
                .ligne("Téléphone 2", tel2).ligne("E-mail", email).ligne("Cabinet", cabinet)
                .afficher(() -> {
                    medecin f = new medecin();
                    f.setIdmedecin(m.getIdmedecin());
                    f.setActif(m.isActif());
                    f.setTitre(titre.getText());
                    f.setNom(nom.getText());
                    f.setPrenom(prenom.getText());
                    f.setOrdmedcin(ordre.getText());
                    f.setSpecialite(specialite.getText());
                    f.setAdresse(adresse.getText());
                    f.setTelephone1(tel1.getText());
                    f.setTelephone2(tel2.getText());
                    f.setEmail(email.getText());
                    f.setNom_cabinet(cabinet.getText());
                    return controleur.modifierMedecin(f);
                });
    }

    private void basculerMedecinActif() {
        medecin m = medecinSelectionne();
        if (m == null) {
            return;
        }
        boolean activer = !m.isActif();
        if (activer || Dialogues.confirmer(this, "Désactiver la fiche de " + m + " ?\n"
                + "Il ne sera plus proposé dans l'agenda ; ses consultations sont conservées.")) {
            controleur.definirMedecinActif(m, activer);
        }
    }

    // ----- aides -----

    private User membreSelectionne() {
        int ligne = table.getSelectedRow();
        return ligne < 0 ? null : modele.membre(table.convertRowIndexToModel(ligne));
    }

    private medecin medecinSelectionne() {
        int ligne = tableMedecins.getSelectedRow();
        return ligne < 0 ? null : modeleMedecins.medecin(tableMedecins.convertRowIndexToModel(ligne));
    }

    private void majBoutons() {
        User u = membreSelectionne();
        modifierBtn.setEnabled(u != null);
        motDePasseBtn.setEnabled(u != null && u.getLogin() != null && u.isActif());
        actifBtn.setEnabled(u != null);
        actifBtn.setText(u != null && !u.isActif() ? "Activer" : "Désactiver");
        medecin m = medecinSelectionne();
        ficheBtn.setEnabled(m != null);
        medecinActifBtn.setEnabled(m != null);
        medecinActifBtn.setText(m != null && !m.isActif() ? "Activer" : "Désactiver");
    }

    /** Fiches actives, et « Nouvelle fiche » en tête pour un nouveau médecin. */
    private JComboBox<medecin> listeMedecins(boolean avecNouvelle) {
        JComboBox<medecin> liste = new JComboBox<>();
        if (avecNouvelle) {
            liste.addItem(NOUVELLE_FICHE);
        }
        for (medecin m : modeleMedecins.donnees) {
            if (m.isActif()) {
                liste.addItem(m);
            }
        }
        liste.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> l, Object valeur, int index, boolean selectionne,
                    boolean focus) {
                Object texte = valeur == NOUVELLE_FICHE ? "Nouvelle fiche (médecin qui arrive)" : valeur;
                return super.getListCellRendererComponent(l, texte, index, selectionne, focus);
            }
        });
        return liste;
    }

    private static JComboBox<Role> listeRoles() {
        JComboBox<Role> liste = new JComboBox<>(Role.values());
        liste.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> l, Object valeur, int index, boolean selectionne,
                    boolean focus) {
                Object texte = valeur instanceof Role ? ((Role) valeur).getLibelle() : valeur;
                return super.getListCellRendererComponent(l, texte, index, selectionne, focus);
            }
        });
        return liste;
    }

    private static JButton bouton(String texte, String icone) {
        return new JButton(texte, Icones.svg(icone, 16, Theme.PRIMAIRE));
    }

    private static JPanel carte(String titre, JTable table, JButton... boutons) {
        JPanel barre = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        barre.setOpaque(false);
        for (JButton b : boutons) {
            barre.add(b);
        }
        JPanel carte = new JPanel(new BorderLayout(0, 8));
        carte.add(barre, BorderLayout.NORTH);
        JScrollPane defilement = new JScrollPane(table);
        carte.add(defilement, BorderLayout.CENTER);
        Theme.carte(carte, titre);
        return carte;
    }

    /** Cellules du thème ; lignes désactivées en gris, « à changer » en orange. */
    private static final class Rendu extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object valeur, boolean selectionne, boolean focus,
                int ligne, int colonne) {
            super.getTableCellRendererComponent(t, valeur, selectionne, focus, ligne, colonne);
            Theme.cellule(this, t, selectionne, ligne);
            String texte = valeur == null ? "" : valeur.toString();
            if (!selectionne) {
                if (DESACTIVE.equals(texte) || A_CHANGER.equals(texte)) {
                    setForeground(DESACTIVE.equals(texte) ? Theme.TEXTE_SECONDAIRE : Theme.AVERTISSEMENT);
                } else {
                    setForeground(Theme.TEXTE);
                }
            }
            return this;
        }
    }

    static final String DESACTIVE = "Désactivé";
    static final String A_CHANGER = "À changer";

    /** Une ligne par membre ; colonnes en lecture seule. */
    static final class ModeleMembres extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private static final String[] COLONNES =
            {"Nom", "Rôle", "Identifiant", "Médecin", "Dernière connexion", "Mot de passe", "État"};

        private final List<User> donnees = new ArrayList<>();
        private final Map<Integer, String> medecins = new HashMap<>();

        void setDonnees(List<User> membres, List<medecin> fiches) {
            donnees.clear();
            donnees.addAll(membres);
            medecins.clear();
            for (medecin m : fiches) {
                medecins.put(m.getIdmedecin(), m.toString());
            }
            fireTableDataChanged();
        }

        User membre(int ligne) {
            return donnees.get(ligne);
        }

        @Override
        public int getRowCount() {
            return donnees.size();
        }

        @Override
        public int getColumnCount() {
            return COLONNES.length;
        }

        @Override
        public String getColumnName(int colonne) {
            return COLONNES[colonne];
        }

        @Override
        public Object getValueAt(int ligne, int colonne) {
            User u = donnees.get(ligne);
            switch (colonne) {
                case 0:
                    return u.toString();
                case 1:
                    return u.getRole().getLibelle();
                case 2:
                    return u.getLogin() == null ? "(aucun)" : u.getLogin();
                case 3:
                    return u.getIdMedecin() == null ? "" : medecins.getOrDefault(u.getIdMedecin(),
                            "n° " + u.getIdMedecin());
                case 4:
                    return u.getDerniereConnexion() == null ? "Jamais" : CONNEXION.format(u.getDerniereConnexion());
                case 5:
                    return u.getLogin() == null ? "" : u.isDoitChanger() ? A_CHANGER : "Choisi";
                default:
                    return u.isActif() ? "Actif" : DESACTIVE;
            }
        }
    }

    /** Une ligne par fiche médecin. */
    static final class ModeleMedecins extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private static final String[] COLONNES = {"Médecin", "N° d'ordre", "Spécialité", "Téléphone", "État"};

        private final List<medecin> donnees = new ArrayList<>();

        void setDonnees(List<medecin> fiches) {
            donnees.clear();
            donnees.addAll(fiches);
            fireTableDataChanged();
        }

        medecin medecin(int ligne) {
            return donnees.get(ligne);
        }

        @Override
        public int getRowCount() {
            return donnees.size();
        }

        @Override
        public int getColumnCount() {
            return COLONNES.length;
        }

        @Override
        public String getColumnName(int colonne) {
            return COLONNES[colonne];
        }

        @Override
        public Object getValueAt(int ligne, int colonne) {
            medecin m = donnees.get(ligne);
            switch (colonne) {
                case 0:
                    return m.toString();
                case 1:
                    return m.getOrdmedcin();
                case 2:
                    return m.getSpecialite();
                case 3:
                    return m.getTelephone1();
                default:
                    return m.isActif() ? "Actif" : DESACTIVE;
            }
        }
    }
}
