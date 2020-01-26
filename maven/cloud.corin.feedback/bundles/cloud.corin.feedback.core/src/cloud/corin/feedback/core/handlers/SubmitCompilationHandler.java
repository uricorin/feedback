package cloud.corin.feedback.core.handlers;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.ibm.ftt.resources.core.IMarkerFactory;
import com.ibm.ftt.resources.zos.zosphysical.impl.ZOSDataSetMember;
import com.ibm.systemz.cobol.editor.jface.editor.CobolEditor;
import cloud.corin.common.rdz.RDZJob;
import cloud.corin.common.rdz.RDZResource;
import cloud.corin.feedback.core.AuditorFeedback;
import cloud.corin.feedback.core.CompilerFeedback;
import cloud.corin.feedback.core.FeedbackConsole;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SubmitCompilationHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

	IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
	Shell activeShell = window.getShell();

	if (HandlerUtil.getActiveEditor(event) != null && HandlerUtil.getActiveEditor(event) instanceof CobolEditor) {
	    CobolEditor editor = (CobolEditor) HandlerUtil.getActiveEditor(event);
	    IFile file = editor.getFile();
	    String memberName = RDZResource.getElementName(file);
	    FeedbackConsole fc = FeedbackConsole.getInstance();

	    if (memberName.length() > 8) {
		System.out.println("Only members are currently supported");
		fc.writeError("Only members are currently supported");
	    } else {
		try {
		    promptSave(activeShell, editor);

		    String jobLocation = (RDZResource.getConnectedUser(file) + ".LIB.FEEDBACK.CNTL(" + memberName + ")")
			    .toUpperCase();
		    ZOSDataSetMember jobFile = (ZOSDataSetMember) RDZResource
			    .findPhysical(RDZResource.getConnectedSystem(file), jobLocation);

		    if (jobFile == null) {
			MessageDialog.openError(activeShell, "Feedback", "Unable to find compilation job - "
				+ jobLocation + "\nYou can create it using \"Edit compilation job\".");
		    } else {
			try {
			    RDZJob rdzjob = RDZJob.submit(RDZResource.getConnectedSystem(file), jobFile);

			    if (rdzjob != null) {
				fc.showConsole();
				IMarkerFactory.eINSTANCE.deleteAllMarkers();

				Job task = new Job("Monitor submitted job") {
				    @Override
				    protected IStatus run(IProgressMonitor monitor) {
					rdzjob.monitor(fc);

					Display.getDefault().syncExec(new Runnable() {
					    public void run() {
						if (rdzjob.isCompleted()) {
						    try {
							CompilerFeedback.showFeedback(file);
						    } catch (FileNotFoundException e) {
							MessageDialog.openError(activeShell, "Feedback",
								"Unable to open XML file - " + e.getMessage()
									+ "\nYou can Check the compilation job using \"Edit compilation job\".");
						    } catch (UnsupportedOperationException e) {
							// TODO Auto-generated
							// catch
							// block
							e.printStackTrace();
						    } catch (CoreException e) {
							// TODO Auto-generated
							// catch
							// block
							e.printStackTrace();
						    } catch (IOException e) {
							// TODO Auto-generated
							// catch
							// block
							e.printStackTrace();
						    }
						    try {
							AuditorFeedback.showFeedback(file);
						    } catch (FileNotFoundException e) {
							try {
							    IMarkerFactory.eINSTANCE.createMarker(
								    RDZResource.getPhysical(file),
								    "Check AUDT job step - Couldn't get Auditor feedback from "
									    + e.getMessage(),
								    "AUDITOR01W", IMarker.SEVERITY_WARNING, 0);
							} catch (IllegalArgumentException
								| UnsupportedOperationException | CoreException e1) {
							    // TODO
							    // Auto-generated
							    // catch block
							    e1.printStackTrace();
							}
							// MessageDialog.openError(activeShell,
							// "Feedback",
							// "Unable to open XML
							// file - " +
							// e.getMessage()
							// + "\nYou can Check
							// the compilation job
							// using \"Edit
							// compilation job\".");
						    } catch (UnsupportedOperationException e) {
							// TODO Auto-generated
							// catch
							// block
							e.printStackTrace();
						    } catch (CoreException e) {
							// TODO Auto-generated
							// catch
							// block
							e.printStackTrace();
						    } catch (IOException e) {
							// TODO Auto-generated
							// catch
							// block
							e.printStackTrace();
						    }
						} else {
						    MessageDialog.openError(activeShell, "Feedback",
							    "Compilation monitor timed out.\n You can get the feedback manually using \"Show feedback\"");
						}
					    }
					});
					return Status.OK_STATUS;
				    }
				};

				task.setUser(true);
				task.schedule();
				// Thread thread = new Thread(task);
				// thread.start();
			    }
			} catch (Exception e1) {
			    MessageDialog.openError(activeShell, "Feedback", "Submission failed."
				    + "\nYou can Check the compilation job using \"Edit compilation job\".");
			}
		    }

		} catch (UnsupportedOperationException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (CoreException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (OperationCanceledException e) {
		} catch (RuntimeException e) {
		    MessageDialog.openError(activeShell, "Feedback", e.getMessage());
		}
	    }
	} else

	{
	    MessageDialog.openError(activeShell, "Feedback", "Not in compatible editor");
	}
	return null;
    }

    public void promptSave(Shell activeShell, CobolEditor editor) throws RuntimeException, OperationCanceledException {
	IFile file = editor.getFile();
	try {
	    if (RDZResource.isDirty(file)) {
		MessageDialog dialog = new MessageDialog(activeShell, "Feedback", null,
			file.getName() + " has unsaved changes", MessageDialog.WARNING,
			new String[] { "Save", "Continue", "Cancel" }, 0);
		int result = dialog.open();

		switch (result) {
		case 0:
		    editor.doSave(null);
		    try {
			if (RDZResource.isDirty(file)) {
			    throw new RuntimeException("Failed to save");
			}
		    } catch (CoreException e) {
			e.printStackTrace();
		    }
		    break;
		case 1:
		    break;
		default:
		    throw new OperationCanceledException();
		}
	    }
	} catch (UnsupportedOperationException | CoreException e2) {
	    e2.printStackTrace();
	}
    }
}
