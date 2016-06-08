package fr.hippo.synchro;

import java.util.Properties;

/**
 * Created by hippo on 12/02/2016.
 */
public class Config {

    public enum MessageType { EMAIL, TELEGRAM, PUSH_BULLET };

    public static final String FROM_FOLDER = "/torrents/";
    public static final String TEMP_FOLDER = "temp/";
    public static final String DEST_FOLDER = "dest/";
    public static final String USERNAME = "user";
    public static final String PASSWORD = "pwd";
    public static final String SERVER = "myserver.ovh";
    public static final String WANTED_FILE = "wanted.txt";
    public static final String DISK_NAME = "/";
    public static final String DISK_LIMIT = "40000000";
    public static final String SECOND_TO_SLEEP = "20";
    public static final String NB_DL = "5";
    public static final String MESSAGE_TYPE = "EMAIL";
    public static final String MESSAGE_FROM = "fake_address@gmail.com";
    public static final String MESSAGE_DEST = "my_addess@gmail.com";

    private String fromFolder;
    private String tempFolder;
    private String destFolder;
    private String username;
    private String password;
    private String server;
    private String wantedFile;
    private String diskName;
    private String diskLimit;
    private int secondToSleep;
    private int nbDL;
    private MessageType messageType;
    private String messageDest;
    private String messageFrom;

    public void initFromProperties(Properties properties) {
        fromFolder = getProperty(properties, "from_folder", FROM_FOLDER);
        tempFolder = getProperty(properties, "temp_folder", TEMP_FOLDER);
        destFolder = getProperty(properties, "dest_folder", DEST_FOLDER);
        username = getProperty(properties, "username", USERNAME);
        password = getProperty(properties, "password", PASSWORD);
        server = getProperty(properties, "server", SERVER);
        wantedFile = getProperty(properties, "wanted_file", WANTED_FILE);
        diskName = getProperty(properties, "disk_name", DISK_NAME);
        diskLimit = getProperty(properties, "disk_limit", DISK_LIMIT);
        secondToSleep = Integer.parseInt(getProperty(properties, "second_to_sleep", SECOND_TO_SLEEP));
        nbDL = Integer.parseInt(getProperty(properties, "nb_dl", NB_DL));
        messageType = MessageType.valueOf(getProperty(properties, "message_type", MESSAGE_TYPE));
        messageDest = getProperty(properties, "message_dest", MESSAGE_DEST);
        messageFrom = getProperty(properties, "message_from", MESSAGE_FROM);
    }

    private String getProperty(Properties properties, String propName, String def) {

        Object prop = properties.get(propName);
        if (prop == null) {
            return def;
        } else {
            return prop.toString();
        }
    }

    public String getFromFolder() {
        return fromFolder;
    }

    public String getTempFolder() {
        return tempFolder;
    }

    public String getDestFolder() {
        return destFolder;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getServer() {
        return server;
    }

    public String getWantedFile() {
        return wantedFile;
    }

    public String getDiskName() {
        return diskName;
    }

    public String getDiskLimit() {
        return diskLimit;
    }

    public int getSecondToSleep() {
        return secondToSleep;
    }

    public int getNbDL() {
        return nbDL;
    }

    public void setNbDL(int nbDL) {
        this.nbDL = nbDL;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getMessageDest() {
        return messageDest;
    }

    public String getMessageFrom() {
        return messageFrom;
    }

    @Override
    public String toString() {
        return "Config{" +
                "\n\tfromFolder='" + fromFolder + '\'' +
                "\n\ttempFolder='" + tempFolder + '\'' +
                "\n\tdestFolder='" + destFolder + '\'' +
                "\n\tusername='" + username + '\'' +
                "\n\tpassword='" + password + '\'' +
                "\n\tserver='" + server + '\'' +
                "\n\twantedFile='" + wantedFile + '\'' +
                "\n\tdiskName='" + diskName + '\'' +
                "\n\tdiskLimit='" + diskLimit + '\'' +
                "\n\tsecondToSleep=" + secondToSleep +
                "\n\tnbDL=" + nbDL +
                "\n\tmessageType=" + messageType +
                "\n\tmessageDest=" + messageDest +
                "\n\tmessageFrom=" + messageFrom +
                "\n}";
    }
}
