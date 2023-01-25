import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

/**
 * The chatbot. Supports customized {@link InputStream} and {@link OutputStream}.
 */
public class Meggy implements Runnable {
    /**
     * What to do when reaching different commands. Keys: non-null strings. Values: Non-null function that accepts
     * unparsed string arguments and return chatbot response strings.
     */
    public final Map<String, MeggyException.Function<String, String>> usrCmdToJob;

    /**
     * What to do when the user-typed the command is unknown. Currently: notify user command is unknown.
     */
    public final MeggyException.Function<String, String> unknownCmdBehavior = s -> {
        throw new MeggyException(Resource.errUnknownCmd(Parser.get1stArg(s)));
    };
    /**
     * Customizable input.
     */
    private final Scanner in;
    /**
     * The text-based UI used by the chatbot.
     */
    private final UI ui;
    /**
     * List of tasks. Allow dupes.
     */
    private final ArrayList<UserTask> tasks;

    /**
     * Location to save cross-session data.
     */
    private final Storage storage;

    /**
     * @param in  Non-null. Customizable input.
     * @param out Non-null. Customizable output.
     */
    public Meggy(InputStream in, OutputStream out) {
        if (in == null)
            throw new NullPointerException("InputStream is null");
        if (out == null)
            throw new NullPointerException("OutputStream is null");
        this.in = new Scanner(in);
        this.ui = new UI(out);
        tasks = new ArrayList<>();
        storage = new Storage(new File(Util.dataFilePath));
        usrCmdToJob = Map.of(
                Resource.cmdExit, s -> Resource.farewell,
                Resource.cmdList, s -> listTasks(),
                Resource.cmdMk, s -> markTaskStatus(s, true),
                Resource.cmdUnmk, s -> markTaskStatus(s, false),
                Resource.cmdTodo, s -> addTask(s, Util.todoNew),
                Resource.cmdDdl, s -> addTask(s, Util.ddlNew),
                Resource.cmdEvent, s -> addTask(s, Util.eventNew),
                Resource.cmdDel, this::deleteTask
        );
    }

    /**
     * 'List' command: prints all tasks in the {@code tasks} list.
     *
     * @return Response to 'list' command.
     */
    private String listTasks() {
        final StringBuilder ans = new StringBuilder(Resource.notifList);
        int i = 0;
        for (UserTask task : tasks)
            ans.append(Resource.idxFmt(i++)).append(task).append('\n');
        return ans.toString();
    }

    /**
     * Updates the status of the user task specified by index.
     *
     * @param args      Non-null. Index (start with 1) string of task to be updated.
     * @param newStatus The task's updated status.
     * @return Response to 'mark/unmark' command.
     */
    private String markTaskStatus(String args, boolean newStatus) {
        final int idx;
        try {
            idx = Parser.parseIdx(args, tasks.size());
        } catch (MeggyException e) {
            return e.getMessage() + Util.usageIdxCmd(newStatus ? Resource.cmdMk : Resource.cmdUnmk);
        }
        final UserTask task = tasks.get(idx);
        task.status = newStatus;
        return (newStatus ? Resource.notifMk : Resource.notifUnmk) + Resource.taskIndent + task + '\n';
    }

    /**
     * Adds task to the bottom of {@code tasks} list.
     *
     * @param args    Non-null. Unparsed task description string.
     * @param newTask Non-null. Constructor of task to accept {@code args}.
     * @return Response to "todo/ddl/event" command.
     */
    private String addTask(String args, MeggyException.Function<String, UserTask> newTask) throws MeggyException {
        final UserTask task = newTask.apply(args);
        tasks.add(task);
        storage.save(tasks);
        return Resource.notifAdd + reportChangedTaskAndList(task);
    }

    /**
     * Deletes task from {@code tasks} list. O(n) runtime.
     *
     * @param arg Non-null. Index (start with 1) string of task to be updated.
     * @return Response to 'delete' command.
     */
    private String deleteTask(String arg) {
        final int idx;
        try {
            idx = Parser.parseIdx(arg, tasks.size());
        } catch (MeggyException e) {
            return e.getMessage() + Util.usageIdxCmd(Resource.cmdDel);
        }
        final UserTask task = tasks.remove(idx);
        try {
            storage.save(tasks);
        } catch (MeggyException e) {
            ui.dispLn(e.getMessage());
        }
        return Resource.notifDel + reportChangedTaskAndList(task);
    }

    /**
     * Formatted string about the recently modified task and {@code tasks} list.
     *
     * @param task Non-null. The recently modified task.
     */
    private String reportChangedTaskAndList(UserTask task) {
        return Resource.taskIndent + task + '\n' + Resource.nTaskFmt(tasks.size());
    }

    /**
     * Interacts with user using designated IO.
     */
    @Override
    public void run() {
        // Front page
        ui.disp(Resource.msgHd);
        ui.dispLn(Resource.logo);
        ui.disp(Resource.greetings);
        ui.disp(Resource.msgTl);
        storage.load(tasks);
        while (in.hasNextLine()) { // reads input and responds in each iteration
            //Parse command and args
            final Parser.JobAndArg<String> jobAndArg = Parser.parseJobAndArg(usrCmdToJob, in.nextLine());
            final MeggyException.Function<String, String> job = jobAndArg.job == null ? unknownCmdBehavior : jobAndArg.job;
            //Execute commands and display results
            ui.disp(Resource.msgHd);
            String response;
            try {
                response = job.apply(jobAndArg.args);
            } catch (MeggyException e) {
                response = e.getMessage();
            }
            ui.disp(response);
            ui.disp(Resource.msgTl);
            if (Resource.cmdExit.equals(jobAndArg.cmd)) {
                in.close();
                ui.close();
                return;
            }
        }
        ui.dispLn("WARNING: REACHED END OF INPUT WITHOUT 'BYE' COMMAND");
    }

    public static void main(String[] args) {
        new Meggy(System.in, System.out).run();
    }
}
