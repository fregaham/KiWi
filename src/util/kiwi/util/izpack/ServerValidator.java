

package kiwi.util.izpack;


import java.io.File;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.DataValidator;


/**
 * Used to prove if the cacerts file exists in the
 * $JAVA_HOME/lib/security directory. The <code>JAVA_HOME</code>
 * is the path where the virtual machine is installed (e.g.
 * /usr/java/1.6.0/jre).
 * 
 * @author Mihai
 * @version 1.01
 * @since 1.01
 */
public class ServerValidator implements DataValidator {

    /**
     * The error message used by this validator.
     */
    private String errorMsg;

    /**
     * Builds a <code>ServerValidator</code> instance.
     */
    public ServerValidator() {
        // UNIMPLEMENTED
    }

    @Override
    public boolean getDefaultAnswer() {
        return false;
    }

    @Override
    public String getErrorMessageId() {
        return errorMsg;
    }

    @Override
    public String getWarningMessageId() {
        return "";
    }

    @Override
    public Status validateData(AutomatedInstallData arg0) {
        final File cacertsFile = getCacertsFile();
        final boolean caFileExist = cacertsFile.exists();
        if (!caFileExist) {
            errorMsg =
                    "The File : "
                            + cacertsFile
                            + " does not exist. Please ensure that this file exist on the specified path.";
        }

        return caFileExist ? Status.OK : Status.ERROR;
    }

    /**
     * The path for the cacert file. This file is located in
     * $JAVA_HOME/lib/security directory. The
     * <code>JAVA_HOME</code> is the path where the virtual
     * machine is installed (e.g. /usr/java/1.6.0/jre).
     * 
     * @return the path for the cacert file.
     */
    private File getCacertsFile() {
        final StringBuilder path =
                new StringBuilder(System.getProperty("java.home"));
        path.append(File.separator);
        path.append("lib");
        path.append(File.separator);
        path.append("security");
        path.append(File.separator);
        path.append("cacerts");

        return new File(path.toString());
    }

}
