/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2014  Linagora
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version, provided you comply 
 * with the Additional Terms applicable for OBM connector by Linagora 
 * pursuant to Section 7 of the GNU Affero General Public License, 
 * subsections (b), (c), and (e), pursuant to which you must notably (i) retain 
 * the “Message sent thanks to OBM, Free Communication by Linagora” 
 * signature notice appended to any and all outbound messages 
 * (notably e-mail and meeting requests), (ii) retain all hypertext links between 
 * OBM and obm.org, as well as between Linagora and linagora.com, and (iii) refrain 
 * from infringing Linagora intellectual property rights over its trademarks 
 * and commercial brands. Other Additional Terms apply, 
 * see <http://www.linagora.com/licenses/> for more details. 
 *
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details. 
 *
 * You should have received a copy of the GNU Affero General Public License 
 * and its applicable Additional Terms for OBM along with this program. If not, 
 * see <http://www.gnu.org/licenses/> for the GNU Affero General Public License version 3 
 * and <http://www.linagora.com/licenses/> for the Additional Terms applicable to 
 * OBM connectors. 
 * 
 * ***** END LICENSE BLOCK ***** */
package org.obm.push.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.transform.TransformerException;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Test;
import org.obm.push.utils.DOMUtils.XMLVersion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;


public class DOMUtilsTest {
	
	@Test
	public void testEncodingXmlWithAccents() throws TransformerException, UnsupportedEncodingException{
		Document reply = DOMUtils.createDoc(null, "Sync");
		Element root = reply.getDocumentElement();
		String expectedString = "éàâ";
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		root.setTextContent(expectedString);
		DOMUtils.serialize(reply, out);

		assertThat(new String(out.toByteArray(), "UTF-8")).contains(expectedString);
	}
	
	@Test
	public void testCDataSectionEncoding() throws TransformerException, IOException{
		Document reply = DOMUtils.createDoc(null, "Root");
		Element root = reply.getDocumentElement();
		Charset charset = Charsets.UTF_8;

		String expectedString = " \" ' éàâ";
		DOMUtils.createElementAndCDataText(root, 
				"CDataSection",  new ByteArrayInputStream(expectedString.getBytes(charset)), charset);
		

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DOMUtils.serialize(reply, out);
				
		assertThat(new String(out.toByteArray(), charset.name())).contains( 
				"<CDataSection><![CDATA["+ expectedString + "]]></CDataSection>");
	}

	@Test
	public void testSerializeDefaultXmlVersionIsXML10() throws Exception {
		String output = DOMUtils.serialize(createDoc());
		assertThat(output).startsWith("<?xml version=\"1.0\"");
	}

	@Test
	public void testSerializeOutputDefaultXmlVersionIsXML10() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DOMUtils.serialize(createDoc(), out);
		assertThat(streamToString(out)).startsWith("<?xml version=\"1.0\"");
	}

	@Test
	public void testSerializePrettyDefaultXmlVersionIsXML10() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DOMUtils.serialize(createDoc(), out, true);
		assertThat(streamToString(out)).startsWith("<?xml version=\"1.0\"");
	}

	@Test
	public void testSerializeNotPrettyDefaultXmlVersionIsXML10() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DOMUtils.serialize(createDoc(), out, false);
		assertThat(streamToString(out)).startsWith("<?xml version=\"1.0\"");
	}

	@Test
	public void testSerializeXmlVersionXML10() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DOMUtils.serialize(createDoc(), out, XMLVersion.XML_10);
		assertThat(streamToString(out)).startsWith("<?xml version=\"1.0\"");
	}

	@Test
	public void testSerializeXmlVersionXML11() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DOMUtils.serialize(createDoc(), out, XMLVersion.XML_11);
		assertThat(streamToString(out)).startsWith("<?xml version=\"1.1\"");
	}

	@Test
	public void testSerializePrettyXmlVersionXML10() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DOMUtils.serialize(createDoc(), out, true, XMLVersion.XML_10);
		assertThat(streamToString(out)).startsWith("<?xml version=\"1.0\"");
	}

	@Test
	public void testSerializeNotPrettyXmlVersionXML10() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DOMUtils.serialize(createDoc(), out, false, XMLVersion.XML_10);
		assertThat(streamToString(out)).startsWith("<?xml version=\"1.0\"");
	}

	@Test
	public void testSerializePrettyXmlVersionXML11() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DOMUtils.serialize(createDoc(), out, true, XMLVersion.XML_11);
		assertThat(streamToString(out)).startsWith("<?xml version=\"1.1\"");
	}

	@Test
	public void testSerializeNotPrettyXmlVersionXML11() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DOMUtils.serialize(createDoc(), out, false, XMLVersion.XML_11);
		assertThat(streamToString(out)).startsWith("<?xml version=\"1.1\"");
	}

	private Document createDoc() throws FactoryConfigurationError {
		return DOMUtils.createDoc(null, "root");
	}
	
	private String streamToString(ByteArrayOutputStream out) {
		return new String(out.toByteArray(), Charsets.UTF_8);
	}
	
	@Test
	public void getElementIntegerIntNode() {
		int expected = 123;
		Document reply = DOMUtils.createDoc(null, "Sync");
		Element root = reply.getDocumentElement();
		Element intNode = DOMUtils.createElementAndText(root, "myNode", String.valueOf(expected));
		Integer actual = DOMUtils.getElementInteger(intNode);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void getElementIntegerNullIntNode() {
		Document reply = DOMUtils.createDoc(null, "Sync");
		Element root = reply.getDocumentElement();
		Element intNode = DOMUtils.createElement(root, "myNode");
		Integer actual = DOMUtils.getElementInteger(intNode);
		assertThat(actual).isNull();
	}
	
	@Test(expected=NumberFormatException.class)
	public void getElementIntegerNonIntNode() {
		Document reply = DOMUtils.createDoc(null, "Sync");
		Element root = reply.getDocumentElement();
		Element intNode = DOMUtils.createElementAndText(root, "myNode", "toto");
		DOMUtils.getElementInteger(intNode);
	}
	
	@Test
	public void testGetElementsByNameZero() throws Exception {
		Element parent = DOMUtils.parse("<root></root>").getDocumentElement();
		Iterable<Node> found = DOMUtils.getElementsByName(parent, "Seeking");
		assertThat(Iterables.size(found)).isEqualTo(0);
	}

	@Test
	public void testGetElementsByNameOne() throws Exception {
		Element parent = DOMUtils.parse("<root><Seeking>data</Seeking></root>").getDocumentElement();
		Iterable<Node> found = DOMUtils.getElementsByName(parent, "Seeking");
		assertThat(Iterables.size(found)).isEqualTo(1);
	}

	@Test
	public void testGetElementsByNameTwo() throws Exception {
		Element parent = DOMUtils.parse("<root><Seeking>data</Seeking><Seeking>data</Seeking></root>").getDocumentElement();
		Iterable<Node> found = DOMUtils.getElementsByName(parent, "Seeking");
		assertThat(Iterables.size(found)).isEqualTo(2);
	}

	@Test
	public void testGetAttributes() throws Exception {
		Element parent = DOMUtils.parse("<root><element a=\"a1\" b=\"b1\" c=\"c1\"/><element a=\"a2\" b=\"b2\" c=\"c2\"/></root>").getDocumentElement();
		String[][] attributes = DOMUtils.getAttributes(parent, "element", new String[] {"a", "b"});
		
		assertThat(attributes).hasSize(2);
		assertThat(Arrays.asList(attributes[0])).containsExactly("a1", "b1");
		assertThat(Arrays.asList(attributes[1])).containsExactly("a2", "b2");
	}
	
	@Test
	public void testGetAttributesWithMissingValues() throws Exception {
		Element parent = DOMUtils.parse("<root><element a=\"a1\" b=\"b1\" c=\"\"/><element a=\"a2\" b=\"b2\"/></root>").getDocumentElement();
		String[][] attributes = DOMUtils.getAttributes(parent, "element", new String[] {"a", "b", "c"});
		
		assertThat(attributes).hasSize(2);
		assertThat(Arrays.asList(attributes[0])).containsExactly("a1", "b1", ""); // Empty attribute
		assertThat(Arrays.asList(attributes[1])).containsExactly("a2", "b2", ""); // Missing attribute
	}
	
	@Test
	public void testParsedHtmlWithInlineNamespaceCanBeSerialized() throws Exception {
		String html = "<root><style>w\\:* {behavior:url(#default#VML);}</style><el w:st=\"on\">data</el></root>";
		Document htmlDoc = DOMUtils.parseHtmlAsDocument(new InputSource(new StringReader(html)));
		
		assertThat(DOMUtils.serializeHtmlDocument(htmlDoc)).isEqualTo(
			"<HTML><HEAD xmlns=\"http://www.w3.org/1999/xhtml\"/>\n" +
				"<BODY>\n" +
					"<ROOT>\n" +
						"<STYLE>w\\:* {behavior:url(#default#VML);}</STYLE>\n" +
						"<EL w:st=\"on\">data</EL>\n" +
					"</ROOT>\n" +
				"</BODY>\n" +
			"</HTML>\n");
	}
	
	@Test
	public void testCreateDocFromElement() throws SAXException, IOException {
		Document initialDoc = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<FirstNode>" +
					"<SubNode>text</SubNode>" +
				"</FirstNode>");
		Element subNode = DOMUtils.getUniqueElement(initialDoc.getDocumentElement(), "SubNode");
		
		Document subNodeAsDoc = DOMUtils.createDocFromElement(subNode);
		
		XMLAssert.assertXMLEqual(subNodeAsDoc, DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<SubNode>text</SubNode>"));
	}
	
	@Test
	public void testCreateDocFromElementDeeply() throws SAXException, IOException {
		Document initialDoc = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<FirstNode>" +
					"<SubNode>" +
						"<SubSubOne>one</SubSubOne>" +
						"<SubSubTwo>two</SubSubTwo>" +
					"</SubNode>" +
				"</FirstNode>");
		Element subNode = DOMUtils.getUniqueElement(initialDoc.getDocumentElement(), "SubNode");
		
		Document subNodeAsDoc = DOMUtils.createDocFromElement(subNode);
		
		XMLAssert.assertXMLEqual(subNodeAsDoc, DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<SubNode>" +
					"<SubSubOne>one</SubSubOne>" +
					"<SubSubTwo>two</SubSubTwo>" +
				"</SubNode>"));
	}
	
	@Test
	public void testCreateDocFromElementWithNamespace() throws SAXException, IOException {
		Document initialDoc = DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<FirstNode xmlns=\"http://www.w3.org/1999/xhtml\">" +
					"<SubNode>text</SubNode>" +
				"</FirstNode>");
		Element subNode = DOMUtils.getUniqueElement(initialDoc.getDocumentElement(), "SubNode");
		
		Document subNodeAsDoc = DOMUtils.createDocFromElement(subNode);
		
		XMLAssert.assertXMLEqual(subNodeAsDoc, DOMUtils.parse(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<SubNode xmlns=\"http://www.w3.org/1999/xhtml\">text</SubNode>"));
	}
}

