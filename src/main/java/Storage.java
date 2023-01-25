import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

/**
 * Save cross-session data in file.
 */
public class Storage {
    /**
     * From "command" in a data line to the type of {@link UserTask} to be created.
     */
    public final static Map<String, MeggyException.Function<String, UserTask>> dataEntryToTask = Map.of(
            Resource.cmdTodo, Util.todoNew,
            Resource.cmdDdl, Util.ddlNew,
            Resource.cmdEvent, Util.eventNew
    );
    final public File dataFile;

    /**
     * @param dataFile Non-null. The data file to read from and write to. If not exist, it will be created upon first
     *                 write operation.
     */
    public Storage(File dataFile) {
        this.dataFile = dataFile;
    }

    /**
     * Write the content of the entire {@code tasks} list into data file. Creates data file if it did not previously
     * exist.
     *
     * @param tasks Non-null. The task list to take snapshot.
     * @throws MeggyException If file IO throws {@link IOException}.
     */
    public void save(ArrayList<UserTask> tasks) throws MeggyException {
        final FileWriter fw;
        try {
            dataFile.createNewFile();
            fw = new FileWriter(dataFile, false);
        } catch (IOException e) {
            throw new MeggyException(Resource.errFileWrite + Resource.errIO);
        } catch (SecurityException e) {
            throw new MeggyException(Resource.errFileWrite + Resource.errNoAccess);
        }
        try {
            for (UserTask t : tasks)
                fw.write(t.encode() + '\n');
            fw.flush();
        } catch (IOException e) {
            throw new MeggyException(Resource.errFileWrite + Resource.errIO);
        } finally {
            try {
                fw.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Reads content of file into the {@code tasks} list. If the file does not exist, process is skipped. Otherwise, all
     * lines with syntax error are skipped.
     * <p>
     * All file {@link IOException}s are ignored as if the file did not exist.
     *
     * @param tasks Non-null. The task list to load in data.
     */
    public void load(ArrayList<UserTask> tasks) {
        final Scanner fileIn;
        try {
            fileIn = new Scanner(dataFile);
        } catch (FileNotFoundException e) {
            return;
        }
        while (fileIn.hasNextLine()) {
            final Parser.JobAndArg<UserTask> jobAndArg = Parser.parseJobAndArg(dataEntryToTask, fileIn.nextLine());
            final MeggyException.Function<String, UserTask> taskNew = jobAndArg.job;
            if (taskNew != null) { // Command recognized
                try {
                    tasks.add(taskNew.apply(jobAndArg.args));
                } catch (MeggyException ignored) {
                }
            }
        }
        fileIn.close();
    }
}
