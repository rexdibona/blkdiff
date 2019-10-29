/*
 * Decompiled with CFR 0.146.
 */
package gnu.getopt;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class LongOpt {
    public static final int NO_ARGUMENT = 0;
    public static final int REQUIRED_ARGUMENT = 1;
    public static final int OPTIONAL_ARGUMENT = 2;
    protected String name;
    protected int has_arg;
    protected StringBuffer flag;
    protected int val;
    private ResourceBundle _messages = ResourceBundle.getBundle("gnu/getopt/MessagesBundle", Locale.getDefault());

    public LongOpt(String name, int has_arg, StringBuffer flag, int val) throws IllegalArgumentException {
        if (has_arg != 0 && has_arg != 1 && has_arg != 2) {
            Object[] msgArgs = new Object[]{new Integer(has_arg).toString()};
            throw new IllegalArgumentException(MessageFormat.format(this._messages.getString("getopt.invalidValue"), msgArgs));
        }
        this.name = name;
        this.has_arg = has_arg;
        this.flag = flag;
        this.val = val;
    }

    public String getName() {
        return this.name;
    }

    public int getHasArg() {
        return this.has_arg;
    }

    public StringBuffer getFlag() {
        return this.flag;
    }

    public int getVal() {
        return this.val;
    }
}
