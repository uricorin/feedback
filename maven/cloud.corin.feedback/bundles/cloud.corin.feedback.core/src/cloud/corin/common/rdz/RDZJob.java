package cloud.corin.common.rdz;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

import com.ibm.etools.zos.subsystem.jes.JESSubSystem;
import com.ibm.etools.zos.subsystem.jes.JMConnection;
import com.ibm.etools.zos.subsystem.jes.JMException;
import com.ibm.etools.zos.subsystem.jes.model.JESJob;
import com.ibm.ftt.jes.util.JESJobNotFoundException;
import com.ibm.ftt.jes.util.core.JesJobUtil;
import com.ibm.ftt.resources.core.physical.IOSImage;
import com.ibm.ftt.resources.core.physical.IPhysicalResource;
import com.ibm.ftt.resources.core.physical.util.OperationFailedException;
import cloud.corin.feedback.core.FeedbackConsole;

public class RDZJob {
    private static final int RETRIES = 30;
    private IOSImage system;
    private JesJobUtil jes;
    private JESJob job;
    private JMConnection jm;
    private JESSubSystem subsys;
    private String jobID;
    private IPhysicalResource jobFile = null;
    private long waitInterval = 400;
    private int startLine = 1;

    private RDZJob(IOSImage system) {
	super();
	this.system = system;
	this.jes = new JesJobUtil(system);
    }

    private boolean locate() {
	try {
	    if (jes.ensureJobExistence(getJobID())) {
		this.job = jes.getJESJob(getJobID());
	    } else {
		return false;
	    }
	} catch (OperationFailedException e) {
	    // TODO Auto-generated catch block
	    // e.printStackTrace();
	    return false;
	}
	this.subsys = (JESSubSystem) (this.job.getSubSystem());
	this.jm = this.subsys.getJMConnection();
	return exists();
    }

    public static RDZJob locate(IOSImage system, String jobID) {
	RDZJob rdzjob = new RDZJob(system);
	rdzjob.setJobID(jobID);

	if (rdzjob.locate())
	    return rdzjob;
	else
	    return null;
    }

    public static RDZJob submit(IOSImage system, IPhysicalResource jobFile) throws Exception {
	RDZJob rdzjob = new RDZJob(system);
	rdzjob.setJobFile(jobFile);
	rdzjob.submit();

	for (int i = 0; i < RETRIES; i++) {
	    Thread.sleep(300);
	    if (rdzjob.locate())
		return rdzjob;
	}
	return null;
    }

    private void setJobFile(IPhysicalResource jobFile) {
	this.jobFile = jobFile;
    }

    private RDZJob(IOSImage system, IPhysicalResource jobFile) {
	super();
	this.system = system;
	this.jes = new JesJobUtil(system);
	this.jobFile = jobFile;
	subsys = (JESSubSystem) (job.getSubSystem());
	jm = subsys.getJMConnection();
    }

    public boolean awaitCompletion() throws TimeoutException {
	return awaitCompletion(5000);
    }

    public boolean awaitCompletion(long milliseconds) throws TimeoutException {
	return awaitCompletion(milliseconds, true);
    }

    public boolean awaitCompletion(long milliseconds, boolean monitor) throws TimeoutException {
	long interval = getWaitInterval();
	for (long i = 0; i < milliseconds; i += interval) {
	    if (!exists() || isCompleted()) {
		return true;
	    }
	    try {
		Thread.sleep(interval);
	    } catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}

	throw new TimeoutException();
    }

    public boolean exists() {
	if (!jobID.isEmpty()) {
	    return jes.ensureJobExistence(getJobID());
	} else {
	    return false;
	}
    }

    public InputStream getDDContents(String DDName) {
	if (exists()) {
	    try {
		return getJES().getContents(getJobID(), DDName);
	    } catch (OperationFailedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return null;
	    }
	} else {
	    return null;
	}
    }

    private JMConnection getJM() {
	return jm;
    }

    private String getDSName(int DSID) {
	String dsname;
	for (int i = 0; i < RETRIES; i++) {
	    try {
		Thread.sleep(getWaitInterval());
	    } catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	    dsname = getJob().getDatasets()[DSID].getdsName();
	    if (dsname.length() > 0)
		return dsname;
	}
	return "";

    }

    public InputStream getJCL() {

	String dsname = null;
	for (int i = 0; i < RETRIES; i++) {
	    try {
		dsname = getDSName(0);
		String string = new String(getJM().getJcl(getJobID(), dsname));
		return new ByteArrayInputStream(string.getBytes());
	    } catch (JMException e) {
		// TODO Auto-generated catch block
		// e.printStackTrace();
	    } catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
	return null;
	// new String(jm.getJcl("JOB14390", ds[0].getdsName()))

    }

    public InputStream getJESMSGLG() {

	String dsname = null;
	for (int i = 0; i < RETRIES; i++) {
	    try {
		dsname = getDSName(0);
		InputStream is = getJM().getOutputSDS(getJobID(), dsname, 100, getReadLines());
		return is;
		// return new ByteArrayInputStream(string.getBytes());
	    } catch (JMException e) {
		// TODO Auto-generated catch block
		// e.printStackTrace();
	    } catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
	return null;
	// new String(jm.getJcl("JOB14390", ds[0].getdsName()))

    }

    public JesJobUtil getJES() {
	return jes;
    }

    public JESJob getJob() {
	return job;
    }

    private IPhysicalResource getJobFile() {
	return jobFile;
    }

    public String getJobID() {
	return jobID;
    }

    private int getReadLines() {
	return startLine;
    }

    public IOSImage getSystem() {
	return system;
    }

    public long getWaitInterval() {
	return waitInterval;
    }

    public boolean isCompleted() {
	if (exists()) {
	    try {
		return jes.didJobExecutionComplete(getJobID());
	    } catch (JESJobNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return true;
	    }
	} else {
	    return true;
	}
    }

    public boolean isConnected() {
	return jes.isConnected();
    }

    private void colorizeFeedback(FeedbackConsole fc, String line, int stepnameIndex, int RCIndex,
	    boolean isProcessingSteps) {

	if (isProcessingSteps == true && stepnameIndex > 0 && RCIndex > 0
		&& line.contains("-STEPNAME PROCSTEP    RC") == false) {
	    fc.writeStandard(line.substring(0, stepnameIndex));
	    String coloredSubstring = line.substring(stepnameIndex, RCIndex + 2);

	    switch (line.substring(RCIndex, RCIndex + 2)) {
	    case "00":
		fc.writeOk(coloredSubstring);
		break;
	    case "04":
		fc.writeWarning(coloredSubstring);
		break;
	    default:
		fc.writeError(coloredSubstring);
		break;
	    }

	    fc.writeStandard(line.substring(RCIndex + 2) + "\n");
	} else {
	    fc.writeStandard(line + "\n");
	}
    }

    // This needs to be refactored
    public void monitor(FeedbackConsole fc) {
	System.out.println("Monitor started");
	fc.clear();
	boolean isProcessingSteps = false;
	int stepnameIndex = 0;
	int RCIndex = 0;

	for (int i = 0; i < 300 && !isCompleted(); i++) {
	    int readLines = 0;
	    for (String line : new LineReader(new InputStreamReader(getJESMSGLG()))) {
		if (line.contains("-STEPNAME PROCSTEP    RC")) {
		    isProcessingSteps = true;
		    stepnameIndex = line.indexOf("STEPNAME");
		    RCIndex = line.indexOf("RC");
		}
		// Check for job end or failure
		else if (line.contains("IEF404I") || line.contains("IEF453I")) {
		    isProcessingSteps = false;
		}
		colorizeFeedback(fc, line, stepnameIndex, RCIndex, isProcessingSteps);
		readLines++;
		System.out.println(readLines);
	    }
	    // fc.showConsole();

	    setReadLines(getReadLines() + readLines);
	    try {
		Thread.sleep(1000);
	    } catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
	for (String line : new LineReader(new InputStreamReader(getJESMSGLG()))) {
	    if (line.contains("-STEPNAME PROCSTEP    RC")) {
		isProcessingSteps = true;
		stepnameIndex = line.indexOf("STEPNAME");
		RCIndex = line.indexOf("RC");
	    } else if (line.contains("IEF404I")) {
		isProcessingSteps = false;
	    }

	    colorizeFeedback(fc, line, stepnameIndex, RCIndex, isProcessingSteps);
	}

	System.out.println("Monitor finished");
    }

    public void monitorDD(String DDName, int lrecl, FeedbackConsole fc) {
	InputStream is = getDDContents(DDName);
	byte[] buffer = new byte[lrecl];
	int readBytes = 0;
	try {
	    readBytes = is.read(buffer, getReadLines(), lrecl);
	    if (readBytes != -1) {
		setReadLines(readBytes);
		fc.writeStandard(buffer.toString());
	    }

	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	// return readBytes;
    }

    public String rc() {
	if (exists()) {
	    try {
		return jes.getJobReturnCode(getJobID());
	    } catch (OperationFailedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return null;
	    }
	} else {
	    return null;
	}
    }

    private void setJobID(String jobID) {
	this.jobID = jobID;
    }

    private void setReadLines(int readLines) {
	this.startLine = readLines;
    }

    public void setWaitInterval(long milliseconds) {
	this.waitInterval = milliseconds;
    }

    private void submit() throws Exception {
	String jobID = getJES().submit(getJobFile());
	if (jobID != null)
	    setJobID(jobID);
	else
	    throw new Exception("Job not submitted");
    }
}
