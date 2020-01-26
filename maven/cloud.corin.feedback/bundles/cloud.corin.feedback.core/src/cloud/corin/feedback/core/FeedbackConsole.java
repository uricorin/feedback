package cloud.corin.feedback.core;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class FeedbackConsole {

    private static FeedbackConsole instance = null;
    private String consoleName = "Compilation feedback";
    private IConsoleManager conMan;
    private IWorkbenchPage page;
    private MessageConsole console;
    private MessageConsoleStream stdout;
    private MessageConsoleStream stdok;
    private MessageConsoleStream stdwarn;
    private MessageConsoleStream stderr;

    public static FeedbackConsole getInstance() {
	if (instance == null) {
	    instance = new FeedbackConsole();
	}
	return instance;
    }

    private FeedbackConsole() {

	conMan = ConsolePlugin.getDefault().getConsoleManager();
	console = new MessageConsole(consoleName, null);
	page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

	add();

    }

    // public static Color getColor() {
    // return out.getColor();
    // }
    //
    // public static void setColor(Color color) {
    // out.setColor(color);
    // }
    // public static void resetColor() {
    // setColor(null);
    // }

    private void add() {
	if (!exists()) {
	    conMan.addConsoles(new IConsole[] { console });
	    stdout = console.newMessageStream();

	    stdok = console.newMessageStream();
	    setColor(stdok, SWT.COLOR_BLUE);

	    stdwarn = console.newMessageStream();
	    setColor(stdwarn, SWT.COLOR_DARK_YELLOW);

	    stderr = console.newMessageStream();
	    setColor(stderr, SWT.COLOR_RED);
	}
    }

    // public void remove() {
    // if (exists())
    // conMan.removeConsoles(new IConsole[] { console });
    // }

    public boolean exists() {
	IConsole[] existing = conMan.getConsoles();

	for (int i = 0; i < existing.length; i++)
	    if (consoleName.equals(existing[i].getName()))
		return true;

	return false;
    }

    public void clear() {
	console.clearConsole();
    }

    private void setColor(MessageConsoleStream stream, int swtColor) {
	stream.setColor(Display.getCurrent().getSystemColor(swtColor));
    }

    private void write(MessageConsoleStream stream, String message) {
	if (!exists()) {
	    add();
	}

	stream.print(message);
	try {
	    stream.flush();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public void writeStandard(String message) {
	write(stdout, message);
    }

    public void writeOk(String message) {
	write(stdok, message);
    }

    public void writeWarning(String message) {
	write(stdwarn, message);
    }

    public void writeError(String message) {
	write(stderr, message);
    }

    public void showConsole() {
	if (exists()) {
	    
	    String id = IConsoleConstants.ID_CONSOLE_VIEW;
	    IConsoleView view;
	    try {
		view = (IConsoleView) page.showView(id);
		view.display(console);
	    } catch (PartInitException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    }
}
