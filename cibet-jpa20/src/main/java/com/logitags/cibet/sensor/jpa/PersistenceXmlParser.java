package com.logitags.cibet.sensor.jpa;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PersistenceXmlParser extends DefaultHandler {

	private Log log = LogFactory.getLog(PersistenceXmlParser.class);

	private PersistenceXmlHandler handler = new PersistenceXmlHandler();

	public void parse(URL url) throws IOException {
		log.info("parsing " + url);
		InputStream stream = url.openStream();
		try {
			parse(stream);
		} finally {
			stream.close();
		}
	}

	public void parse(InputStream xml) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			SAXParser parser = factory.newSAXParser();
			parser.parse(xml, handler);
		} catch (ParserConfigurationException e) {
			throw new PersistenceException(e.getMessage(), e);
		} catch (SAXException e) {
			throw new PersistenceException(e.getMessage(), e);
		} catch (IOException e) {
			throw new PersistenceException(e.getMessage(), e);
		}
	}

	public boolean containsPersistenceUnitInfo(String persistenceUnitName) {
		return handler.containsPersistenceUnitInfo(persistenceUnitName);
	}

	public PersistenceUnitInfo getPersistenceUnitInfo(String persistenceUnitName) {
		return handler.getPersistenceUnitInfo(persistenceUnitName);
	}

}
