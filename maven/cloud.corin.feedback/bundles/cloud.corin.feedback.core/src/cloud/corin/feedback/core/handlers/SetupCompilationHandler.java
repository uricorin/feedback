package cloud.corin.feedback.core.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.ibm.systemz.cobol.editor.jface.editor.CobolEditor;
import cloud.corin.common.rdz.RDZResource;
import cloud.corin.feedback.core.CompilationSetup;
import com.ibm.ftt.ui.resources.core.editor.EditorOpener;
import org.eclipse.swt.widgets.Shell;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SetupCompilationHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

	IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
	Shell activeShell = window.getShell();

	if (HandlerUtil.getActiveEditor(event) != null && HandlerUtil.getActiveEditor(event) instanceof CobolEditor) {
	    CobolEditor editor = (CobolEditor) HandlerUtil.getActiveEditor(event);
	    IFile file = editor.getFile();

	    String memberName = RDZResource.getElementName(file);

	    if (memberName.length() > 8) {
		System.out.println("Only members are currently supported");
	    } else {
		try {
		    CompilationSetup compilation = new CompilationSetup(file);
		    EditorOpener.getInstance().open(compilation.getEditableCompilationFile());

		} catch (UnsupportedOperationException | CoreException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (Exception e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	}
	
	return null;
    }
}
