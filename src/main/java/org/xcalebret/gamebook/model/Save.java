package org.xcalebret.gamebook.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * <ol>
 * <li>découper la méthode Gamebook.startGame() pour améliorer la lisibilité du code.</li>
 * <li>Prendre en compte la possibilité pour l'utilisateur de quiter le jeu en cours de parite au moment d'enter une saisie clavier.</li>
 * <li>Si l'utilisateur quitte en cours de partie, proposer de sauvegarder sa partie. fair eun objet Java Save comportant les infos a persister en fichier .xml.</li>
 * <li>Prendr een compte la possibilité un second argument en paramètre d'application pour qu'il reprenne une partie sauvegardée.</li>
 * </ol>
 * @author Utilisateur
 *
 */

@XmlRootElement
public class Save {
	
	@XmlAttribute
	public String gameId; // en référence à l'identifiant du fichier xml du jeu originel "superstory"
	
	@XmlAttribute
	public Integer lastStep;
}
