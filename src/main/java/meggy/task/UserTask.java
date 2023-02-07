package meggy.task;

import meggy.Resource;
import meggy.Util;
import meggy.exception.MeggyException;
import meggy.exception.MeggyNoArgException;

/** Entries to be recorded by the chatbot. */
public abstract class UserTask {
    /** Task description. */
    public final String desc;
    /** Task completion status. */
    private boolean isDone;

    /** @param desc Non-null. Description string of task with command removed. */
    public UserTask(String desc) throws MeggyException {
        assert desc != null;
        if (desc.isEmpty()) { // No arguments
            throw new MeggyNoArgException();
        }
        this.desc = desc;
        isDone = false;
    }

    /**
     * Get ask type label from their names.
     *
     * @param taskType Non-null, non-empty. Name of task type.
     * @return Task-type-specific label.
     */
    public static String getTaskTypeLabel(String taskType) {
        assert taskType != null;
        return Util.parenthesize(Character.toUpperCase(taskType.charAt(0)));
    }

    /**
     * Formats the time keywords used to indicate date-time in user input.
     *
     * @param keyword Non-null. Raw time keyword.
     * @return Command-syntax-marking time keyword.
     */
    public static String formatKeyword(String keyword) {
        assert keyword != null;
        return '/' + keyword + ' ';
    }

    /** Update the completion status of this task. */
    public void setDone(boolean done) {
        isDone = done;
    }

    /**
     * @return The string representation of this task in data file format. Currently: re-create the command that would
     *         add the task.
     */
    public abstract String encode();

    /** @return The string representation of this task in text UI. */
    @Override
    public String toString() {
        return Util.parenthesize(isDone ? Resource.DONE_MK : ' ') + ' ' + desc;
    }
}
