package cloud.corin.feedback.core;

import java.io.InputStream;
import java.util.ArrayList;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import com.ibm.ftt.resources.core.physical.IOSImage;
import com.ibm.ftt.resources.core.physical.IPhysicalResource;
import com.ibm.ftt.resources.zos.zosphysical.DataSetType;
import com.ibm.ftt.resources.zos.zosphysical.IDataSetCharacteristics;
import com.ibm.ftt.resources.zos.zosphysical.IZOSDataSetMember;
import com.ibm.ftt.resources.zos.zosphysical.RecordFormat;
import com.ibm.ftt.resources.zos.zosphysical.SpaceUnits;
import com.ibm.ftt.resources.zos.zosphysical.impl.ZOSPhysicalResourceUtility;
import cloud.corin.common.rdz.RDZResource;

public class CompilationSetup {

    private IOSImage system;
    private String member;
    private String user;

    ArrayList<String> lines = new ArrayList<>();

    public CompilationSetup(IFile targetFile) throws UnsupportedOperationException, CoreException {
	this.system = RDZResource.getConnectedSystem(targetFile);
	this.member = RDZResource.getElementName(targetFile);
	this.user = RDZResource.getConnectedUser(targetFile);
    }

    public boolean exists() {
	try {
	    IPhysicalResource jobFile = getEditableCompilationFile();
	    return jobFile.exists();
	} catch (Exception e) {
	    return false;
	}
    }

    public String getLibraryName() {
	return (getUser() + ".LIB.FEEDBACK.CNTL").toUpperCase();
    }

    public IZOSDataSetMember getEditableCompilationFile() throws UnsupportedOperationException, CoreException {

	return (IZOSDataSetMember) RDZResource.allocatePhysical(getSystem(), getLibraryName() + "(" + getMember() + ")",
		getCharacteristics());
    }

    private IDataSetCharacteristics getCharacteristics() {
	IDataSetCharacteristics characteristics = ZOSPhysicalResourceUtility.createDataSetCharacteristics();
	characteristics.setBlockSize(0);
	characteristics.setRecordLength(80);
	characteristics.setVolumeSerial("");
	characteristics.setRecordFormat(RecordFormat.get("FB"));
	characteristics.setDSOrg("PO");
	characteristics.setPrimaryQuantity(5);
	characteristics.setSecondaryQuantity(5);
	characteristics.setSpaceUnits(SpaceUnits.get("CYLINDERS"));
	characteristics.setDirectoryBlocks(100);
	characteristics.setDSNType(DataSetType.LIBRARY_LITERAL);
	characteristics.setManagementClass("");
	characteristics.setStorageClass("");
	characteristics.setDataClass("");
	return characteristics;
    }

    public void getContents() {
	if (exists()) {
	    System.out.println("content");
	}
    }

    public String getMember() {
	return member.toUpperCase();
    }

    public InputStream getStream() {
	return null;
    }

    public IOSImage getSystem() {
	return system;
    }

    public String getUser() {
	return user;
    }

}
