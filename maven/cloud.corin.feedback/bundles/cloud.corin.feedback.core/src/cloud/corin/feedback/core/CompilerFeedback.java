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

public class CompilerFeedback {

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

    static class FileReference {
	private Hashtable<String, String> files = new Hashtable<>();

	public FileReference(Node packageElement) {
	    NodeList fileNodes = ((Element) packageElement).getElementsByTagName("FILE");

	    for (Node node : iterable(fileNodes)) {

		Element file = (Element) node;
		String fileNumber = ((Element) file.getElementsByTagName("FILENUMBER").item(0)).getTextContent().trim();
		String fileName = ((Element) file.getElementsByTagName("FILENAME").item(0)).getTextContent().trim();

		files.put(fileNumber, fileName);
	    }
	}

	public String getFileByHash(String fileNumber) {
	    return files.get(fileNumber);
	}
    }

    static class CompilerMessage {
	Vector<RemoteMessage> messages = new Vector<RemoteMessage>();
//	IMarkerFactory factory = IMarkerFactory.eINSTANCE;

	public CompilerMessage(Node packageElement, FileReference files) {
	    NodeList messageNodes = ((Element) packageElement).getElementsByTagName("MESSAGE");

	    for (Node node : iterable(messageNodes)) {

		Element message = (Element) node;
		String messageNumber = ((Element) message.getElementsByTagName("MSGNUMBER").item(0)).getTextContent()
			.trim();
		String messageLine = ((Element) message.getElementsByTagName("MSGLINE").item(0)).getTextContent()
			.trim();
		String messageFileRef = ((Element) message.getElementsByTagName("MSGFILE").item(0)).getTextContent()
			.trim();
		String messageText = ((Element) message.getElementsByTagName("MSGTEXT").item(0)).getTextContent()
			.trim();

		messages.add(new RemoteMessage(messageNumber, messageLine, files.getFileByHash(messageFileRef),
			messageText));
	    }
	}

	public Vector<RemoteMessage> getMessages() {
	    return this.messages;
	}
    }

    public static void showFeedback(IFile file) throws UnsupportedOperationException, CoreException, IOException {
	IOSImage system = RDZResource.getConnectedSystem(file);
	IPhysicalResource xmlFile = RDZResource.findPhysical(RDZResource.getConnectedSystem(file),
		getFeedbackFileName(file));

	if (xmlFile != null) {
	    for (RemoteMessage compilerMessage : parseXML((RDZResource.getContents(xmlFile)))) {
		compilerMessage.createMarker(system);
	    }

	    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
		    .showView("com.ibm.tpf.connectionmgr.errorlist.zOSErrorListView");

	    xmlFile.delete(true, null);
	} else {
	    throw new FileNotFoundException(getFeedbackFileName(file));
	}
    }

    public static String getFeedbackFileName(IFile file) throws CoreException {
	return (RDZResource.getConnectedUser(file) + ".FEEDBACK." + RDZResource.getElementName(file) + ".XML")
		.toUpperCase();
    }

    public static Vector<RemoteMessage> parseXML(String xml) {
	// System.out.println("Handling XML");

	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	DocumentBuilder builder;
	try {
	    builder = factory.newDocumentBuilder();
	    Document doc;

	    doc = builder.parse(new InputSource(new StringReader("<ROOT>" + xml + "</ROOT>")));

	    // ?
	    doc.getDocumentElement().normalize();

	    Vector<RemoteMessage> messages = new Vector<RemoteMessage>();
	    for (Node packageNode : iterable(doc.getElementsByTagName("PACKAGE"))) {
		Element packageElement = (Element) packageNode;
		FileReference fileRef = new FileReference(packageElement);
		messages.addAll(new CompilerMessage(packageElement, fileRef).getMessages());
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
