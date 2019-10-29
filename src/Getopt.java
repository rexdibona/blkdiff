/*
 * Decompiled with CFR 0.146.
 */
package gnu.getopt;

import gnu.getopt.LongOpt;
import java.io.PrintStream;
import java.text.MessageFormat;

public class Getopt {
    protected static final int REQUIRE_ORDER = 1;
    protected static final int PERMUTE = 2;
    protected static final int RETURN_IN_ORDER = 3;
    protected String optarg;
    protected int optind = 0;
    protected boolean opterr = true;
    protected int optopt = 63;
    protected String nextchar;
    protected String optstring;
    protected LongOpt[] long_options;
    protected boolean long_only;
    protected int longind;
    protected boolean posixly_correct;
    protected boolean longopt_handled;
    protected int first_nonopt = 1;
    protected int last_nonopt = 1;
    private boolean endparse = false;
    protected String[] argv;
    protected int ordering;
    protected String progname;

    public Getopt(String progname, String[] argv, String optstring) {
        this(progname, argv, optstring, null, false);
    }

    public Getopt(String progname, String[] argv, String optstring, LongOpt[] long_options) {
        this(progname, argv, optstring, long_options, false);
    }

    public Getopt(String progname, String[] argv, String optstring, LongOpt[] long_options, boolean long_only) {
        if (optstring.length() == 0) {
            optstring = " ";
        }
        this.progname = progname;
        this.argv = argv;
        this.optstring = optstring;
        this.long_options = long_options;
        this.long_only = long_only;
        this.posixly_correct = System.getProperty("gnu.posixly_correct", null) != null;
        if (optstring.charAt(0) == '-') {
            this.ordering = 3;
            if (optstring.length() > 1) {
                this.optstring = optstring.substring(1);
            }
        } else if (optstring.charAt(0) == '+') {
            this.ordering = 1;
            if (optstring.length() > 1) {
                this.optstring = optstring.substring(1);
            }
        } else {
            this.ordering = this.posixly_correct ? 1 : 2;
        }
    }

    public void setOptstring(String optstring) {
        if (optstring.length() == 0) {
            optstring = " ";
        }
        this.optstring = optstring;
    }

    public int getOptind() {
        return this.optind;
    }

    public void setOptind(int optind) {
        this.optind = optind;
    }

    public void setArgv(String[] argv) {
        this.argv = argv;
    }

    public String getOptarg() {
        return this.optarg;
    }

    public void setOpterr(boolean opterr) {
        this.opterr = opterr;
    }

    public int getOptopt() {
        return this.optopt;
    }

    public int getLongind() {
        return this.longind;
    }

    protected void exchange(String[] argv) {
        int bottom = this.first_nonopt;
        int middle = this.last_nonopt;
        int top = this.optind;
        while (top > middle && middle > bottom) {
            int i;
            int len;
            String tem;
            if (top - middle > middle - bottom) {
                len = middle - bottom;
                for (i = 0; i < len; ++i) {
                    tem = argv[bottom + i];
                    argv[bottom + i] = argv[top - (middle - bottom) + i];
                    argv[top - (middle - bottom) + i] = tem;
                }
                top -= len;
                continue;
            }
            len = top - middle;
            for (i = 0; i < len; ++i) {
                tem = argv[bottom + i];
                argv[bottom + i] = argv[middle + i];
                argv[middle + i] = tem;
            }
            bottom += len;
        }
        this.first_nonopt += this.optind - this.last_nonopt;
        this.last_nonopt = this.optind;
    }

    /*
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Lifted jumps to return sites
     */
    protected int checkLongOption() {
        pfound = null;
        this.longopt_handled = true;
        ambig = false;
        exact = false;
        this.longind = -1;
        nameend = this.nextchar.indexOf("=");
        if (nameend == -1) {
            nameend = this.nextchar.length();
        }
        for (i = 0; i < this.long_options.length; ++i) {
            if (!this.long_options[i].getName().startsWith(this.nextchar.substring(0, nameend))) continue;
            if (this.long_options[i].getName().equals(this.nextchar.substring(0, nameend))) {
                pfound = this.long_options[i];
                this.longind = i;
                exact = true;
                break;
            }
            if (pfound == null) {
                pfound = this.long_options[i];
                this.longind = i;
                continue;
            }
            ambig = true;
        }
        if (ambig && !exact) {
            if (this.opterr) {
                msgArgs = new Object[]{this.progname, this.argv[this.optind]};
                System.err.println(MessageFormat.format("getopt.ambigious", msgArgs));
            }
            this.nextchar = "";
            this.optopt = 0;
            ++this.optind;
            return 63;
        }
        if (pfound == null) {
            this.longopt_handled = false;
            return 0;
        }
        ++this.optind;
        if (nameend == this.nextchar.length()) ** GOTO lbl48
        if (pfound.has_arg != 0) {
            this.optarg = this.nextchar.substring(nameend).length() > 1 ? this.nextchar.substring(nameend + 1) : "";
        } else {
            if (this.opterr) {
                if (this.argv[this.optind - 1].startsWith("--")) {
                    msgArgs = new Object[]{this.progname, pfound.name};
                    System.err.println(MessageFormat.format("getopt.arguments1", msgArgs));
                } else {
                    msgArgs = new Object[]{this.progname, new Character(this.argv[this.optind - 1].charAt(0)).toString(), pfound.name};
                    System.err.println(MessageFormat.format("getopt.arguments2", msgArgs));
                }
            }
            this.nextchar = "";
            this.optopt = pfound.val;
            return 63;
lbl48: // 1 sources:
            if (pfound.has_arg == 1) {
                if (this.optind < this.argv.length) {
                    this.optarg = this.argv[this.optind];
                    ++this.optind;
                } else {
                    if (this.opterr) {
                        msgArgs = new Object[]{this.progname, this.argv[this.optind - 1]};
                        System.err.println(MessageFormat.format("getopt.requires", msgArgs));
                    }
                    this.nextchar = "";
                    this.optopt = pfound.val;
                    if (this.optstring.charAt(0) != ':') return 63;
                    return 58;
                }
            }
        }
        this.nextchar = "";
        if (pfound.flag == null) return pfound.val;
        pfound.flag.setLength(0);
        pfound.flag.append(pfound.val);
        return 0;
    }

    public int getopt() {
        int c;
        this.optarg = null;
        if (this.endparse) {
            return -1;
        }
        if (this.nextchar == null || this.nextchar.equals("")) {
            if (this.last_nonopt > this.optind) {
                this.last_nonopt = this.optind;
            }
            if (this.first_nonopt > this.optind) {
                this.first_nonopt = this.optind;
            }
            if (this.ordering == 2) {
                if (this.first_nonopt != this.last_nonopt && this.last_nonopt != this.optind) {
                    this.exchange(this.argv);
                } else if (this.last_nonopt != this.optind) {
                    this.first_nonopt = this.optind;
                }
                while (this.optind < this.argv.length && (this.argv[this.optind].equals("") || this.argv[this.optind].charAt(0) != '-' || this.argv[this.optind].equals("-"))) {
                    ++this.optind;
                }
                this.last_nonopt = this.optind;
            }
            if (this.optind != this.argv.length && this.argv[this.optind].equals("--")) {
                ++this.optind;
                if (this.first_nonopt != this.last_nonopt && this.last_nonopt != this.optind) {
                    this.exchange(this.argv);
                } else if (this.first_nonopt == this.last_nonopt) {
                    this.first_nonopt = this.optind;
                }
                this.last_nonopt = this.argv.length;
                this.optind = this.argv.length;
            }
            if (this.optind == this.argv.length) {
                if (this.first_nonopt != this.last_nonopt) {
                    this.optind = this.first_nonopt;
                }
                return -1;
            }
            if (this.argv[this.optind].equals("") || this.argv[this.optind].charAt(0) != '-' || this.argv[this.optind].equals("-")) {
                if (this.ordering == 1) {
                    return -1;
                }
                this.optarg = this.argv[this.optind++];
                return 1;
            }
            this.nextchar = this.argv[this.optind].startsWith("--") ? this.argv[this.optind].substring(2) : this.argv[this.optind].substring(1);
        }
        if (this.long_options != null && (this.argv[this.optind].startsWith("--") || this.long_only && (this.argv[this.optind].length() > 2 || this.optstring.indexOf(this.argv[this.optind].charAt(1)) == -1))) {
            c = this.checkLongOption();
            if (this.longopt_handled) {
                return c;
            }
            if (!this.long_only || this.argv[this.optind].startsWith("--") || this.optstring.indexOf(this.nextchar.charAt(0)) == -1) {
                if (this.opterr) {
                    if (this.argv[this.optind].startsWith("--")) {
                        Object[] msgArgs = new Object[]{this.progname, this.nextchar};
                        System.err.println(MessageFormat.format("getopt.unrecognized", msgArgs));
                    } else {
                        Object[] msgArgs = new Object[]{this.progname, new Character(this.argv[this.optind].charAt(0)).toString(), this.nextchar};
                        System.err.println(MessageFormat.format("getopt.unrecognized2", msgArgs));
                    }
                }
                this.nextchar = "";
                ++this.optind;
                this.optopt = 0;
                return 63;
            }
        }
        c = this.nextchar.charAt(0);
        this.nextchar = this.nextchar.length() > 1 ? this.nextchar.substring(1) : "";
        String temp = null;
        if (this.optstring.indexOf(c) != -1) {
            temp = this.optstring.substring(this.optstring.indexOf(c));
        }
        if (this.nextchar.equals("")) {
            ++this.optind;
        }
        if (temp == null || c == 58) {
            if (this.opterr) {
                if (this.posixly_correct) {
                    Object[] msgArgs = new Object[]{this.progname, new Character((char)c).toString()};
                    System.err.println(MessageFormat.format("getopt.illegal", msgArgs));
                } else {
                    Object[] msgArgs = new Object[]{this.progname, new Character((char)c).toString()};
                    System.err.println(MessageFormat.format("getopt.invalid", msgArgs));
                }
            }
            this.optopt = c;
            return 63;
        }
        if (temp.charAt(0) == 'W' && temp.length() > 1 && temp.charAt(1) == ';') {
            if (!this.nextchar.equals("")) {
                this.optarg = this.nextchar;
            } else {
                if (this.optind == this.argv.length) {
                    if (this.opterr) {
                        Object[] msgArgs = new Object[]{this.progname, new Character((char)c).toString()};
                        System.err.println(MessageFormat.format("getopt.requires2", msgArgs));
                    }
                    this.optopt = c;
                    if (this.optstring.charAt(0) == ':') {
                        return 58;
                    }
                    return 63;
                }
                this.nextchar = this.argv[this.optind];
                this.optarg = this.argv[this.optind];
            }
            c = this.checkLongOption();
            if (this.longopt_handled) {
                return c;
            }
            this.nextchar = null;
            ++this.optind;
            return 87;
        }
        if (temp.length() > 1 && temp.charAt(1) == ':') {
            if (temp.length() > 2 && temp.charAt(2) == ':') {
                if (!this.nextchar.equals("")) {
                    this.optarg = this.nextchar;
                    ++this.optind;
                } else {
                    this.optarg = null;
                }
                this.nextchar = null;
            } else {
                if (!this.nextchar.equals("")) {
                    this.optarg = this.nextchar;
                    ++this.optind;
                } else {
                    if (this.optind == this.argv.length) {
                        if (this.opterr) {
                            Object[] msgArgs = new Object[]{this.progname, new Character((char)c).toString()};
                            System.err.println(MessageFormat.format("getopt.requires2", msgArgs));
                        }
                        this.optopt = c;
                        if (this.optstring.charAt(0) == ':') {
                            return 58;
                        }
                        return 63;
                    }
                    this.optarg = this.argv[this.optind];
                    ++this.optind;
                    if (this.posixly_correct && this.optarg.equals("--")) {
                        if (this.optind == this.argv.length) {
                            if (this.opterr) {
                                Object[] msgArgs = new Object[]{this.progname, new Character((char)c).toString()};
                                System.err.println(MessageFormat.format("getopt.requires2", msgArgs));
                            }
                            this.optopt = c;
                            if (this.optstring.charAt(0) == ':') {
                                return 58;
                            }
                            return 63;
                        }
                        this.optarg = this.argv[this.optind];
                        ++this.optind;
                        this.first_nonopt = this.optind;
                        this.last_nonopt = this.argv.length;
                        this.endparse = true;
                    }
                }
                this.nextchar = null;
            }
        }
        return c;
    }
}
