package org.xcalebret.gamebook.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;  // JAX-B java xml bind...

@XmlRootElement
public class Game {

	@XmlAttribute
	public String title;
	
	@XmlAttribute(name="lang")
	public String language;
	
	@XmlAttribute
	public String id;
	
	@XmlElementWrapper
	@XmlElement(name="step")
	public List<Step> steps;
	
	/**
	 * Recherche  d'une étape par son identifiant
	 * 
	 * @param id l'identifiant recherché.
	 * @return Step l'étape trouvée ou null.
	 */
	public Step getById(final int id) {
		Step result = null;
		for (Step step : this.steps) {
			if (step.id == id) {
				result = step;
				break;
			}
		}
		return result;
	}
	
}
