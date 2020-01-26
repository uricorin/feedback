package cloud.corin.feedback.core.handlers;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.ibm.ftt.resources.core.IMarkerFactory;
import com.ibm.systemz.cobol.editor.jface.editor.CobolEditor;
import cloud.corin.common.rdz.RDZResource;
import cloud.corin.feedback.core.AuditorFeedback;
import cloud.corin.feedback.core.CompilerFeedback;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class GetCompilationMessagesHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

	IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
	Shell activeShell = window.getShell();

	if (HandlerUtil.getActiveEditor(event) != null && HandlerUtil.getActiveEditor(event) instanceof CobolEditor) {
	    CobolEditor editor = (CobolEditor) HandlerUtil.getActiveEditor(event);
	    IFile file = editor.getFile();

	    IMarkerFactory.eINSTANCE.deleteAllMarkers();
	    try {
		CompilerFeedback.showFeedback(file);
	    } catch (FileNotFoundException e) {
		MessageDialog.openError(activeShell, "Feedback",
			"Unable to open XML file - " + e.getMessage() + "\nCheck the compilation job.");
	    } catch (UnsupportedOperationException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (CoreException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	    try {
		AuditorFeedback.showFeedback(file);
	    } catch (FileNotFoundException e) {
		try {
		    IMarkerFactory.eINSTANCE.createMarker(RDZResource.getPhysical(file),
			    "Check AUDT job step - Couldn't get Auditor feedback from " + e.getMessage(), "AUDITOR01W",
			    IMarker.SEVERITY_WARNING, 0);
		} catch (IllegalArgumentException | UnsupportedOperationException | CoreException e1) {
		    // TODO Auto-generated catch block
		    e1.printStackTrace();
		}
		// MessageDialog.openError(activeShell, "Feedback",
		// "Unable to open XML file - " + e.getMessage() + "\nCheck the
		// compilation job.");
	    } catch (UnsupportedOperationException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (CoreException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	} else {
	    MessageDialog.openError(activeShell, "Feedback", "Not in compatible editor");
	}
	return null;
    }
}
