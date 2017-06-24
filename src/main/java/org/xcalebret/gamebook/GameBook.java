package org.xcalebret.gamebook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Scanner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xcalebret.gamebook.model.Choice;
import org.xcalebret.gamebook.model.Game;
import org.xcalebret.gamebook.model.Save;
import org.xcalebret.gamebook.model.Step;

public class GameBook implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameBook.class);
	private static final int EXIT_CODE = -1;
	private static final int BAKCUP_CODE = -2;
	private static final int NO_ACTION_CODE = -3;
	private static final String DEFAULT_CONFIG = "src/main/resources/properties.config";
	
	private String dataPath;
	private String savePath;
	private Game game;
	private Scanner scanner;
	private Step currentStep;
	private Step backupStep;
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		/* Build game based on runtime arguments */
		if (args != null && args.length > 0) {
			GameBook.LOGGER.debug("Jeu paramétré manuellement à l'execution.");
			new GameBook(args[0], args.length > 1 ? args[1]: null).run();

		/* Build game based on embeded config file */
		} else {
			try {
				Properties properties = new Properties();
				properties.load(new FileInputStream(GameBook.DEFAULT_CONFIG));
				new GameBook(
						properties.getProperty("default_game"),
						properties.getProperty("default_backup")).run();
				
			} catch (IOException e) {
				String msg = "Impossible de charger le jeu par défaut.";
				GameBook.LOGGER.error(msg);
				GameBook.LOGGER.debug(msg, e);
			}
		}
	}
	
	/**
	 * 
	 * @param dataPath
	 */
	public GameBook(String dataPath, String savePath) {
		
		this.dataPath = dataPath;
		this.savePath = savePath;
		this.scanner = new Scanner(System.in);
	}

	/**
	 * 
	 */
	@Override
	public void run() {
		
		if (this.checkDataPath()) {
			this.game = this.parseDatas();
			this.wrapGame(); // ajout des options au jeu
			if (this.game != null) {
				this.checkForResume(); // restaurer la partie si besoin.
				GameBook.LOGGER.debug("Nom du jeu paramétré : " + this.game.title);
				GameBook.LOGGER.debug("Nombre d'étapes dans le jeu : " + this.game.steps.size());
				this.startGame();
			}
		} else {
			this.showUsage();
		}
	}


	/**
	 * Initialise la valeur de l'étape courante pour commencer le jeu.
	 */
	private void startGame() {
		
		/* Démarrage du jeu au 1er step d'identifiant 0 si aucun backup n'a été restauré */
		if (this.currentStep == null) {
			this.currentStep = this.game.getById(0);
		}
		while(this.continueGame()) {
			this.displayGame();
			this.interactGame();
		}
		/* Afficher les information de l'étape de fin. */
		if (this.currentStep != null) {
			GameBook.LOGGER.info(this.currentStep.description);
			GameBook.LOGGER.info(this.currentStep.question);
		}
		GameBook.LOGGER.info("--- Le jeu vient de se terminer ---");
		this.scanner.close();
	}

	/**
	 * Restaurer la partie si ncessaire.
	 */
	private void checkForResume() {
		
		boolean stepMatch = false;
		if (this.savePath != null) {
			Save save = this.parseBackup();
			if (save != null && save.gameId.equals(this.game.id)) {
				for (Step step : this.game.steps) {
					if (step.id == save.lastStep) {
						this.currentStep = step;
						stepMatch = true;
						break;
					}
				}
				if (!stepMatch) {
					GameBook.LOGGER.error("La sauvegarde fait référence à une étape invalide.Le jeu redémarre depuis le début.");
					GameBook.LOGGER.error("Le jeu redémarre depuis le début.");
				}
			}
		}
	}

	/**
	 * Demande d'une saisie utilisateur pour demander le choix de l'étape courante.
	 * Puis mise à jour de l'étape courante avec la valeur du gotostep.
	 */
	private void interactGame() {
		
		boolean isValidChoice = false;
		
		while (!isValidChoice) {
			GameBook.LOGGER.info(">>> ");
			try {
				int choixIndex = this.scanner.nextInt();
				if (choixIndex >= 0 && choixIndex < this.currentStep.actions.size()) {
					Choice choice = this.currentStep.actions.get(choixIndex);
					// Mémoriser l'étape précédence
					Step prevStep = this.currentStep;
					// Recherche de l'étape correspondante au choix de l'utilisateur
					this.currentStep = this.game.getById(choice.gotostep);
					/* Traitement des options de sortie et de backup */
					if (this.currentStep != null ) {
						switch (this.currentStep.id) {
							case GameBook.EXIT_CODE :
								this.backupStep = prevStep;
								break;
							case GameBook.BAKCUP_CODE :
								this.saveGame();
								break;
						}
					}
					isValidChoice = true;
				} else {
					displayWrongEntry();
				}
			} catch(InputMismatchException e) {
				GameBook.LOGGER.debug("Mauvaise saisie utilisateur : " + e);
				displayWrongEntry();
				this.scanner.next();
			}
		}
	}

	/**
	 * Affiche le contenu de l'étape courante et de ses choix
	 */
	private void displayGame() {
		
		GameBook.LOGGER.info(this.currentStep.description);
		GameBook.LOGGER.info(this.currentStep.question);
		
		for (int i = 0 ; i < this.currentStep.actions.size(); ++i) {
			Choice choice = this.currentStep.actions.get(i);
			GameBook.LOGGER.info("\t" + i + " - " + choice.content);
		}
	}
	
	/**
	 * Mauvaise saisie utilisateur
	 */
	private void displayWrongEntry() {
		
		GameBook.LOGGER.info("Saisie invalide, veuillez saisir un nombre correspondant à votre réponse");
	}

	/**
	 * @return boolean vrai si l'étape courante possède au moins une action.
	 */
	private boolean continueGame() {
		
		return (this.currentStep != null && this.currentStep.actions.size() > 0);
	}

	
	/**
	 * 
	 */
	private void showUsage() {
		
		GameBook.LOGGER.error("Usage : gamebook <path_to_xml_data>");
	}

	/**
	 * 
	 * @return
	 */
	private boolean checkDataPath() {
		
		String errorPrefix = "Impossible de lancer le jeu : ";
		
		if (this.dataPath == null || this.dataPath.isEmpty()) {
			GameBook.LOGGER.error(errorPrefix + "chemin vers un fichier de données XML corrompu.");
			return false;
		}
		
		File file = new File(this.dataPath);
		if (!file.exists() || !file.canRead()) {
			GameBook.LOGGER.error(
					errorPrefix + "le fichier n'existe pas ou l'accès en lecture est interdit pour le chemin '{}'",
					this.dataPath);
			return false;
		}

		GameBook.LOGGER.debug("Le chemin '{}' est confirmé et valide", this.dataPath);
		return true;
	}
	
	/**
	 * Parser le fichier Xml par l'API JAXB
	 * 
	 * @return
	 */
	private Game parseDatas() {
		
		Game game = null;
		try {
			JAXBContext context = JAXBContext.newInstance(Game.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			game = (Game) unmarshaller.unmarshal(new File(this.dataPath));
			GameBook.LOGGER.debug("Analyse et transformation des données en objet réussie.");
		} catch (JAXBException e) {
			GameBook.LOGGER.error("Impossible de démarrer un contexte JAXB.", e);
		}
		return game;	
	}
	
	/**
	 * Parser le backup d'une partie précédente.
	 * 
	 * @return Save
	 */
	private Save parseBackup() {
		
		Save save = null;
		String msg;
		
		try {
			JAXBContext context = JAXBContext.newInstance(Save.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			File backupFile = new File(this.savePath);
			
			if (backupFile.exists() && backupFile.isFile() && backupFile.canRead()) {
				try {
					save = (Save) unmarshaller.unmarshal(backupFile);				
					GameBook.LOGGER.debug("Recuperation des données de la partie sauvegardée.");
				} catch (JAXBException j) {
					msg = "Lecture du fichier de sauvegarde impossible.";
					GameBook.LOGGER.error(msg);
					GameBook.LOGGER.debug(msg, j);
				}
			} else {
				GameBook.LOGGER.error("Le fichier de sauvegarde spécifié n'existe pas ou n'est pas lisible.");
			}
		} catch (JAXBException e) {
			msg = "Impossible de démarrer un contexte JAXB.";
			GameBook.LOGGER.error(msg);
			GameBook.LOGGER.debug(msg, e);
		}
		
		return save;
	}
	
	/**
	 * Sauvegarde de la partie en cours.
	 */
	private void saveGame() {
		
		String msg;
		
		Save save = new Save();
		save.gameId = this.game.id;
		save.lastStep = this.backupStep.id;
		
		try {
			JAXBContext context = JAXBContext.newInstance(Save.class);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			try {
				marshaller.marshal(save, new File(this.savePath));
				GameBook.LOGGER.info("Partie sauvegardée avec succès.");
			} catch (JAXBException e) {
				msg = "Impossible de sauvegarder dans le fichier.";
				GameBook.LOGGER.error(msg);
				GameBook.LOGGER.debug(msg, e);
			}
		} catch (JAXBException e) {
			msg = "Impossible de démarrer un contexte JAXB.";
			GameBook.LOGGER.error(msg);
			GameBook.LOGGER.debug(msg, e);
		}
		
	}
	
	/**
	 * Ajout d'une option pour quitter le jeu à la liste des choix de chaque Etape.
	 * Création d'une étape dédiée à la sortie du jeu pour proposer une sauvegarde.
	 */
	private void wrapGame() {
		
		/* Ajout d''une option pour quitter en cours de jeu si l'étape n'est pas finale. */
		Choice choice = new Choice();
		choice.gotostep = -1;
		choice.content = "Exit game";
		for(Step step : this.game.steps) {
			
			if (step.actions.size() > 0) {
				step.actions.add(choice);
			}
		}
		
		/* Construction des étapes Exit et Backup */
		Step backupStep = new Step();
		backupStep.id = GameBook.BAKCUP_CODE;
		backupStep.description = "N'oubliez pas de recharger votre sauvegarde la prochaine fois";
		backupStep.question = "";
		backupStep.actions = new ArrayList<Choice>(); 
		this.game.steps.add(backupStep);
		
		Choice backupOk = new Choice();
		backupOk.content = "oui";
		backupOk.gotostep = GameBook.BAKCUP_CODE;
		
		Choice backupKo = new Choice();
		backupKo.content = "non";
		backupKo.gotostep = GameBook.NO_ACTION_CODE;
		
		Step quitStep = new Step();
		quitStep.description = "Vous avez choisi de quitter le jeu.";
		quitStep.question = "Souhaitez-vous sauvegarder votre avancée ?";
		quitStep.id = GameBook.EXIT_CODE;
		quitStep.actions = new LinkedList<Choice>();
		quitStep.actions.add(backupKo);
		quitStep.actions.add(backupOk);
		this.game.steps.add(quitStep);
	}	
}
