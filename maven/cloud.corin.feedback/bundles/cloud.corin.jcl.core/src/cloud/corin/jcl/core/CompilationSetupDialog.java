package cloud.corin.jcl.core;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class CompilationSetupDialog extends TitleAreaDialog {
    private Text txtJobID;
    private String jobID;
    public CompilationSetupDialog(Shell parentShell) {
        super(parentShell);
    }
    @Override
    public void create() {
        super.create();
    }
    public void create(String title, String message) {
        this.create();
        setTitle(title);
        setMessage(message, IMessageProvider.INFORMATION);
    }
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);
        createJobID(container);
        return area;
    }
    private void createJobID(Composite container) {
        Label lbtJobID = new Label(container, SWT.NONE);
        lbtJobID.setText("Job ID");
        GridData dataJobID = new GridData();
        dataJobID.grabExcessHorizontalSpace = true;
        dataJobID.horizontalAlignment = GridData.FILL;
        txtJobID = new Text(container, SWT.BORDER);
        txtJobID.setLayoutData(dataJobID);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
    // save content of the Text fields because they get disposed
    // as soon as the Dialog closes
    private void saveInput() {
        jobID = txtJobID.getText();
    }
    @Override
    protected void okPressed() {
        saveInput();
        super.okPressed();
    }
    public String getJobID() {
        return jobID;
    }
}