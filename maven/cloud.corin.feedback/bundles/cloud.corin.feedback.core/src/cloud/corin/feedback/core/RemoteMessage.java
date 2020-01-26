package cloud.corin.feedback.core;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import com.ibm.ftt.resources.core.IMarkerFactory;
import com.ibm.ftt.resources.core.physical.IOSImage;
import cloud.corin.common.rdz.RDZResource;

public class RemoteMessage {
    /**
     * 
     */
    // private final CompilerFeedback.CompilerMessage compilerMessage;
    private String messageNumber;
    private String messageLine;
    private String messageFile;
    private String messageText;

    public RemoteMessage(String messageNumber, String messageLine, String messageFile, String messageText) {
	this.messageNumber = messageNumber;
	this.messageLine = messageLine;
	this.messageFile = messageFile;
	this.messageText = messageText;
    }

    public String getID() {
	return this.messageNumber;
    }

    public int getLine() {
	return Integer.parseInt(this.messageLine);
    }

    public String getFile() {
	return this.messageFile;
    }

    public String getText() {
	return this.messageText;
    }

    public int getSeverity() {
	switch (getID().substring(getID().length() - 1)) {
	case "I":
	    return IMarker.SEVERITY_INFO;
	case "W":
	    return IMarker.SEVERITY_WARNING;
	case "E":
	case "S":
	default:
	    return IMarker.SEVERITY_ERROR;
	}
    }

    public void createMarker(IOSImage system)
	    throws IllegalArgumentException, UnsupportedOperationException, CoreException {

	IMarkerFactory.eINSTANCE.createMarker(RDZResource.findPhysical(system, getFile()), getText(), getID(),
		getSeverity(), getLine());
    }
}