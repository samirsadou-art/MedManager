package My_application;

import My_application.controller.ConnexionController;
import My_application.controller.Vue;
import My_application.entities.User;
import My_application.service.AuthentificationService;
import My_application.service.MotDePasse;
import My_application.vue.Dialogues;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Écran de connexion, affiché avant la fenêtre principale.
 * <p>
 * À la première utilisation (aucun compte en base), il propose de choisir
 * l'identifiant et le mot de passe de l'utilisateur par défaut. Écrit à la
 * main (pas de fichier .form) : la mise en page tient en quelques lignes.
 */
public class ConnexionForm extends JDialog implements Vue {

    private final ConnexionController controleur;
    private final boolean creation;
    private final JTextField login = new JTextField(20);
    private final JPasswordField motDePasse = new JPasswordField(20);
    private final JPasswordField confirmation = new JPasswordField(20);
    private User connecte;

    private ConnexionForm(AuthentificationService service, boolean creation, User initial) {
        super((Dialog) null, "GestiMed : connexion", Dialog.ModalityType.APPLICATION_MODAL);
        this.controleur = new ConnexionController(this, service);
        this.creation = creation;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        construire(initial);
        Theme.harmoniser(this);
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Affiche l'écran de connexion et attend : renvoie l'utilisateur connecté,
     * ou null si l'utilisateur a quitté ou si la base est inaccessible.
     */
    public static User demander(AuthentificationService service) {
        // Avant que la fenêtre existe : les messages s'affichent sans parent.
        ConnexionController controleur = new ConnexionController(new Vue() {
            @Override
            public void afficherErreur(String message) {
                Dialogues.erreur(null, message);
            }

            @Override
            public void afficherInformation(String message) {
                Dialogues.information(null, message);
            }
        }, service);
        Boolean premiere = controleur.premiereUtilisation();
        if (premiere == null) {
            return null;
        }
        User initial = null;
        if (premiere) {
            initial = controleur.utilisateurInitial();
            if (initial == null) {
                return null;
            }
        }
        ConnexionForm f = new ConnexionForm(service, premiere, initial);
        f.setVisible(true);
        return f.connecte;
    }

    private void construire(User initial) {
        JPanel contenu = new JPanel(new BorderLayout(0, 16));
        contenu.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));
        contenu.setBackground(Theme.SURFACE);

        JLabel titre = new JLabel(creation ? "Première utilisation" : "Connexion à GestiMed",
                Icones.svg(creation ? "user-plus" : "user", 28, Theme.PRIMAIRE), JLabel.LEFT);
        titre.setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE + 5));
        titre.setForeground(Theme.TEXTE);
        titre.setIconTextGap(12);
        JPanel haut = new JPanel(new BorderLayout(0, 6));
        haut.setOpaque(false);
        haut.add(titre, BorderLayout.NORTH);
        if (creation) {
            JLabel aide = new JLabel("<html>Choisissez l'identifiant et le mot de passe de <b>" + echapper(initial.toString())
                    + "</b>.<br>Au moins " + MotDePasse.LONGUEUR_MIN + " caractères, dont une lettre et un chiffre.</html>");
            aide.setForeground(Theme.TEXTE_SECONDAIRE);
            haut.add(aide, BorderLayout.CENTER);
        }
        contenu.add(haut, BorderLayout.NORTH);

        JPanel champs = new JPanel(new GridBagLayout());
        champs.setOpaque(false);
        ligne(champs, 0, "Identifiant", login);
        ligne(champs, 1, "Mot de passe", motDePasse);
        if (creation) {
            ligne(champs, 2, "Confirmation", confirmation);
        }
        contenu.add(champs, BorderLayout.CENTER);

        JButton valider = new JButton(creation ? "Créer le compte" : "Se connecter");
        valider.setBackground(Theme.PRIMAIRE);
        valider.setForeground(java.awt.Color.WHITE);
        valider.setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE));
        JButton quitter = new JButton("Quitter");
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        boutons.setOpaque(false);
        boutons.add(quitter);
        boutons.add(valider);
        contenu.add(boutons, BorderLayout.SOUTH);

        valider.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                valider();
            }
        });
        quitter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        getRootPane().setDefaultButton(valider);
        setContentPane(contenu);
    }

    private static void ligne(JPanel panneau, int y, String libelle, JComponent champ) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.insets = new Insets(4, 0, 4, 12);
        c.anchor = GridBagConstraints.LINE_START;
        JLabel l = new JLabel(libelle);
        l.setForeground(Theme.TEXTE);
        panneau.add(l, c);
        c.gridx = 1;
        c.insets = new Insets(4, 0, 4, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        panneau.add(champ, c);
    }

    private void valider() {
        // getPassword() renvoie une copie que le contrôleur efface après usage.
        User u = creation
                ? controleur.creerPremierCompte(login.getText(), motDePasse.getPassword(), confirmation.getPassword())
                : controleur.connecter(login.getText(), motDePasse.getPassword());
        if (u != null) {
            connecte = u;
            dispose();
        } else {
            motDePasse.setText("");
            confirmation.setText("");
            // Après la fermeture du message d'erreur, qui rend d'abord le focus au bouton.
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    motDePasse.requestFocusInWindow();
                }
            });
        }
    }

    private static String echapper(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public void afficherErreur(String message) {
        Dialogues.erreur(this, message);
    }

    @Override
    public void afficherInformation(String message) {
        Dialogues.information(this, message);
    }
}
