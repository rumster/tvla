/*
 * Created on Sep 11, 2003
 */
package tvla.relevance;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author Eran Yahav eyahav
 * @TODO: clean this !
 */
public class RelevanceEnvironment {

	/** XML Tag corresponding to component */
	protected static final String COMPONENT_TAG = "component";
	/** XML Tag corresponding to implementors */
	protected static final String IMPLEMENTS_TAG = "implements";
	/** XML Tag corresponding to extenders */
	protected static final String EXTENDS_TAG = "extends";
	/** XML Tag corresponding to quantifier order */
	protected static final String QUANT_ORDER_TAG = "quantifiers";
	/** XML Tag corresponding to quantifiers */
	protected static final String QUANT_TAG = "quantifier";
	/** XML Tag for name proprety */
	protected static final String NAME_TAG = "name";

	/** singleton */
	private static RelevanceEnvironment theInstance;

	/** quantifiers in separation strategy */
	private RelevanceQuantifiers relevanceQuantifiers =
		new RelevanceQuantifiers();
	/** type information for separataion strategy */
	private RelevanceTypeInformation relevanceTypeInformation =
		new RelevanceTypeInformation();

	/**
	 * singleton
	 */
	private RelevanceEnvironment() {

	}

	/**
	 * singleton - get the instance
	 * @return the instance
	 */
	public static RelevanceEnvironment getInstance() {
		if (theInstance == null) {
			theInstance = new RelevanceEnvironment();
			theInstance.init();
		}
		return theInstance;
	}

	/**
	 * initialize RelevanceEnvironment
	 */
	protected void init() {
		try {
			parseRelevanceFile("relevance.xml");
		} catch (Exception e) {
			System.err.println("Error parsing relevance information");
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Parses relevance file genearted by frontend and creates
	 * internal representation
	 * @param fileName - name of relevance file to be parsed
	 * @throws ParserConfigurationException - parsing config exception
	 * @throws SAXException - xml parsing exception
	 * @throws IOException - IO failure
	 */
	protected void parseRelevanceFile(String fileName)
		throws ParserConfigurationException, SAXException, IOException {
		Document doc;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setIgnoringComments(true);

		DocumentBuilder db = dbf.newDocumentBuilder();

		OutputStreamWriter errorWriter = new OutputStreamWriter(System.err);
		db.setErrorHandler(
			new MyErrorHandler(new PrintWriter(errorWriter, true)));

		doc = db.parse(new File(fileName));

		NodeList components = doc.getElementsByTagName(COMPONENT_TAG);
		int n = components.getLength();
		for (int i = 0; i < n; i++) {
			Node c = components.item(i);
			String extendsId = "";
			List implementsIds = new ArrayList();

			if (c.hasAttributes()) {
				Node node = c.getAttributes().getNamedItem(NAME_TAG);
				String componentName = node.getNodeValue();

				for (Node child = c.getFirstChild();
					child != null;
					child = child.getNextSibling()) {
					if (child.hasAttributes()) {
						Node currName =
							child.getAttributes().getNamedItem(NAME_TAG);
						String nodeType = child.getNodeName();
						if (nodeType.equals(EXTENDS_TAG)) {
							extendsId = currName.getNodeValue();
						} else if (nodeType.equals(IMPLEMENTS_TAG)) {
							implementsIds.add(currName.getNodeValue());
						}

					}
				}

				relevanceTypeInformation.addComponent(
					componentName,
					extendsId,
					implementsIds);
			}
		}

		List quantifiers = new ArrayList();
		NodeList quantifierOrder = doc.getElementsByTagName(QUANT_ORDER_TAG);
		int qn = quantifierOrder.getLength();
		for (int i = 0; i < qn; i++) {
			Node quantifiersNode = quantifierOrder.item(i);
			for (Node qChild = quantifiersNode.getFirstChild();
				qChild != null;
				qChild = qChild.getNextSibling()) {
				if (qChild.hasAttributes()) {
					Node currName =
						qChild.getAttributes().getNamedItem(NAME_TAG);
					String nodeType = qChild.getNodeName();
					if (nodeType.equals(QUANT_TAG)) {
						quantifiers.add(currName.getNodeValue());
					}

				}
			}
		}
		relevanceQuantifiers.setOrder(quantifiers);
	}

	/**
	 * XML Parser error handler
	 * @author Eran Yahav yahave
	 */
	private static class MyErrorHandler implements ErrorHandler {
		/** Error handler output goes here */
		private PrintWriter out;

		/**
		 * create a new error handler
		 * @param pw - the printer writer
		 */
		MyErrorHandler(PrintWriter pw) {
			this.out = pw;
		}

		/**
		* Returns a string describing parse exception details
		* @param spe - the parse exception
		* @return string describing parse exception
		*/
		private String getParseExceptionInfo(final SAXParseException spe) {
			String systemId = spe.getSystemId();
			if (systemId == null) {
				systemId = "null";
			}
			String info =
				"URI="
					+ systemId
					+ " Line="
					+ spe.getLineNumber()
					+ ": "
					+ spe.getMessage();
			return info;
		}

		// The following methods are standard SAX ErrorHandler methods.
		// See SAX documentation for more info.

		/** warning in XML parsing
		 * @param spe - the parse exception thrown
		 * @throws SAXException - XML parsing exception
		 **/
		public void warning(SAXParseException spe) throws SAXException {
			out.println("Warning: " + getParseExceptionInfo(spe));
		}

		/** error in XML parsing
		 * @param spe - the parse exception thrown
		 * @throws SAXException - XML parsing exception
		 * */
		public void error(SAXParseException spe) throws SAXException {
			String message = "Error: " + getParseExceptionInfo(spe);
			throw new SAXException(message);
		}

		/** fatal error in XML parsing
		 * @param spe - the parse exception thrown
		 * @throws SAXException - XML parsing exception
		 * */
		public void fatalError(SAXParseException spe) throws SAXException {
			String message = "Fatal Error: " + getParseExceptionInfo(spe);
			throw new SAXException(message);
		}
	}

	/**
	 * get the relevance quantifier information extracted from file
	 * @return relevant quantifiers
	 */
	public RelevanceQuantifiers getRelevanceQuantifiers() {
		return relevanceQuantifiers;
	}

	/**
	 * get the relevance type information extracted from file
	 * @return relevance-type-information
	 */
	public RelevanceTypeInformation getRelevanceTypeInformation() {
		return relevanceTypeInformation;
	}

}