package org.xcalebret.gamebook.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class Step {

	@XmlElement
	public Integer id;
	
	@XmlElement
	public String description;
	
	@XmlElement
	public String question;
	
	@XmlElementWrapper
	@XmlElement(name="choice")
	public List<Choice> actions;
}
