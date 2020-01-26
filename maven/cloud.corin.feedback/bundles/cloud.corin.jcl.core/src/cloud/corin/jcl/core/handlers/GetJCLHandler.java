package cloud.corin.jcl.core.handlers;

import java.io.InputStream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.ibm.systemz.jcl.editor.jface.editor.JclEditor;
import cloud.corin.common.rdz.RDZJob;
import cloud.corin.common.rdz.RDZResource;
import cloud.corin.jcl.core.SpoolExtractor;
import org.eclipse.swt.widgets.Shell;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class GetJCLHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

	IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
	Shell activeShell = window.getShell();
	
	if (HandlerUtil.getActiveEditor(event) != null && HandlerUtil.getActiveEditor(event) instanceof JclEditor) {
	    JclEditor editor = (JclEditor) HandlerUtil.getActiveEditor(event);
	    IFile file = editor.getFile();

	    try {
//		RDZJob rdzjob = RDZJob.locate(RDZResource.getConnectedSystem(file), "JOB15876");
		SpoolExtractor spool = new SpoolExtractor(file, "JOB15876");
		InputStream is = spool.getJCL();

		// BUG? altering contents causes high CPU
		file.setContents(is, false, false, null);
		//		System.out.println(rdzjob.getJCL());

//		file.setc
		
	    } catch (UnsupportedOperationException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (CoreException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
	
	return null;
    }
}
