package cloud.corin.common.rdz;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import com.ibm.ftt.projects.zos.zoslogical.impl.LZOSDataSet;
import com.ibm.ftt.projects.zos.zoslogical.impl.LZOSDataSetMember;
import com.ibm.ftt.resources.core.ResourcesCorePlugin;
import com.ibm.ftt.resources.core.factory.IPhysicalResourceFactory;
import com.ibm.ftt.resources.core.factory.IPhysicalResourceFinder;
import com.ibm.ftt.resources.core.physical.IOSImage;
import com.ibm.ftt.resources.core.physical.IPhysicalFile;
import com.ibm.ftt.resources.core.physical.IPhysicalResource;
import com.ibm.ftt.resources.core.physical.util.OperationFailedException;
import com.ibm.ftt.resources.zos.util.MVSSystemEditableRemoteFile;
import com.ibm.ftt.resources.zos.zosfactory.IZOSResourceIdentifier;
import com.ibm.ftt.resources.zos.zosfactory.impl.ZOSResourceIdentifierUtility;
import com.ibm.ftt.resources.zos.zosphysical.IDataSetCharacteristics;
import com.ibm.ftt.resources.zos.zosphysical.IZOSDataSet;
import com.ibm.ftt.resources.zos.zosphysical.IZOSDataSetMember;
import com.ibm.ftt.resources.zos.zosphysical.IZOSPartitionedDataSet;
import com.ibm.ftt.resources.zos.zosphysical.impl.ZOSCatalog;
import com.ibm.ftt.resources.zos.zosphysical.impl.ZOSDataSet;
import com.ibm.ftt.resources.zos.zosphysical.impl.ZOSDataSetMember;
import com.ibm.ftt.resources.zos.zosphysical.impl.ZOSPartitionedDataSet;
import com.ibm.ftt.resources.zos.zosphysical.impl.ZOSSequentialDataSet;

public class RDZResource {

    public enum QualifiedNames {
	EDIT_OBJECT("", "EDIT_OBJECT"), MVS_OBJECT("", "MVS_OBJECT"), REMOTE_FILE("", "remote_file_object_key");

	private String qualifier, localName;

	private QualifiedNames(String qualifier, String localName) {
	    this.qualifier = qualifier;
	    this.localName = localName;
	}

	public QualifiedName getQN() {
	    return new QualifiedName(this.qualifier, this.localName);
	}
    }

    public static IZOSDataSet allocateDataset(IOSImage system, String datasetName,
	    IDataSetCharacteristics characteristics) throws UnsupportedOperationException, CoreException {
	IZOSDataSet result = (IZOSDataSet) RDZResource.findPhysical(system, datasetName);

	if (result != null && result.exists())
	    return result;
	else {
	    ZOSCatalog catalog = (ZOSCatalog) system.getRoot();
	    IPhysicalResourceFactory physicalFactory = ResourcesCorePlugin.getPhysicalResourceFactory("zos");

	    ZOSDataSet res;
	    switch (characteristics.getDSOrg()) {
	    case "PS":
		res = (ZOSDataSet) physicalFactory.getPhysicalResource(catalog, ZOSSequentialDataSet.class,
			datasetName);
		break;
	    case "PO":
		res = (ZOSDataSet) physicalFactory.getPhysicalResource(catalog, ZOSPartitionedDataSet.class,
			datasetName);
		break;
	    default:
		throw new UnsupportedOperationException("Unknown DSORG: " + characteristics.getDSOrg());
	    }

	    res.setCharacteristics(characteristics);
	    res.allocate(null);

	    return (IZOSDataSet) RDZResource.findPhysical(system, datasetName);
	}
    }

    public static IZOSDataSetMember allocateMember(IOSImage system, IZOSPartitionedDataSet pds, String memberName)
	    throws UnsupportedOperationException, CoreException {
	IPhysicalResourceFactory physicalFactory = ResourcesCorePlugin.getPhysicalResourceFactory("zos");

	IZOSDataSetMember member = (IZOSDataSetMember) pds.findMember(memberName);

	if (member == null || !member.exists()) {
	    member = (IZOSDataSetMember) physicalFactory.getPhysicalResource(pds, IZOSDataSetMember.class, memberName);
	    member.create(new ByteArrayInputStream("".getBytes()), true, null);
	}
	return member;
    }

    public static IPhysicalResource allocatePhysical(IOSImage system, String name,
	    IDataSetCharacteristics characteristics) throws UnsupportedOperationException, CoreException {

	// Split by words - 1 for Sequential datasets, 2 for members
	String[] nameParts = name.split("[\\(||//)]");
	String datasetName = nameParts[0];
	String memberName = nameParts.length > 1 ? nameParts[1] : null;

	if (memberName != null && characteristics.getDSOrg() == "PS") {
	    throw new UnsupportedOperationException("Trying to allocate member in Sequential dataset");
	} else {
	    IZOSDataSet dataset = allocateDataset(system, datasetName, characteristics);
	    if (memberName != null) {
		return allocateMember(system, (IZOSPartitionedDataSet) dataset, memberName);
	    } else {
		return dataset;
	    }
	}
    }

    public static IPhysicalResource findPhysical(IOSImage system, String name)
	    throws UnsupportedOperationException, CoreException {

	if (!system.isConnected()) {
	    return null;
	} else {
	    IPhysicalResourceFinder finder = ResourcesCorePlugin.getPhysicalResourceFinder("zos");
	    IZOSResourceIdentifier identifier = ZOSResourceIdentifierUtility.createZOSResourceIdentifier();

	    // Split by words - 1 for Sequential datasets, 2 for members
	    String[] nameParts = name.split("[\\(||//)]");
	    identifier.setDataSetName(nameParts[0]);

	    // Set member name if name contains multiple parts
	    identifier.setMemberName((nameParts.length > 1) ? nameParts[1] : null);

	    // Set hostname
	    identifier.setSystem(system.getName());

	    return finder.findPhysicalResource(identifier);
	}
    }

    /**
     * <p>
     * Get the remote z/OS system hosting the file
     * </p>
     * 
     * @param file
     *            File resource
     * @return Remote z/OS system object
     * @since 1.0.0
     */
    public static IOSImage getConnectedSystem(IFile file) throws UnsupportedOperationException, CoreException {
	return getPhysical(file).getSystem();
    }

    /**
     * <p>
     * Get the user used to connect to the remote z/OS system hosting the file
     * </p>
     * 
     * @param file
     *            File resource
     * @return User name
     * @since 1.0.0
     */
    public static String getConnectedUser(IFile file) throws UnsupportedOperationException, CoreException {
	return getConnectedSystem(file).getUserId();
    }

    /**
     * <p>
     * Get the contents of a remote RDZ resource <br>
     * Non-English characters not supported
     * </p>
     * 
     * @param res
     *            Remote RDZ resource
     * @return Contents as string
     * @since 1.0.0
     */
    public static String getContents(IPhysicalResource res) throws OperationFailedException, IOException {
	if (res instanceof IPhysicalFile) {
	    try (InputStream is = ((IPhysicalFile) res).getContents()) {
		return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
	    }
	} else {
	    return null;
	}
    }

    /**
     * <p>
     * Get minimal name of a file resource
     * </p>
     * 
     * @param file
     *            File resource
     * @return Full name for Sequential Dataset, Member name if Partitioned
     * @since 1.0.0
     */
    public static String getElementName(IFile file) {
	String name = file.getName();

	if (name.length() > file.getFileExtension().length()) {
	    return name.substring(0, name.length() - file.getFileExtension().length() - 1);
	} else {
	    return null;
	}
    }

    /**
     * <p>
     * Get a remote RDZ resource from a file resource <br>
     * Only Datasets and Members currently supported
     * </p>
     * 
     * @param file
     *            File resource
     * @return Remote RDZ resource
     * @since 1.0.0
     */
    public static IPhysicalResource getPhysical(IFile file) throws UnsupportedOperationException, CoreException {
	Object remote = file.getSessionProperty(QualifiedNames.EDIT_OBJECT.getQN());
	// remote = file.getSessionProperty(QualifiedNames.REMOTE_FILE.getQN());

	IPhysicalResourceFinder finder = ResourcesCorePlugin.getPhysicalResourceFinder("zos");
	IZOSResourceIdentifier identifier = ZOSResourceIdentifierUtility.createZOSResourceIdentifier();
	IOSImage system;

	if (remote instanceof ZOSDataSetMember) {
	    ZOSDataSet dataset = (ZOSDataSet) ((ZOSDataSetMember) remote).getParent();
	    identifier.setDataSetName(dataset.getName());
	    identifier.setMemberName(((ZOSDataSetMember) remote).getName());
	    system = dataset.getSystem();
	} else if (remote instanceof LZOSDataSetMember) {
	    ZOSDataSet dataset = (ZOSDataSet) ((LZOSDataSetMember) remote).getZOSResource().getParent();
	    identifier.setDataSetName(dataset.getName());
	    identifier.setMemberName(((LZOSDataSetMember) remote).getName());
	    system = dataset.getSystem();
	} else if (remote instanceof LZOSDataSet) {
	    ZOSDataSet dataset = (ZOSDataSet) ((LZOSDataSetMember) remote).getZOSResource();
	    identifier.setDataSetName(dataset.getName());
	    identifier.setMemberName(null);
	    system = dataset.getSystem();
	} else if (remote instanceof ZOSDataSet) {
	    ZOSDataSet dataset = (ZOSDataSet) remote;
	    identifier.setDataSetName(dataset.getName());
	    identifier.setMemberName(null);
	    system = dataset.getSystem();
	} else {
	    throw new UnsupportedOperationException("Type " + remote.getClass() + " is unsupported");
	}

	// MVSSystemEditableRemoteFile o = (MVSSystemEditableRemoteFile) remote;
	

	// Set hostname
	identifier.setSystem(system.getName());

	return finder.findPhysicalResource(identifier);

    }

    /**
     * <p>
     * Check if the resource has unsaved changes in Editor <br>
     * Only Datasets and Members currently supported
     * </p>
     * 
     * @param file
     *            File resource
     * @return True if unsaved changes
     * @since 1.0.0
     */
    public static boolean isDirty(IFile file) throws UnsupportedOperationException, CoreException {
	Object remote;
	remote = file.getSessionProperty(QualifiedNames.REMOTE_FILE.getQN());
	if (remote instanceof MVSSystemEditableRemoteFile) {
	    MVSSystemEditableRemoteFile o = (MVSSystemEditableRemoteFile) remote;
	    return o.isDirty();
	} else {
	    throw new UnsupportedOperationException("Type " + remote.getClass() + " is unsupported");
	}
    }

    private RDZResource() {
    }
}
