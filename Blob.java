package gitlet;


import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {
    private byte[] contents;
    private String name;


    public Blob(File filename) {


        contents = Utils.readContents(filename);
        name = this.makeSHA();

    }


    public String getName() {
        return name;
    }

    public byte[] getContents() {
        return contents;
    }

    public static Blob getBlob(String name) throws Exception {
        File blob = new File(".gitlet/blobs/" + name);
        if (!blob.exists()) throw new Exception("Blob with that sha1 does not exist");
        return Utils.readObject(blob, Blob.class);
    }

    public void saveBlob() {
        File blob = Utils.join(Main.blobs, name);
        Utils.writeObject(blob, this);
    }

    private String makeSHA() {
        File temp = new File("temp");
        Utils.writeObject(temp, this);
        String sha = Utils.sha1(Utils.readContents(temp), "blob");
        Utils.restrictedDelete(temp);
        return sha;
    }

    /**
     * read file that blob is storing
     */
    public String readBlob() {
        File x = new File("temp");
        Utils.writeContents(x, getContents());
        String answer = Utils.readContentsAsString(x);
        x.delete();
        return answer.trim();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Blob)) return false;
        Blob o = (Blob) obj;
        return getName().equals(o.getName());
    }

}
