package cloud.corin.jcl.core;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import com.ibm.ftt.resources.core.physical.IOSImage;
import cloud.corin.common.rdz.RDZJob;
import cloud.corin.common.rdz.RDZResource;

public class SpoolExtractor {

    private String jobID;
    ArrayList<String> lines = new ArrayList<>();

    private IOSImage system;


    public SpoolExtractor(IFile targetFile, String jobID) throws UnsupportedOperationException, CoreException {
	this.system = RDZResource.getConnectedSystem(targetFile);
	this.jobID = jobID;
    }
    public boolean exists() {
	return false;
    }

    
    public void getContents() {
    }
    public InputStream getJCL() {
	RDZJob job = RDZJob.locate(getSystem(), getJobID());

	if (job != null) {
	    handleInput(job.getJCL());
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();

	    for (String line : getLines()) {
		try {
		    baos.write(line.getBytes());
		    baos.write('\n');
		} catch (IOException e) {
		    e.printStackTrace();
		    return null;
		}
	    }

	    byte[] bytes = baos.toByteArray();

	    return new ByteArrayInputStream(bytes);
	}
	return null;
    }
    
    public String getJobID() {
        return jobID;
    }
    public List<String> getLines() {
	List<String> results = lines.stream().filter(line -> !line.contains("EXIT52")).collect(Collectors.toList());
	return results;
    }

    public InputStream getStream() {
	return null;
    }

    public IOSImage getSystem() {
	return system;
    }

    public void handleInput(InputStream is) {
	try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
	    while (br.ready()) {
		lines.add(br.readLine());
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
}
