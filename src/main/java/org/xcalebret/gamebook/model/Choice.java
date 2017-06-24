package org.xcalebret.gamebook.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class Choice {

	@XmlAttribute
	public Integer gotostep;
	
	@XmlValue
	public String content;
}
