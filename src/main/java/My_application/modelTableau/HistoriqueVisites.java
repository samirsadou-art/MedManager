package My_application.modelTableau;

import My_application.entities.DossierPatient;
import My_application.entities.consultation;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Locale;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

/**
 * Modèle d'arbre (vue) de l'historique des visites : Année → Mois → Jour [heure].
 * <p>
 * Déplacé du paquet {@code entities} vers la couche vue : c'est un modèle
 * d'affichage Swing, pas une donnée métier. Corrections : suppression de
 * l'import {@code com.sun.javafx.util.Utils} (classe interne JavaFX absente des
 * JDK récents) et des {@code Enumeration<DefaultMutableTreeNode>} qui ne
 * compilent plus à partir de Java 9.
 */
public class HistoriqueVisites {

    private final DossierPatient dossier;
    private final DefaultTreeModel racine = new DefaultTreeModel(null);
    private final DefaultMutableTreeNode visites;
    private final DefaultMutableTreeNode consult;

    public HistoriqueVisites(DossierPatient dossier) {
        this.dossier = dossier;
        visites = new DefaultMutableTreeNode(new NoeudHistorique(-1, "Visites", -1));
        consult = new DefaultMutableTreeNode(new NoeudHistorique(-1, "Consultations", -1));
        // Plus de branche « Rendez-Vous » : elle restait toujours vide, les
        // rendez-vous du patient se prennent par le bouton « Nouveau Rendez-vous ».
        visites.add(consult);
        racine.setRoot(visites);
    }

    public DefaultTreeModel getTreemodel() {
        return racine;
    }

    public DefaultMutableTreeNode getVisitesNoeud() {
        return visites;
    }

    public DefaultMutableTreeNode getConsultNoeud() {
        return consult;
    }

    public void chargerHistorique() {
        for (int i = 0; i < dossier.getNombreConsultation(); i++) {
            addConsulToHistorique(dossier.getConsultationAt(i), i);
        }
        racine.reload();
    }

    public void addConsulToHistorique(consultation myconsult, int index) {
        LocalDate date = myconsult.getDate_cons();
        // "yyyy" (année calendaire) et non "YYYY" (année de semaine ISO), qui
        // classait les consultations de fin décembre dans l'année suivante.
        DefaultMutableTreeNode annee = enfant(consult, date.format(DateTimeFormatter.ofPattern("yyyy")), index);
        DefaultMutableTreeNode mois = enfant(annee, date.format(DateTimeFormatter.ofPattern("MMMM", Locale.FRENCH)), index);
        String jour = date.format(DateTimeFormatter.ofPattern("EEEE,d ", Locale.FRENCH)) + "[" + myconsult.getHeureString() + "]";
        mois.add(new DefaultMutableTreeNode(new NoeudHistorique(index, jour, 0)));
        racine.nodeStructureChanged(consult);
    }

    /** Renvoie le fils portant ce libellé, en le créant s'il n'existe pas. */
    private static DefaultMutableTreeNode enfant(DefaultMutableTreeNode parent, String libelle, int index) {
        Enumeration<? extends TreeNode> e = parent.children();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
            if (libelle.equals(n.toString())) {
                return n;
            }
        }
        DefaultMutableTreeNode n = new DefaultMutableTreeNode(new NoeudHistorique(index, libelle, 0));
        parent.add(n);
        return n;
    }

    /** Cherche la feuille correspondant à la consultation d'indice donné. */
    public DefaultMutableTreeNode lookForNoeud(DefaultMutableTreeNode noeud, int index) {
        DefaultMutableTreeNode depart = noeud == visites ? consult : noeud;
        Enumeration<? extends TreeNode> e = depart.children();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode elemt = (DefaultMutableTreeNode) e.nextElement();
            if (!elemt.isLeaf()) {
                DefaultMutableTreeNode resultat = lookForNoeud(elemt, index);
                if (resultat != null) {
                    return resultat;
                }
            } else {
                NoeudHistorique nh = (NoeudHistorique) elemt.getUserObject();
                if (nh.getIndiceConsult() == index) {
                    return elemt;
                }
            }
        }
        return null;
    }
}
