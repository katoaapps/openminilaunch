/*
 * Lua pattern functions (string.find, match, gmatch and gsub) that charge their work to a budget.
 *
 * LuaJ's own versions run in Java, outside the Lua instruction hook, so a pattern that backtracks
 * badly (such as ("a?"):rep(40) .. ("a"):rep(40) against 40 "a"s) could keep the phone busy for
 * hours. These use LuaJ's matcher but count each step: every match and single-character test costs
 * one, and a %b scan costs the text it could cover. When the budget runs out, Spend throws.
 *
 * The loops below follow LuaJ 3.0.1's StringLib (MIT licence, Copyright (c) 2009-2011 Luaj.org);
 * this file lives in its package to reach the matcher, which is package-private.
 */
package org.luaj.vm2.lib;

import org.luaj.vm2.Buffer;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

public final class MeteredPatterns {
    /** Takes [units] of work from the budget, or throws when it is spent. */
    public interface Spend {
        void spend(int units);
    }

    /** The longest pattern accepted; real patterns are a few dozen characters. */
    public static final int MAX_PATTERN = 512;

    private static final LuaString SPECIALS = LuaValue.valueOf("^$*+?.([%-");

    private MeteredPatterns() {}

    /** Replaces find, match, gmatch and gsub in [string] (the sandbox's string table). */
    public static void install(LuaTable string, final Spend spend) {
        string.set("find", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return findAux(args, true, spend);
            }
        });
        string.set("match", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return findAux(args, false, spend);
            }
        });
        string.set("gmatch", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaString src = args.checkstring(1);
                LuaString pat = pattern(args, 2);
                return new GMatch(state(args, src, pat, spend), src.length());
            }
        });
        string.set("gsub", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return gsub(args, spend);
            }
        });
    }

    private static LuaString pattern(Varargs args, int i) {
        LuaString p = args.checkstring(i);
        if (p.length() > MAX_PATTERN) {
            throw new LuaError("pattern longer than " + MAX_PATTERN + " characters");
        }
        return p;
    }

    private static Metered state(Varargs args, LuaString s, LuaString p, Spend spend) {
        return new Metered(args, s, p, spend);
    }

    private static Varargs findAux(Varargs args, boolean find, Spend spend) {
        LuaString s = args.checkstring(1);
        LuaString pat = pattern(args, 2);
        int init = start(args.optint(3, 1), s.length());
        boolean plain = find && (args.arg(4).toboolean() || pat.indexOfAny(SPECIALS) == -1);
        return plain ? plainFind(s, pat, init) : patternFind(args, s, pat, init, find, spend);
    }

    /** Lua's init: 1-based, negative from the end; as a 0-based offset within the text. */
    private static int start(int init, int length) {
        if (init > 0) {
            return Math.min(init - 1, length);
        }
        return init < 0 ? Math.max(0, length + init) : 0;
    }

    private static Varargs plainFind(LuaString s, LuaString pat, int init) {
        int result = s.indexOf(pat, init);
        if (result == -1) {
            return LuaValue.NIL;
        }
        return LuaValue.varargsOf(LuaValue.valueOf(result + 1), LuaValue.valueOf(result + pat.length()));
    }

    private static Varargs patternFind(Varargs args, LuaString s, LuaString pat, int init, boolean find, Spend spend) {
        Metered ms = state(args, s, pat, spend);
        boolean anchor = pat.length() > 0 && pat.luaByte(0) == '^';
        int poff = anchor ? 1 : 0;
        int soff = init;
        do {
            ms.reset();
            int res = ms.match(soff, poff);
            if (res != -1) {
                if (find) {
                    return LuaValue.varargsOf(LuaValue.valueOf(soff + 1), LuaValue.valueOf(res), ms.push_captures(false, soff, res));
                }
                return ms.push_captures(true, soff, res);
            }
        } while (soff++ < s.length() && !anchor);
        return LuaValue.NIL;
    }

    private static Varargs gsub(Varargs args, Spend spend) {
        LuaString src = args.checkstring(1);
        final int srclen = src.length();
        LuaString p = pattern(args, 2);
        LuaValue repl = args.arg(3);
        int maxS = args.optint(4, srclen + 1);
        final boolean anchor = p.length() > 0 && p.charAt(0) == '^';
        Buffer lbuf = new Buffer(srclen);
        Metered ms = state(args, src, p, spend);
        int soffset = 0;
        int n = 0;
        boolean more = true;
        while (more && n < maxS) {
            ms.reset();
            int res = ms.match(soffset, anchor ? 1 : 0);
            if (res != -1) {
                n++;
                ms.add_value(lbuf, soffset, res, repl);
            }
            if (res != -1 && res > soffset) {
                soffset = res;
            } else if (soffset < srclen) {
                lbuf.append((byte) src.luaByte(soffset++));
            } else {
                more = false;
            }
            more = more && !anchor;
        }
        lbuf.append(src.substring(soffset, srclen));
        return LuaValue.varargsOf(lbuf.tostring(), LuaValue.valueOf(n));
    }

    private static final class GMatch extends VarArgFunction {
        private final Metered ms;
        private final int srclen;
        private int soffset;

        GMatch(Metered ms, int srclen) {
            this.ms = ms;
            this.srclen = srclen;
        }

        @Override
        public Varargs invoke(Varargs args) {
            for (; soffset < srclen; soffset++) {
                ms.reset();
                int res = ms.match(soffset, 0);
                if (res >= 0) {
                    int soff = soffset;
                    soffset = res;
                    return ms.push_captures(true, soff, res);
                }
            }
            return LuaValue.NIL;
        }
    }

    /** LuaJ's matcher, counting its steps. */
    private static final class Metered extends StringLib.MatchState {
        private final Spend spend;

        Metered(Varargs args, LuaString s, LuaString p, Spend spend) {
            super(args, s, p);
            this.spend = spend;
        }

        @Override
        int match(int soffset, int poffset) {
            spend.spend(1);
            return super.match(soffset, poffset);
        }

        @Override
        boolean singlematch(int c, int poff, int ep) {
            spend.spend(1);
            return super.singlematch(c, poff, ep);
        }

        @Override
        int matchbalance(int soff, int poff) {
            spend.spend(Math.max(1, s.length() - soff));
            return super.matchbalance(soff, poff);
        }
    }
}
