package meggy;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import meggy.exception.Function;
import meggy.exception.MeggyException;
import meggy.exception.MeggyNoArgException;
import meggy.task.UserTask;

/** The chatbot. After initialization, interact by calling {@code getResponse} method. */
public class Meggy {
    /**
     * What to do when reaching different commands. Keys: non-null strings. Values: Non-null function that accepts
     * unparsed string arguments and return chatbot response strings.
     */
    public final Map<String, Function<String, String>> usrCmdToJob;

    /** What to do when the user-typed the command is unknown. Currently: notify user command is unknown. */
    public final Function<String, String> unknownCmdBehavior = s -> {
        throw new MeggyException(Resource.errUnknownCmd(Parser.get1stArg(s)));
    };
    /** List of tasks. Allows dupes. */
    private final TaskList tasks;
    /** Location to save cross-session data. */
    private final Storage storage;

    /**
     * Creates a chatbot agent instance.
     */
    public Meggy() {
        tasks = new TaskList();
        usrCmdToJob = Map.of(
                Resource.CMD_EXIT, s -> Resource.FAREWELL,
                Resource.CMD_LIST, s -> Resource.NOTIF_LIST + tasks,
                Resource.CMD_MARK, s -> markTaskStatus(s, true),
                Resource.CMD_UNMK, s -> markTaskStatus(s, false),
                Resource.CMD_TODO, s -> addTask(s, Util.TODO_NEW),
                Resource.CMD_DDL, s -> addTask(s, Util.DDL_NEW),
                Resource.CMD_EVENT, s -> addTask(s, Util.EVENT_NEW),
                Resource.CMD_DEL, this::deleteTask,
                Resource.CMD_FIND, this::find
        );
        storage = new Storage(new File(Util.DATA_FILE_PATH));
        storage.load(tasks);
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
            idx = Parser.parseIdx(args);
            tasks.boundsCheck(idx);
        } catch (MeggyException e) {
            return e.getMessage() + Util.usageIdxCmd(newStatus ? Resource.CMD_MARK : Resource.CMD_UNMK);
        }
        final UserTask task = tasks.get(idx);
        task.setDone(newStatus);
        return (newStatus ? Resource.NOTIF_MARK : Resource.NOTIF_UNMK) + Resource.TASK_STRING_INDENT + task + '\n';
    }

    /**
     * Adds task to the bottom of {@code tasks} list.
     *
     * @param args    Non-null. Unparsed task description string.
     * @param newTask Non-null. Constructor of task to accept {@code args}.
     * @return Response to "todo/ddl/event" command.
     */
    private String addTask(String args, Function<String, UserTask> newTask) throws MeggyException {
        final UserTask task = newTask.apply(args);
        tasks.add(task);
        storage.save(tasks);
        return Resource.NOTIF_ADD + reportChangedTaskAndList(task);
    }

    /**
     * Deletes task from {@code tasks} list. Currently O(n) runtime.
     *
     * @param arg Non-null. Index (start with 1) string of task to be updated.
     * @return Response to 'delete' command.
     * @throws MeggyException If storage file IO throws {@link IOException}.
     */
    private String deleteTask(String arg) throws MeggyException {
        final int idx;
        try {
            idx = Parser.parseIdx(arg);
            tasks.boundsCheck(idx);
        } catch (MeggyException e) {
            return e.getMessage() + Util.usageIdxCmd(Resource.CMD_DEL);
        }
        final UserTask task = tasks.remove(idx);
        storage.save(tasks);
        return Resource.NOTIF_DEL + reportChangedTaskAndList(task);
    }

    /**
     * Formats the string of the recently modified task and {@code tasks} list.
     *
     * @param task Non-null. The recently modified task.
     */
    private String reportChangedTaskAndList(UserTask task) {
        return Resource.TASK_STRING_INDENT + task + '\n' + Resource.nTaskFmt(tasks.size());
    }

    /**
     * Lists all the tasks with description containing the keyword.
     *
     * @param substring Non-null. The keyword to look for.
     * @return The printable string of the listed tasks.
     * @throws MeggyNoArgException If user search keyword is blank.
     */
    private String find(String substring) throws MeggyNoArgException {
        if ("".equals(substring)) {
            throw new MeggyNoArgException();
        }
        final TaskList ans = new TaskList();
        for (UserTask task : tasks) {
            if (task.desc.contains(substring)) {
                ans.add(task);
            }
        }
        return Resource.NOTIF_FIND + ans;
    }

    /**
     * Parses and executes user's input line.
     *
     * @param in Non-null. User's raw input line.
     * @return Complete response of this chatbot. Either the response of a valid query or error message.
     */
    public String getResponse(String in) {
        try {
            final Parser.JobAndArg<String> jobAndArg = Parser.parseJobAndArg(usrCmdToJob, in);
            final Function<String, String> job = jobAndArg.job == null ? unknownCmdBehavior : jobAndArg.job;
            return job.apply(jobAndArg.args);
        } catch (MeggyException e) {
            return e.getMessage();
        }
        // if (Resource.CMD_EXIT.equals(jobAndArg.cmd)) return;
    }
}
