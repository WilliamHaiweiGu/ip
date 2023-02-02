package meggy.task;

import meggy.MeggyTime;
import meggy.Resource;
import meggy.exception.MeggyException;

/** {@link UserTask} with a due time. */
public class DdlTask extends UserTask {
    /** Bracketed icon of task type. */
    public static final String LABEL = getTaskTypeLabel(Resource.CMD_DDL);
    /** 'Due' keyword formatted to be looked up in user input during parsing. */
    public static final String DUE_KEYWORD_FORMATTED = formatKeyword(Resource.KW_DUE);
    /** Formatted 'Due' keyword length. Cached for later use. */
    public static final int DUE_LEN = DUE_KEYWORD_FORMATTED.length();
    /** Due time. */
    public final MeggyTime due;

    /**
     * @param desc Non-null. Description string of task.
     * @param due  Non-null. Due time.
     */
    private DdlTask(String desc, MeggyTime due) throws MeggyException {
        super(desc);
        this.due = due;
    }

    /**
     * Factory method. Parses description and due time from arguments.
     *
     * @param args Non-null. User input line with command removed.
     */
    public static DdlTask of(String args) throws MeggyException {
        final int kwIdx = args.indexOf(DUE_KEYWORD_FORMATTED);
        // If no key word in args, time is set to "N/A".
        final String desc;
        final MeggyTime due;
        if (kwIdx < 0) {
            desc = args;
            due = MeggyTime.NA;
        } else {
            desc = args.substring(0, kwIdx).trim();
            due = MeggyTime.of(args.substring(kwIdx + DUE_LEN));
        }
        return new DdlTask(desc, due);
    }

    /** @inheritDoc */
    @Override
    public String encode() {
        return Resource.CMD_DDL + ' ' + desc + ' ' + DUE_KEYWORD_FORMATTED + due.encode();
    }

    /** Two {@link DdlTask} objects are equal iff they have same (non-null) description and due time. */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DdlTask)) {
            return false;
        }
        final DdlTask other = (DdlTask) o;
        return due.equals(other.due) && desc.equals(other.desc);
    }

    /** @inheritDoc */
    @Override
    public String toString() {
        return LABEL + super.toString() + " (by: " + due + ')';
    }
}
