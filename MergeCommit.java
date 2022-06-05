package gitlet;

public class MergeCommit extends Commit {
    private String mergeParent;

    public MergeCommit(String givenBranch, String mergeParent) throws Exception {
        super("Merged " + givenBranch + " into " + Main.getCurBranch() + ".");
        this.mergeParent = mergeParent;
    }


    public String getMergeParent() {
        return mergeParent;
    }


}
