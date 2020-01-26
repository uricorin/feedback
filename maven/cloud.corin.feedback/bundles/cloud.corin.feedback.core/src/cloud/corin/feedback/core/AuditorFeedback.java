/**
 * 
 */
package cloud.corin.feedback.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.ftt.resources.core.physical.IOSImage;
import com.ibm.ftt.resources.core.physical.IPhysicalResource;
import cloud.corin.common.rdz.RDZResource;

public class AuditorFeedback {

    private static final String PROGRAM_SOURCE_HASH = "0";

    static Iterable<Node> iterable(final NodeList nodeList) {
	return () -> new Iterator<Node>() {
	    private int index = 0;

	    @Override
	    public boolean hasNext() {
		return index < nodeList.getLength();
	    }

	    @Override
	    public Node next() {
		if (!hasNext())
		    throw new NoSuchElementException();
		return nodeList.item(index++);
	    }
	};
    }

    static class CopybookReference {
	private Hashtable<String, String> copybooks = new Hashtable<>();

	public CopybookReference(Node copiesNode) {

	    if (copiesNode != null) {
		NodeList copyNodes = ((Element) copiesNode).getElementsByTagName("copy");

		for (Node node : iterable(copyNodes)) {

		    Element copy = (Element) node;

		    String copyNumber = copy.getAttribute("lines").trim();
		    String memberName = copy.getAttribute("name").trim();

		    copybooks.put(copyNumber, memberName);
		}
	    }
	}

	public String getCopybookByHash(String copyNumber) {
	    return copybooks.get(copyNumber);
	}
    }

    static class FileReference {
	private Hashtable<String, String> files = new Hashtable<>();

	public FileReference(Node programNode) {

	    Element programElement = (Element) programNode;
	    String datasetName = programElement.getElementsByTagName("path").item(0).getTextContent().trim();
	    String membberName = ((Element) programElement).getAttribute("name");

	    files.put(AuditorFeedback.PROGRAM_SOURCE_HASH, datasetName + "(" + membberName + ")");

	    CopybookReference copybooks = new CopybookReference(
		    ((Element) programElement).getElementsByTagName("copys").item(0));

	    NodeList infoNodes = ((Element) programElement).getElementsByTagName("info_message");

	    for (Node node : iterable(infoNodes)) {

		Element file = (Element) node;

		if (file.getAttribute("message").startsWith("COPY:")) {
		    String copyNumber = file.getAttribute("info_lnr").replaceAll("^0*", "").trim();
		    String copyName = file.getAttribute("message").replaceFirst("COPY:", "").trim() + "("
			    + copybooks.getCopybookByHash(copyNumber) + ")";

		    files.put(copyNumber, copyName);
		}
	    }
	}

	public String getFileByHash(String fileNumber) {
	    return files.get(fileNumber);
	}
    }

    static class AuditorMessage {
	Vector<RemoteMessage> messages = new Vector<RemoteMessage>();

	public AuditorMessage(Node programElement, FileReference files) {
	    NodeList metricNodes = ((Element) programElement).getElementsByTagName("metric");

	    for (Node metric : iterable(metricNodes)) {
		NodeList locationNodes = ((Element) metric).getElementsByTagName("location");

		for (Node location : iterable(locationNodes)) {

		    Element locationElement = (Element) location;

		    String messageFile;
		    String messageLine;

		    if (locationElement.getAttribute("copy_name").trim().length() > 1) {
			messageFile = files
				.getFileByHash(locationElement.getAttribute("src_lnr").replaceAll("^0*", ""));
			messageLine = locationElement.getAttribute("copy_lnr").replaceAll("^0*", "").trim();
		    } else {
			messageFile = files.getFileByHash(PROGRAM_SOURCE_HASH);
			messageLine = locationElement.getAttribute("src_lnr").replaceAll("^0*", "").trim();
		    }

		    String messageSeverity;
		    String metricGrade = ((Element) metric).getElementsByTagName("metric_grade").item(0)
			    .getTextContent().trim();
		    switch (metricGrade) {
		    case "OK":
		    case "I":
			messageSeverity = "I";
			break;
		    case "W":
			messageSeverity = "W";
			break;
		    case "E":
		    case "S":
		    default:
			messageSeverity = "E";
		    }

		    String messageNumber = ((Element) metric).getAttribute("id").trim() + messageSeverity;
		    String messageText = ((Element) metric).getElementsByTagName("descr").item(0).getTextContent()
			    .trim();

		    messages.add(new RemoteMessage(messageNumber, messageLine, messageFile, messageText));
		}
	    }
	}

	public Vector<RemoteMessage> getMessages() {
	    return this.messages;
	}
    }

    public static void showFeedback(IFile file) throws UnsupportedOperationException, CoreException, IOException {
	IOSImage system = RDZResource.getConnectedSystem(file);
	IPhysicalResource xmlFile = RDZResource.findPhysical(RDZResource.getConnectedSystem(file),
		getAuditorFileName(file));

	if (xmlFile != null) {
	    for (RemoteMessage auditorMessage : parseXML((RDZResource.getContents(xmlFile)))) {
		auditorMessage.createMarker(system);
	    }

	    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
		    .showView("com.ibm.tpf.connectionmgr.errorlist.zOSErrorListView");

	    xmlFile.delete(true, null);
	} else {
	    throw new FileNotFoundException(getAuditorFileName(file));
	}
    }

    public static String getAuditorFileName(IFile file) throws CoreException {
	return (RDZResource.getConnectedUser(file) + ".AUDITOR." + RDZResource.getElementName(file) + ".XML")
		.toUpperCase();
    }

    public static Vector<RemoteMessage> parseXML(String xml) {

	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	DocumentBuilder builder;
	try {
	    builder = factory.newDocumentBuilder();
	    Document doc;

	    doc = builder.parse(new InputSource(
		    new StringReader("<ROOT>" + xml.replaceAll("<\\\\?.*xml.*\\\\?>", "") + "</ROOT>")));

	    doc.getDocumentElement().normalize();

	    Vector<RemoteMessage> messages = new Vector<RemoteMessage>();
	    for (Node auditorNode : iterable(doc.getElementsByTagName("ccauditor"))) {
		for (Node programNode : iterable(((Element) auditorNode).getElementsByTagName("program"))) {
		    Element programElement = (Element) programNode;
		    FileReference fileRef = new FileReference(programElement);
		    messages.addAll(new AuditorMessage(programElement, fileRef).getMessages());
		}
	    }

	    return messages;

	} catch (ParserConfigurationException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (SAXException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return null;

    }

}
