package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 */
public class Main {

    static final File vcs = new File(".gitlet");
    static final File staging = new File(".gitlet/staging");
    static final File addition = Utils.join(staging, "addition");
    static final File removal = Utils.join(staging, "removal");
    static final File commits = new File(".gitlet/commits");
    static final File head = Utils.join(commits, "head");
    static final File headCommit = Utils.join(head, "commit");
    static final File headBranch = Utils.join(head, "branch");
    static final File blobs = new File(".gitlet/blobs");
    static final File branches = new File(".gitlet/branches");


    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        if (!vcs.exists() && !args[0].equals("init")) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }


        File[] directories = {vcs, staging, addition, removal, commits, head, blobs, branches};
        if (!vcs.exists() && !args[0].equals("init")) {
            System.out.println("Not in an initialized Gitlet directory.");
        }
        switch (args[0]) {

            case "init":
                if (args.length > 1) {
                    System.out.print("Incorrect operands.");
                    break;
                }


                if (vcs.exists()) {
                    System.out.println("A Gitlet version-control system already exists in the current directory.");
                } else {
                    for (int i = 0; i < directories.length; i++) directories[i].mkdir();
                    Commit initial = new Commit();
                    initial.saveCommit();
                    File masterBranch = Utils.join(branches, "master");
                    Utils.writeContents(masterBranch, initial.getName());
                    Utils.writeContents(headBranch, "master");
                    updateHead(initial);
                }
                break;

            case "add":
                if (args.length > 2 || args.length == 1) {
                    System.out.print("Incorrect operands.");
                    break;
                }

                File wdf = new File(args[1]);
                if (!wdf.exists()) {
                    System.out.println("File does not exist.");
                    break;
                }
                if (Utils.plainFilenamesIn(removal).contains(args[1])) {
                    Utils.join(removal, args[1]).delete();
                    break;
                }
                Commit currentCommit = Commit.getCommit(getHead());
                File filetoBeStaged = Utils.join(addition, args[1]);
                if (currentCommit.getFiles().containsKey(args[1]) && currentCommit.getFiles().get(args[1]).equals((new Blob(wdf)).getName())) {
                    if (filetoBeStaged.exists()) filetoBeStaged.delete();
                } else addToStaging(args[1]);
                break;

            case "log":
                if (args.length > 1) {
                    System.out.print("Incorrect operands.");
                    break;
                }
                Commit cCommit = Commit.getCommit(getHead());
                while (true) {
                    System.out.println("=== ");
                    System.out.println("commit " + cCommit.getName());
                    if (cCommit instanceof MergeCommit) {
                        System.out.println("Merge: " + cCommit.getParent().substring(0, 7) + " " + ((MergeCommit) cCommit).getMergeParent().substring(0, 7));
                    }
                    System.out.print("Date: ");
                    System.out.println(cCommit.getTimestamp());
                    System.out.println(cCommit.getLogMessage());
                    System.out.println();
                    if (!(cCommit.getParent() == null)) {
                        cCommit = Commit.getCommit(cCommit.getParent());
                    } else {
                        break;
                    }
                }
                break;
            case "global-log":
                if (args.length > 1) {
                    System.out.print("Incorrect operands.");
                    break;
                }
                List<String> allCommits = Utils.plainFilenamesIn(commits);
                for (String com : allCommits) {
                    Commit tempCommit = Commit.getCommit(com);
                    System.out.println("=== ");
                    System.out.println("commit " + tempCommit.getName());
                    if (tempCommit instanceof MergeCommit) {
                        System.out.println("Merge: " + tempCommit.getParent().substring(0, 7) + " " + ((MergeCommit) tempCommit).getMergeParent().substring(0, 7));
                    }
                    System.out.print("Date: ");
                    System.out.println(tempCommit.getTimestamp());
                    System.out.println(tempCommit.getLogMessage());
                    System.out.println();
                }
                break;

            case "commit":
                if (args.length > 2 || args.length == 1) {
                    System.out.print("Incorrect operands.");
                    break;
                }
                if (Utils.plainFilenamesIn(addition).isEmpty() && Utils.plainFilenamesIn(removal).isEmpty()) {
                    System.out.println("No changes added to the commit.");
                    break;
                } else if (args[1].isBlank()) {
                    System.out.println("Please enter a commit message.");
                    break;
                }
                Commit committed = new Commit(args[1]);
                updateHead(committed);
                for (String s : Utils.plainFilenamesIn(removal)) {
                    committed.getFiles().remove(s);
                }
                clearDirec(removal);

                for (String s : Utils.plainFilenamesIn(addition)) {
                    committed.getFiles().put(s, Utils.readContentsAsString(Utils.join(addition, s)));
                }
                clearDirec(addition);
                committed.saveCommit();
                break;

            case "rm":
                if (args.length > 2 || args.length == 1) {
                    System.out.print("Incorrect operands.");
                    break;
                }
                List<String> addedFiles = Utils.plainFilenamesIn(addition);
                Commit curCommit = Commit.getCommit(getHead());
                boolean inAdd = false;
                boolean inCommit = false;
                for (String s : addedFiles) {
                    if (s.equals(args[1])) {
                        Utils.join(addition, s).delete();
                        inAdd = true;
                        break;
                    }
                }
                if (curCommit.getFiles().containsKey(args[1])) {
                    inCommit = true;
                    File wdFile = new File(args[1]);
                    Utils.restrictedDelete(wdFile);
                    File removalFile = Utils.join(removal, args[1]);
                    removalFile.createNewFile();
                }
                if (!inAdd && !inCommit) {
                    System.out.println("No reason to remove the file.");
                }
                break;

            case "branch":
                if (args.length > 2 || args.length == 1) {
                    System.out.print("Incorrect operands.");
                    break;
                }
                if (Utils.plainFilenamesIn(branches).contains(args[1])) {
                    System.out.println("A branch with that name already exists.");
                    break;
                }
                File branch = Utils.join(branches, args[1]);
                Utils.writeContents(branch, getHead());
                Commit.getCommit(getHead()).makeSplitPointTrue();

                break;

            case "find":
                if (args.length > 2 || args.length == 1) {
                    System.out.print("Incorrect operands.");
                    break;
                }
                List<String> commitList = Utils.plainFilenamesIn(commits);
                boolean exists = false;
                for (String c : commitList) {
                    Commit findCommit = Commit.getCommit(c);
                    if (findCommit.getLogMessage().equals(args[1])) {
                        System.out.println(findCommit.getName());
                        exists = true;
                    }
                }
                if (!exists) System.out.println("Found no commit with that message.");
                break;
            case "rm-branch":
                if (args.length != 2) {
                    System.out.print("Incorrect operands.");
                    break;
                }
                if (!Utils.plainFilenamesIn(branches).contains(args[1])) {
                    System.out.println("A branch with that name does not exist.");
                    break;
                } else if (getCurBranch().equals(args[1])) {
                    System.out.println("Cannot remove the current branch.");
                    break;
                }
                Utils.join(branches, args[1]).delete();
                break;
            case "status":
                if (args.length > 1) {
                    System.out.print("Incorrect operands.");
                    break;
                }
                System.out.println("=== Branches ===");
                for (String s : Utils.plainFilenamesIn(branches)) {
                    if (s.equals(getCurBranch())) {
                        System.out.print("*");
                    }

                    System.out.println(s);
                }
                System.out.println();
                System.out.println("=== Staged Files ===");
                for (String s : Utils.plainFilenamesIn(addition)) {
                    System.out.println(s);
                }
                System.out.println();
                System.out.println("=== Removed Files ===");
                for (String s : Utils.plainFilenamesIn(removal)) {
                    System.out.println(s);
                }
                System.out.println();
                System.out.println("=== Modifications Not Staged For Commit ===");
                List<String> modified = new ArrayList<>();
                List<String> untracked = new ArrayList<String>();
                Commit cur = Commit.getCommit(getHead());
                for (String s : Utils.plainFilenamesIn(new File("."))) { //every file in WD
                    if (Utils.join(addition, s).exists()) { //being staged for addition

                        if (!new Blob(new File(s)).getName().equals(Blob.getBlob(Utils.readContentsAsString(Utils.join(addition, s))).getName())) { //checks if file that is staged for addition is different from working directory file

                            modified.add(s + " (modified)");
                        }
                    } else if (cur.getFiles().containsKey(s)) { //being tracked in current commit

                        if (!new Blob(new File(s)).getName().equals(Blob.getBlob(cur.getFiles().get(s)).getName())) { // checks if wd file is different from file tracked in commit

                            modified.add(s + " (modified)");
                        }
                    } else {
                        untracked.add(s);
                    }
                }
                for (String s : Utils.plainFilenamesIn(addition)) {
                    if (!modified.contains(s) && !(new File(s).exists())) { //file staged but deleted in wd
                        modified.add(s + " (deleted)");
                    }
                }
                for (String s : cur.getFiles().keySet()) {
                    if (!Utils.join(removal, s).exists() && !(new File(s).exists())) { // filed tracked but not staged for removal nor in wd
                        modified.add(s + " (deleted)");
                    }
                }
                Collections.sort(modified);
                Collections.sort(untracked);
                for (String s : modified) System.out.println(s);
                System.out.println();
                System.out.println("=== Untracked Files ===");
                for (String s : untracked) System.out.println(s);
                System.out.println();
                break;


            case "checkout":
                List<String> untrakedFiles2 = new ArrayList<>();
                List<String> workingDirectoryFiles2 = Utils.plainFilenamesIn(new File("."));
                for (String s : workingDirectoryFiles2) {
                    if (!getHeadCommit().getFiles().containsKey(s)) {
                        untrakedFiles2.add(s);
                    }
                }

                if (args.length > 4 || args.length == 1) {
                    System.out.print("Incorrect operands.");
                    break;
                }

                switch (args[1]) {

                    case "--":
                        if (args.length > 3) {
                            System.out.print("Incorrect operands.");
                            break;
                        }

                        Commit search = Commit.getCommit(getHead());
                        for (String s : untrakedFiles2) {
                            if (search.getFiles().containsKey(s)) {
                                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                                return;
                            }
                        }
                        if (!search.getFiles().containsKey(args[2])) {
                            System.out.print("File does not exit in that commit.");
                        }
                        String searchFile = search.getFiles().get(args[2]);
                        Blob bl = Blob.getBlob(searchFile);
                        File newFile = new File(args[2]);
                        Utils.writeContents(newFile, bl.getContents());
                        break;
                    default:

                        if (args.length == 4) {
                            if (!args[2].equals("--")) {
                                System.out.print("Incorrect operands.");
                                break;
                            }

                            if (args[1].length() != 40) {
                                args[1] = findCommitId(args[1]);
                                if (args[1] == null) {
                                    System.out.println("No commit with that id exists.");
                                    break;
                                }
                            }
                            List<String> commitfiles = Utils.plainFilenamesIn(Main.commits);
                            if (!commitfiles.contains(args[1])) {
                                System.out.println("No commit with that id exists.");
                                return;
                            }
                            Commit searched = Commit.getCommit(args[1]);
                            if (!searched.getFiles().containsKey(args[3])) {
                                System.out.print("File does not exists in that commit.");
                                break;
                            }
                            if (untrakedFiles2.contains(args[3])) {
                                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                                return;
                            }
                            File createdFile = new File(args[3]);
                            Utils.writeContents(createdFile, Blob.getBlob(searched.getFiles().get(args[3])).getContents());
                            return;
                        } else {
                            if (args.length > 3) {
                                System.out.print("Incorrect operands.");
                                break;
                            }

                            if (!Utils.plainFilenamesIn(branches).contains(args[1])) {
                                System.out.println("No such branch exists.");
                                return;
                            }
                            if (getCurBranch().equals(args[1])) {
                                System.out.println("No need to checkout the current branch.");
                            }
                            Commit pulledCommit = Commit.getCommit(Utils.readContentsAsString(Utils.join(branches, args[1])));
                            for (String s : untrakedFiles2) {
                                if (pulledCommit.getFiles().containsKey(s)) {
                                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                                    return;
                                }
                            }

                            for (String s : getHeadCommit().getFiles().keySet()) {
                                if (!pulledCommit.getFiles().containsKey(s)) {
                                    new File(s).delete();
                                }
                            }
                            restoreCommit(pulledCommit);
                            updateHead(pulledCommit, args[1]);


                        }

                }

                break;

            case "merge":
                if (args.length > 2) {
                    System.out.print("Incorrect operands.");
                    break;
                }
                if (!Utils.plainFilenamesIn(addition).isEmpty() || !Utils.plainFilenamesIn(removal).isEmpty()) {
                    System.out.println("You have uncommitted changes.");
                    break;
                }
                if (!Utils.plainFilenamesIn(branches).contains(args[1])) {
                    System.out.println("A branch with that name does not exist.");
                    break;
                }
                if (args[1].equals(getCurBranch())) {
                    System.out.println("Cannot merge a branch with itself.");
                    break;
                }

                Map<String, byte[]> additionsnapShot = new HashMap<>();
                List<String> removalSnapShot = Utils.plainFilenamesIn(removal);
                Map<String, byte[]> wdSnapShot = new HashMap<>();
                for (String s : Utils.plainFilenamesIn(addition)) {
                    additionsnapShot.put(s, Utils.readContents(Utils.join(addition, s)));
                }

                List<String> untrakedFiles = new ArrayList<>();
                List<String> workingDirectoryFiles = Utils.plainFilenamesIn(new File("."));
                for (String s : workingDirectoryFiles) {
                    if (!getHeadCommit().getFiles().containsKey(s)) {
                        untrakedFiles.add(s);
                    }
                }


                Commit branchCommit = Commit.getCommit(Utils.readContentsAsString(Utils.join(branches, args[1])));
                Commit cCom = getHeadCommit();
                Commit ancestor = findAncestor(cCom, branchCommit);

                if (ancestor.equals(branchCommit)) {
                    System.out.println("Given branch is an ancestor of the current branch.");
                    break;
                }
                if (ancestor.equals(cCom)) {
                    for (String s : untrakedFiles) {
                        if (branchCommit.getFiles().containsKey(s)) {
                            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                            return;
                        }
                    }
                    restoreCommit(branchCommit);
                    for (String file : cCom.getFiles().keySet()) {
                        if (!branchCommit.getFiles().containsKey(file)) {
                            new File(file).delete();
                        }
                    }
                    System.out.println("Current branch fast-forwarded.");
                    break;
                }
                for (String File : ancestor.getFiles().keySet()) {
                    /** Any file that exists in split and is unmodified in current branch and modified in given branch*/
                    if (cCom.getFiles().containsKey(File) && cCom.getFiles().get(File).equals(ancestor.getFiles().get(File)) && branchCommit.getFiles().containsKey(File) && !branchCommit.getFiles().get(File).equals(ancestor.getFiles().get(File))) {
                        if (untrakedFiles.contains(File)) {
                            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                            clearDirec(addition);
                            for (String str : additionsnapShot.keySet()) {
                                Utils.writeContents(Utils.join(addition, str), additionsnapShot.get(str));
                            }
                            clearDirec(removal);
                            for (String str : removalSnapShot) {
                                Utils.join(removal, str).createNewFile();
                            }
                            for (String str : wdSnapShot.keySet()) {
                                Utils.writeContents(new File(str), wdSnapShot.get(str));
                            }
                            return;
                        }
                        Utils.writeContents(new File(File), Blob.getBlob(branchCommit.getFiles().get(File)).getContents());
                        addToStaging(File);
                    }
                    /** unmodified in current and does not exist in given*/
                    if (cCom.getFiles().containsKey(File)) {
                        if (cCom.getFiles().get(File).equals(ancestor.getFiles().get(File)) && !branchCommit.getFiles().containsKey(File)) {
                            for (String s : Utils.plainFilenamesIn(addition)) {
                                if (s.equals(File)) {
                                    Utils.join(addition, s).delete();
                                    break;
                                }
                            }

                            File wdFile = new File(File);
                            wdSnapShot.put(File, Utils.readContents(wdFile));
                            Utils.restrictedDelete(wdFile);
                            File removalFile = Utils.join(removal, File);
                            removalFile.createNewFile();

                        }
                    }

                    /** doesn't exist in current and modified in given*/
                    if (!cCom.getFiles().containsKey(File) && branchCommit.getFiles().containsKey(File) && !ancestor.getFiles().get(File).equals(branchCommit.getFiles().get(File))) {
                        if (untrakedFiles.contains(File)) {
                            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                            clearDirec(addition);
                            for (String str : additionsnapShot.keySet()) {
                                Utils.writeContents(Utils.join(addition, str), additionsnapShot.get(str));
                            }
                            clearDirec(removal);
                            for (String str : removalSnapShot) {
                                Utils.join(removal, str).createNewFile();
                            }
                            for (String str : wdSnapShot.keySet()) {
                                Utils.writeContents(new File(str), wdSnapShot.get(str));
                            }
                            return;
                        }
                        System.out.println("Encountered a merge conflict.");
                        String headString = "<<<<<<< HEAD";
                        String eq = "=======";
                        String arrows = ">>>>>>>";
                        String branchFile = Blob.getBlob(branchCommit.getFiles().get(File)).readBlob();
                        Utils.writeContents(new File(File), headString, "\n", eq, "\n", branchFile, "\n", arrows, "\n");
                        addToStaging(File);
                    }
                    /** doesnt exist in given and modified in current */
                    if (!branchCommit.getFiles().containsKey(File) && cCom.getFiles().containsKey(File) && !ancestor.getFiles().get(File).equals(cCom.getFiles().get(File))) {
                        if (untrakedFiles.contains(File)) {
                            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                            clearDirec(addition);
                            for (String str : additionsnapShot.keySet()) {
                                Utils.writeContents(Utils.join(addition, str), additionsnapShot.get(str));
                            }
                            clearDirec(removal);
                            for (String str : removalSnapShot) {
                                Utils.join(removal, str).createNewFile();
                            }
                            for (String str : wdSnapShot.keySet()) {
                                Utils.writeContents(new File(str), wdSnapShot.get(str));
                            }
                            return;
                        }
                        System.out.println("Encountered a merge conflict.");
                        String headString = "<<<<<<< HEAD";
                        String eq = "=======";
                        String arrows = ">>>>>>>";
                        String currentFile = Blob.getBlob(cCom.getFiles().get(File)).readBlob();
                        Utils.writeContents(new File(File), headString, "\n", currentFile, "\n", eq, "\n", arrows, "\n");
                        addToStaging(File);
                    }
                }
                List onlyinbranch = branchCommit.getFiles().keySet().stream().filter((String x) -> !cCom.getFiles().containsKey(x) && !ancestor.getFiles().containsKey(x)).collect(Collectors.toList());
                /** only in branch */
                for (Object file : onlyinbranch) {
                    if (untrakedFiles.contains(file)) {
                        System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                        clearDirec(addition);
                        for (String str : additionsnapShot.keySet()) {
                            Utils.writeContents(Utils.join(addition, str), additionsnapShot.get(str));
                        }
                        clearDirec(removal);
                        for (String str : removalSnapShot) {
                            Utils.join(removal, str).createNewFile();
                        }
                        for (String str : wdSnapShot.keySet()) {
                            Utils.writeContents(new File(str), wdSnapShot.get(str));
                        }
                        return;
                    }
                    Utils.writeContents(new File((String) file), Blob.getBlob(branchCommit.getFiles().get(file)).getContents());
                    addToStaging((String) file);
                }
                List commonFiles = branchCommit.getFiles().keySet().stream().filter((String x) -> cCom.getFiles().containsKey(x)).collect(Collectors.toList());

                for (Object file : commonFiles) {
                    /** not in ancestor and given and current have same file with different contents */
                    if (!ancestor.getFiles().containsKey(file)) {
                        if (!branchCommit.getFiles().get(file).equals(cCom.getFiles().get(file))) {
                            if (untrakedFiles.contains(file)) {
                                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                                clearDirec(addition);
                                for (String str : additionsnapShot.keySet()) {
                                    Utils.writeContents(Utils.join(addition, str), additionsnapShot.get(str));
                                }
                                clearDirec(removal);
                                for (String str : removalSnapShot) {
                                    Utils.join(removal, str).createNewFile();
                                }
                                for (String str : wdSnapShot.keySet()) {
                                    Utils.writeContents(new File(str), wdSnapShot.get(str));
                                }
                                return;
                            }
                            System.out.println("Encountered a merge conflict.");
                            String headString = "<<<<<<< HEAD";
                            String eq = "=======";
                            String arrows = ">>>>>>>";
                            String currentFile = Blob.getBlob(cCom.getFiles().get(file)).readBlob();
                            String branchFile = Blob.getBlob(branchCommit.getFiles().get(file)).readBlob();
                            Utils.writeContents(new File((String) file), headString, "\n", currentFile, "\n", eq, "\n", branchFile, "\n", arrows, "\n");
                            addToStaging((String) file);
                        }
                    }
                    /** ancestor does equal either branches and neither branches equal each other */
                    else if (!ancestor.getFiles().get(file).equals(branchCommit.getFiles().get(file)) && !ancestor.getFiles().get(file).equals(cCom.getFiles().get(file))) {
                        if (!branchCommit.getFiles().get(file).equals(cCom.getFiles().get(file))) {
                            if (untrakedFiles.contains(file)) {
                                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                                clearDirec(addition);
                                for (String str : additionsnapShot.keySet()) {
                                    Utils.writeContents(Utils.join(addition, str), additionsnapShot.get(str));
                                }
                                clearDirec(removal);
                                for (String str : removalSnapShot) {
                                    Utils.join(removal, str).createNewFile();
                                }
                                for (String str : wdSnapShot.keySet()) {
                                    Utils.writeContents(new File(str), wdSnapShot.get(str));
                                }
                                return;
                            }
                            System.out.println("Encountered a merge conflict.");
                            String headString = "<<<<<<< HEAD";
                            String eq = "=======";
                            String arrows = ">>>>>>>";
                            String currentFile = Blob.getBlob(cCom.getFiles().get(file)).readBlob();
                            String branchFile = Blob.getBlob(branchCommit.getFiles().get(file)).readBlob();
                            Utils.writeContents(new File((String) file), headString, "\n", currentFile, "\n", eq, "\n", branchFile, "\n", arrows, "\n");
                            addToStaging((String) file);
                        }
                    }


                }

                MergeCommit mergeCommit = new MergeCommit(args[1], branchCommit.getName());
                branchCommit.makeSplitPointTrue();
                updateHead(mergeCommit);
                for (String s : Utils.plainFilenamesIn(removal)) {
                    mergeCommit.getFiles().remove(s);
                }
                clearDirec(removal);

                for (String s : Utils.plainFilenamesIn(addition)) {
                    mergeCommit.getFiles().put(s, Utils.readContentsAsString(Utils.join(addition, s)));
                }
                clearDirec(addition);
                mergeCommit.saveCommit();
                break;

            case "reset":
                if (args.length > 2) {
                    System.out.print("Incorrect operands.");
                    break;
                }
                if (args[1].length() == 40 && !Utils.join(commits, args[1]).exists()) {
                    System.out.println("No commit with that id exists.");
                    break;
                } else if (args[1].length() < 40) {
                    args[1] = findCommitId(args[1]);
                    if (args[1] == null) {
                        System.out.println("No commit with that id exists.");
                        return;
                    }
                }
                List<String> untrackedFiles = new ArrayList<String>();
                Commit curCom = Commit.getCommit(getHead());
                for (String s : Utils.plainFilenamesIn(".")) {
                    if (!curCom.getFiles().containsKey(s)) {
                        untrackedFiles.add(s);
                    }
                }
                Commit resetCommit = Commit.getCommit(args[1]);
                boolean untrackedFileError = false;
                for (String s : untrackedFiles) {
                    if (resetCommit.getFiles().containsKey(s)) {
                        untrackedFileError = true;
                    }
                }
                if (untrackedFileError) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    break;
                }
                restoreCommit(Commit.getCommit(args[1]));
                for (String s : curCom.getFiles().keySet()) {
                    if (!resetCommit.getFiles().containsKey(s)) {
                        Utils.restrictedDelete(s);
                    }
                }
                updateBranchHead(getCurBranch(), resetCommit);
                updateHead(resetCommit);
                clearDirec(addition);
                clearDirec(removal);
                break;
            default:
                System.out.println("No command with that name exists.");
                break;


        }


    }
    /** ----------- Helper Methods ----------- */

    /**
     * Used to find the Com ID that the abbreviated ID corresponds to. Returns null if does not exist
     */
    private static String findCommitId(String abbr) {
        String targetCommit = null;
        for (String s : Objects.requireNonNull(Utils.plainFilenamesIn(commits))) {
            if (s.indexOf(abbr) == 0) {
                targetCommit = s;
                break;
            }
        }
        return targetCommit;
    }


    private static void addToStaging(String filename) throws IOException {
        File wdFile = new File(filename);
        Blob added = new Blob(wdFile);
        added.saveBlob();
        File staged = Utils.join(addition, filename);
        staged.createNewFile();
        Utils.writeContents(staged, added.getName());

    }

    private static Commit findAncestor(Commit com1, Commit com2) throws Exception {
        HashMap<String, Integer> com1Splits = getSplits(com1, 0);
        HashMap<String, Integer> com2Splits = getSplits(com2, 0);
        Map<String, Integer> ancestors = new HashMap<>();

        for (String s : com1Splits.keySet()) {
            if (com2Splits.containsKey(s)) {
                ancestors.put(s, Math.min(com1Splits.get(s), com2Splits.get(s)));
            }
        }
        int minimum = Collections.min(ancestors.values());
        String answer = null;
        for (String s : ancestors.keySet()) {
            if (ancestors.get(s) == minimum) {
                answer = s;
                break;
            }
        }
        return Commit.getCommit(answer);

    }

    /**
     * Given a commit sha1, restores all of its files to wd (can be used for checkout and reset)
     */
    private static void restoreCommit(Commit commit) throws Exception {
        for (String s : commit.getFiles().keySet()) {
            File createdFile = new File(s);
            Utils.writeContents(createdFile, Blob.getBlob(commit.getFiles().get(s)).getContents());
        }
    }

    /**
     * Empties out a directory
     */
    private static void clearDirec(File dir) {
        for (String s : Utils.plainFilenamesIn(dir)) Utils.join(dir, s).delete();
    }

    private static void updateHead(Commit commit) {
        Utils.writeContents(headCommit, commit.getName());
        Utils.writeContents(Utils.join(branches, getCurBranch()), commit.getName());
    }

    /**
     * Same as update head but also changes the current branch. (Only should be used for checkout)
     */
    private static void updateHead(Commit commit, String branch) throws Exception {
        Utils.writeContents(headCommit, commit.getName());
        if (!Utils.plainFilenamesIn(branches).contains(branch))
            throw new Exception("No branch named " + branch + " exists.");
        Utils.writeContents(headBranch, branch);
    }

    public static String getCurBranch() {
        return Utils.readContentsAsString(headBranch);
    }

    /**
     * Update branch head to given commit
     */
    private static void updateBranchHead(String branch, Commit commit) throws Exception {
        if (!Utils.plainFilenamesIn(branches).contains(branch))
            throw new Exception("No branch named " + branch + " exists.");
        Utils.writeContents(Utils.join(branches, branch), commit.getName());
    }


    public static String getHead() {
        return Utils.readContentsAsString(headCommit);
    }

    private static Commit getHeadCommit() throws Exception {
        return Commit.getCommit(getHead());
    }


    private static HashMap<String, Integer> getSplits(Commit com, int i) throws Exception {
        HashMap<String, Integer> splits = new HashMap<String, Integer>();
        if (com.getLogMessage().equals("initial commit") && com.getSplitPoint()) {
            splits.put(com.getName(), i);
        } else if (com.getLogMessage().equals("initial commit")) {
            return splits;
        } else if (com.getSplitPoint() && com instanceof MergeCommit) {
            splits.put(com.getName(), i);
            splits.putAll(getSplits(Commit.getCommit(com.getParent()), i + 1));

            Commit merg = Commit.getCommit(((MergeCommit) com).getMergeParent());
            splits.putAll(getSplits(merg, i + 1));
        } else if (com instanceof MergeCommit) {
            Commit merg = Commit.getCommit(((MergeCommit) com).getMergeParent());
            splits.putAll(getSplits(Commit.getCommit(com.getParent()), i + 1));
            splits.putAll(getSplits(merg, i + 1));
        } else if (com.getSplitPoint()) {
            splits.put(com.getName(), i);
            splits.putAll(getSplits(Commit.getCommit(com.getParent()), i + 1));
        } else {

            splits.putAll(getSplits(Commit.getCommit(com.getParent()), i + 1));
        }
        return splits;
    }


}