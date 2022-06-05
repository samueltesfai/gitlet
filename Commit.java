package gitlet;


import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class Commit implements Serializable {
    private String name;
    private String timestamp;
    private String logMessage;
    private HashMap<String, String> files;
    private String firstParent;
    private boolean splitPoint;

    public Commit() {
        Date time = new Date(0);
        timestamp = convert(time);
        logMessage = "initial commit";
        name = this.makeSHA();
        files = new HashMap<String, String>();
        splitPoint = false;
    }

    private String convert(Date time) {
        String date = time.toString();
        String timezone = date.substring(20, 24);
        date = date.replace(timezone, "");
        return date + " -0800";

    }

    public Commit(String message) throws Exception {
        Date temp = new Date();
        timestamp = convert(temp);
        logMessage = message;
        firstParent = Main.getHead();
        files = getCommit(firstParent).files;

        name = this.makeSHA();


    }

    public String getName() {
        return this.name;
    }

    public String getParent() {
        return this.firstParent;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public String getLogMessage() {
        return this.logMessage;
    }

    public boolean getSplitPoint() {
        return this.splitPoint;
    }

    /**
     * makes splitpoint true and store the commit
     */
    public void makeSplitPointTrue() {
        splitPoint = true;
        saveCommit();
    }

    /**
     * Given a sha1 one, return the corresponding Commit object
     */
    public static Commit getCommit(String name) throws Exception {
        File commit = Utils.join(Main.commits, name);
        if (!commit.exists()) throw new Exception("Commit with that sha1 does not exist");
        return Utils.readObject(commit, Commit.class);
    }

    public HashMap<String, String> getFiles() {
        return files;
    }

    public void saveCommit() {
        File commit = Utils.join(Main.commits, name);
        Utils.writeObject(commit, this);
    }

    private String makeSHA() {
        File temp = new File("temp");
        Utils.writeObject(temp, this);
        String sha = Utils.sha1(Utils.readContents(temp), "commit");
        Utils.restrictedDelete(temp);
        return sha;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Commit)) return false;
        Commit o = (Commit) obj;
        return getName().equals(o.getName());
    }


}


