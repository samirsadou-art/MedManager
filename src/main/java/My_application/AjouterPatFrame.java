/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application;

/**
 *
 * @author GLOBAL INFO LAGA
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;



public class AjouterPatFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	JButton ajouter;
	JButton annuler;
	JButton atacher;
	
	JPanel panelc;
	JPanel panelb;
	private JPanel panelh;
	
	
	private JLabel numeroLab;
	private JTextField numeroText;
	private JLabel numeroSecLab;
	private JTextField numeroSecText;
	private JLabel nomLab;
	private JTextField nomText;
	private JLabel prenomLab;
	private JTextField prenomText;
	
	private JLabel datNaissLab;
	
	private JLabel sexeLab;
	private JComboBox sexeText;
	
	private JLabel ageLab;
	private JComboBox ageText;
	private JComboBox jourText;
	private JComboBox moisText;
	private JComboBox anneeText;
	private JLabel adresseLab;

	private JLabel enteteLab;
    private JLabel photoLab;
    private JPanel paysPanel;
    private JLabel paysLabel;
    private JComboBox paysText;
    private JPanel wilayaPanel;
    private JLabel  wilayaLabel;
    private JComboBox wilayaText;
    private JPanel villePanel;
    private JLabel villeLabel;
    private JComboBox villeText;
    private JLabel telLab;
    private JTextField telText;
    private JLabel mailLab;
    private JTextField mailText;

	
	AjouterPatFrame(){
		super("Veuillez introduire les informations du patient");
               
		//setIconImage(Toolkit.getDefaultToolkit().getImage(this.class.getResource("/res/pills3.png")));
		this.setLocationRelativeTo(null);
		setAlwaysOnTop(true);
		this.disposerComposant();
		this.pack();
		this.setResizable(false);
		
		
	}
	
        @SuppressWarnings("unchecked")
	void disposerComposant(){
	panelc=new JPanel();
	//panelc.setMaximumSize(new Dimension(400, 400));
	panelc.setBorder(new EmptyBorder(15, 15, 15, 15));
	panelb=new JPanel();
	panelb.setBorder(new LineBorder(new Color(0, 204, 255)));
	FlowLayout fl_panelb = (FlowLayout) panelb.getLayout();
	fl_panelb.setHgap(50);
	GridBagLayout gbl_panelc = new GridBagLayout();
	gbl_panelc.rowWeights = new double[]{0.0, 0.0, 1.0, 1.0, 1.0,0.0};
	gbl_panelc.columnWeights = new double[]{0.0, 1.0, 1.0, 1.0, 0.0};
	gbl_panelc.columnWidths = new int[] {100, 100, 100, 100, 100};
	gbl_panelc.rowHeights = new int[] {32, 32, 32, 32, 32,32};
	panelc.setLayout(gbl_panelc);

	ajouter=new JButton("Ajouter");
	ajouter.setFont(new Font("Tahoma", Font.PLAIN, 12));
	ajouter.setIcon(null);
	annuler=new JButton("Annuler");
	annuler.setFont(new Font("Tahoma", Font.PLAIN, 12));
	annuler.setIcon(null);
	panelb.add(annuler);
	panelb.add(ajouter);
	
	panelc.getInsets().set(15, 15, 15, 15);
	this.getContentPane().add(panelc,BorderLayout.CENTER);
	
	numeroLab = new JLabel("Num\u00E9ro :");
	GridBagConstraints gbc_telLab = new GridBagConstraints();
	gbc_telLab.fill = GridBagConstraints.HORIZONTAL;
	gbc_telLab.insets = new Insets(0, 0, 5, 5);
	gbc_telLab.gridx = 0;
	gbc_telLab.gridy = 0;
	panelc.add(numeroLab, gbc_telLab);

	
	numeroText = new JTextField("");
	GridBagConstraints gbc_telText = new GridBagConstraints();
	gbc_telText.insets = new Insets(0, 0, 5, 5);
	gbc_telText.fill = GridBagConstraints.BOTH;
	gbc_telText.gridx = 1;
	gbc_telText.gridy = 0;
	panelc.add(numeroText, gbc_telText);
	
	numeroSecLab = new JLabel("Num\u00E9ro de s\u00E9curit\u00E9 sociale :");
	GridBagConstraints gbc_label_1 = new GridBagConstraints();
	gbc_label_1.fill = GridBagConstraints.HORIZONTAL;
	gbc_label_1.insets = new Insets(0, 0, 5, 5);
	gbc_label_1.gridx = 2;
	gbc_label_1.gridy = 0;
	panelc.add(numeroSecLab, gbc_label_1);
	
	numeroSecText = new JTextField("");
	GridBagConstraints gbc_textField_1 = new GridBagConstraints();
	gbc_textField_1.fill = GridBagConstraints.BOTH;
	gbc_textField_1.insets = new Insets(0, 0, 5, 5);
	gbc_textField_1.gridx = 3;
	gbc_textField_1.gridy = 0;
	panelc.add(numeroSecText, gbc_textField_1);
	
	photoLab = new JLabel("");
	photoLab.setBackground(new Color(240, 255, 240));
	GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
	gbc_lblNewLabel_1.fill = GridBagConstraints.BOTH;
	gbc_lblNewLabel_1.gridheight = 5;
	gbc_lblNewLabel_1.gridx = 4;
	gbc_lblNewLabel_1.gridy = 0;
	panelc.add(photoLab, gbc_lblNewLabel_1);
	photoLab.setHorizontalAlignment(SwingConstants.CENTER);
	photoLab.setIcon(new javax.swing.ImageIcon(getClass().getResource("/My_application/source/pills5.png")));
	
	nomLab = new JLabel("Nom :");
	GridBagConstraints gbc_label_2 = new GridBagConstraints();
	gbc_label_2.fill = GridBagConstraints.HORIZONTAL;
	gbc_label_2.insets = new Insets(0, 0, 5, 5);
	gbc_label_2.gridx = 0;
	gbc_label_2.gridy = 1;
	panelc.add(nomLab, gbc_label_2);
	
	nomText = new JTextField("");
	GridBagConstraints gbc_textField_2 = new GridBagConstraints();
	gbc_textField_2.insets = new Insets(0, 0, 5, 5);
	gbc_textField_2.fill = GridBagConstraints.BOTH;
	gbc_textField_2.gridx = 1;
	gbc_textField_2.gridy = 1;
	panelc.add(nomText, gbc_textField_2);
	
	prenomLab = new JLabel("Pr\u00E9nom :");
	GridBagConstraints gbc_label_3 = new GridBagConstraints();
	gbc_label_3.fill = GridBagConstraints.HORIZONTAL;
	gbc_label_3.insets = new Insets(0, 0, 5, 5);
	gbc_label_3.gridx = 2;
	gbc_label_3.gridy = 1;
	panelc.add(prenomLab, gbc_label_3);
	
	prenomText= new JTextField("");
	GridBagConstraints gbc_textField_3 = new GridBagConstraints();
	gbc_textField_3.fill = GridBagConstraints.BOTH;
	gbc_textField_3.insets = new Insets(0, 0, 5, 5);
	gbc_textField_3.gridx = 3;
	gbc_textField_3.gridy = 1;
	panelc.add(prenomText, gbc_textField_3);

	sexeLab = new JLabel("Sexe :");
	GridBagConstraints gbc_label_9 = new GridBagConstraints();
	gbc_label_9.fill = GridBagConstraints.HORIZONTAL;
	gbc_label_9.insets = new Insets(0, 0, 5, 5);
	gbc_label_9.gridx = 0;
	gbc_label_9.gridy = 2;
	panelc.add(sexeLab, gbc_label_9);
	
	sexeText = new JComboBox();
	sexeText.addItemListener(new ItemListener() {
                @Override
		public void itemStateChanged(ItemEvent arg) {
			if (arg.getItem()=="Masculin")
			{
				photoLab.setIcon(new javax.swing.ImageIcon(getClass().getResource("/My_application/source/male.png")));	
			}
			else
			{
				photoLab.setIcon(new javax.swing.ImageIcon(getClass().getResource("/My_application/source/female.png")));
			}
		}
	});
	sexeText.setModel(new DefaultComboBoxModel(new String[] {"Masculin", "Féminin"}));
	GridBagConstraints gbc_comboBox_3 = new GridBagConstraints();
	gbc_comboBox_3.fill = GridBagConstraints.HORIZONTAL;
	gbc_comboBox_3.insets = new Insets(0, 0, 5, 5);
	gbc_comboBox_3.gridx = 1;
	gbc_comboBox_3.gridy = 2;
	panelc.add(sexeText, gbc_comboBox_3);
	
	ageLab = new JLabel("Age :");
	GridBagConstraints gbc_label_10 = new GridBagConstraints();
	gbc_label_10.fill = GridBagConstraints.HORIZONTAL;
	gbc_label_10.insets = new Insets(0, 0, 5, 5);
	gbc_label_10.gridx = 2;
	gbc_label_10.gridy = 2;
	panelc.add(ageLab, gbc_label_10);
	


	
	ageText = new JComboBox();
	ageText.setModel(new DefaultComboBoxModel(new String[] {"25"}));
	for(int i=1;i<=150;i++)
		ageText.addItem(String.valueOf(i));
	GridBagConstraints gbc_ageText = new GridBagConstraints();
	gbc_ageText.fill = GridBagConstraints.BOTH;
	gbc_ageText.insets = new Insets(0, 0, 5, 5);
	gbc_ageText.gridx = 3;
	gbc_ageText.gridy = 2;
	panelc.add(ageText, gbc_ageText);
	ageText.setEditable(true);
	
	datNaissLab = new JLabel("Date de naissance :");
	GridBagConstraints gbc_label_4 = new GridBagConstraints();
	gbc_label_4.anchor = GridBagConstraints.EAST;
	gbc_label_4.insets = new Insets(0, 0, 5, 5);
	gbc_label_4.gridx = 0;
	gbc_label_4.gridy = 3;
	panelc.add(datNaissLab, gbc_label_4);
	
	jourText = new JComboBox();
	jourText.setEditable(true);
	GridBagConstraints gbc_comboBox_2 = new GridBagConstraints();
	gbc_comboBox_2.insets = new Insets(0, 0, 5, 5);
	gbc_comboBox_2.fill = GridBagConstraints.HORIZONTAL;
	gbc_comboBox_2.gridx = 1;
	gbc_comboBox_2.gridy = 3;
	panelc.add(jourText, gbc_comboBox_2);
	
	moisText = new JComboBox();
	moisText.setEditable(true);
	GridBagConstraints gbc_comboBox_1 = new GridBagConstraints();
	gbc_comboBox_1.insets = new Insets(0, 0, 5, 5);
	gbc_comboBox_1.fill = GridBagConstraints.HORIZONTAL;
	gbc_comboBox_1.gridx = 2;
	gbc_comboBox_1.gridy = 3;
	panelc.add(moisText, gbc_comboBox_1);
	
	for(int i=1;i<=31;i++){
		jourText.addItem(String.valueOf(i));
		if (i<=12)
			moisText.addItem(String.valueOf(i));
	}
	
	anneeText = new JComboBox();
	
	for(int i=1930;i<=2900;i++)
		anneeText.addItem(String.valueOf(i));
	anneeText.setEditable(true);
	GridBagConstraints gbc_comboBox = new GridBagConstraints();
	gbc_comboBox.insets = new Insets(0, 0, 5, 5);
	gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
	gbc_comboBox.gridx = 3;
	gbc_comboBox.gridy = 3;
	panelc.add(anneeText, gbc_comboBox);
	
	adresseLab = new JLabel("Adresse :");
	GridBagConstraints gbc_label_8 = new GridBagConstraints();
	gbc_label_8.fill = GridBagConstraints.HORIZONTAL;
	gbc_label_8.insets = new Insets(0, 0, 5, 5);
	gbc_label_8.gridx = 0;
	gbc_label_8.gridy = 4;
	panelc.add(adresseLab, gbc_label_8);
	
	paysPanel = new JPanel();
	FlowLayout flowLayout_4 = (FlowLayout) paysPanel.getLayout();
	flowLayout_4.setHgap(0);
	flowLayout_4.setAlignment(FlowLayout.RIGHT);
	GridBagConstraints gbc_panel_3 = new GridBagConstraints();
	gbc_panel_3.insets = new Insets(0, 0, 5, 5);
	gbc_panel_3.fill = GridBagConstraints.BOTH;
	gbc_panel_3.gridx = 1;
	gbc_panel_3.gridy = 4;
	panelc.add(paysPanel, gbc_panel_3);
	
	paysLabel = new JLabel("Pays : ");
	paysPanel.add(paysLabel);
	
	paysText = new JComboBox();
	paysText.setEditable(true);
	paysText.setModel(new DefaultComboBoxModel(new String[] {"Alg\u00E9rie"}));
	paysPanel.add(paysText);
	
	wilayaPanel= new JPanel();
	FlowLayout flowLayout_3 = (FlowLayout) wilayaPanel.getLayout();
	flowLayout_3.setHgap(0);
	flowLayout_3.setAlignment(FlowLayout.RIGHT);
	GridBagConstraints gbc_panel_4 = new GridBagConstraints();
	gbc_panel_4.insets = new Insets(0, 0, 5, 5);
	gbc_panel_4.fill = GridBagConstraints.BOTH;
	gbc_panel_4.gridx = 2;
	gbc_panel_4.gridy = 4;
	panelc.add(wilayaPanel, gbc_panel_4);
	
	wilayaLabel = new JLabel("Wilaya : ");
	wilayaPanel.add(wilayaLabel);
	
	wilayaText = new JComboBox();
	wilayaText .setModel(new DefaultComboBoxModel(new String[] {"Tizi-Ouzou", "B\u00E9jaia", "Alger", "Annaba", "Tipaza"}));
	wilayaText .setEditable(true);
	wilayaPanel.add(wilayaText );
	
	villePanel = new JPanel();
	FlowLayout flowLayout_5 = (FlowLayout) villePanel.getLayout();
	flowLayout_5.setHgap(0);
	flowLayout_5.setAlignment(FlowLayout.RIGHT);
	GridBagConstraints gbc_panel_5 = new GridBagConstraints();
	gbc_panel_5.insets = new Insets(0, 0, 5, 5);
	gbc_panel_5.fill = GridBagConstraints.BOTH;
	gbc_panel_5.gridx = 3;
	gbc_panel_5.gridy = 4;
	panelc.add(villePanel, gbc_panel_5);
	
	villeLabel = new JLabel("Ville  : ");
	villeLabel.setHorizontalAlignment(SwingConstants.LEFT);
	villePanel.add(villeLabel);
	
	villeText = new JComboBox();
	villeText.setModel(new DefaultComboBoxModel(new String[] {"Freha", "Tizi-Ouzou"}));
	villeText.setEditable(true);
	villePanel.add(villeText);
	
	telLab = new JLabel("T\u00E9l\u00E9phone : ");
	telLab.setHorizontalAlignment(SwingConstants.LEFT);
	GridBagConstraints gbctelLab = new GridBagConstraints();
	gbctelLab.fill = GridBagConstraints.HORIZONTAL;
	gbctelLab.anchor = GridBagConstraints.EAST;
	gbctelLab.insets = new Insets(0, 0, 0, 5);
	gbctelLab.gridx = 0;
	gbctelLab.gridy = 5;
	panelc.add(telLab, gbctelLab);
	
	telText = new JTextField();
	GridBagConstraints gbctelText = new GridBagConstraints();
	gbctelText.insets = new Insets(0, 0, 5, 5);
	gbctelText.fill = GridBagConstraints.BOTH;
	gbctelText.gridx = 1;
	gbctelText.gridy = 5;
	panelc.add(telText, gbctelText);
	telText.setColumns(10);
	
	mailLab = new JLabel("E-Mail : ");
	GridBagConstraints gbc_mailLab = new GridBagConstraints();
	gbc_mailLab.fill = GridBagConstraints.HORIZONTAL;
	gbc_mailLab.anchor = GridBagConstraints.EAST;
	gbc_mailLab.insets = new Insets(0, 0, 0, 5);
	gbc_mailLab.gridx = 2;
	gbc_mailLab.gridy = 5;
	panelc.add(mailLab, gbc_mailLab);
	
	mailText = new JTextField();
	GridBagConstraints gbc_mailText = new GridBagConstraints();
	gbc_mailText.insets = new Insets(0, 0, 5, 5);
	gbc_mailText.fill = GridBagConstraints.BOTH;
	gbc_mailText.gridx = 3;
	gbc_mailText.gridy = 5;
	panelc.add(mailText, gbc_mailText);
	mailText.setColumns(10);
	
	atacher = new JButton("Attacher une photo");
	atacher.setFont(new Font("Tahoma", Font.PLAIN, 12));
	GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
	gbc_btnNewButton.insets = new Insets(0, 10, 0, 10);
	gbc_btnNewButton.gridx = 4;
	gbc_btnNewButton.gridy = 5;
	panelc.add(atacher, gbc_btnNewButton);
	

	this.getContentPane().add(panelb,BorderLayout.SOUTH);
	
	panelh = new JPanel();
	panelh.setBorder(new EmptyBorder(0, 0, 5, 0));
	panelh.setBackground(new Color(102, 204, 255));
	FlowLayout fl_panelh = (FlowLayout) panelh.getLayout();
	fl_panelh.setAlignment(FlowLayout.LEFT);
	getContentPane().add(panelh, BorderLayout.NORTH);
	
	enteteLab = new JLabel("Ajouter un patient ");
	enteteLab.setFont(new Font("Tahoma", Font.BOLD, 20));
	enteteLab.setForeground(Color.WHITE);
	panelh.add(enteteLab);
	

	
	
		
	}
	
	public Insets getInsets() {
		Insets normal = super.getInsets();
		return new Insets(normal.top + 5, normal.left +5,
		normal.bottom + 5, normal.right + 5);
		}
	
	
}
