/*
 * Copyright 2017-2018 Deltix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package deltix.vtype.common;

/**
 * Crude AdHoc "Logger" class. Extra dependencies are dangerous in a Java Agent, so this should do.
 * Main feature of this simple logger class it that it uses "standard" printf syntax,
 * but only generates garbage if it actually prints something (not counting boxing operations for ints).
 * It has configurable logging level, and can throw exceptions instead of just logging an error,
 * or be used to set a breakpoint before throwing the exception, for easier debugging
 */

public class CrudeLogger {
    public final static int TRACE = 0;
    public final static int DBG = 1;
    public final static int WRN = 2;
    public final static int ERR = 3;

    static private final String[] prefixes = {"", "DBG: ", "WRN: ", "ERR: "};
    private int level;
    private int throwLevel = ERR;
    private int breakLevel = ERR;

    public void debuggerBreak() {

        level += 0; // <- set breakpoint here
    }

    private void checkBreakpoint(int lvl) {

        if (lvl >= breakLevel) {
            debuggerBreak();
        }
    }

    public void print(String str) {
        System.out.println(str);
    }


    private void breakOrThrow(int lvl, String out) {
        checkBreakpoint(lvl);
        if (lvl >= throwLevel) {
            throw new RuntimeException(out);
        }
    }


    public void print(int lvl, String str, Object ... args) {
        String out = String.format(prefixes[lvl] + str, args);
        (lvl >= ERR ? System.err : System.out).println(out);
        breakOrThrow(lvl, out);
    }

    public void print(String str, Object ... args) {
        System.out.printf(str, args);
    }

    // Debugging helper
    public void breakOnTrace() {
        if (level <= TRACE) {
            debuggerBreak();
        }
    }

    public void trace(String str) {
        if (level <= TRACE) print( str);
    }
    public void trace(String str, Object param1) {
        if (level <= TRACE) print(TRACE, str, param1);
    }
    public void trace(String str, Object param1, Object param2) {
        if (level <= TRACE) print(TRACE, str, param1, param2);
    }
    public void trace(String str, Object param1, Object param2, Object param3) {
        if (level <= TRACE) print(TRACE, str, param1, param2, param3);
    }
    public void trace(String str, Object param1, Object param2, Object param3, Object param4) {
        if (level <= TRACE) print(TRACE, str, param1, param2, param3, param4);
    }

    public void dbg(String str) {
        if (level <= DBG) print(str);
    }
    public void dbg(String str, Object param1) {
        if (level <= DBG) print(DBG, str, param1);
    }
    public void dbg(String str, Object param1, Object param2) {
        if (level <= DBG) print(DBG, str, param1, param2);
    }
    public void dbg(String str, Object param1, Object param2, Object param3) {
        if (level <= DBG) print(DBG, str, param1, param2, param3);
    }
    public void dbg(String str, Object param1, Object param2, Object param3, Object param4) {
        if (level <= DBG) print(DBG, str, param1, param2, param3, param4);
    }

    public void dbg(String str, Object param1, Object param2, Object param3, Object param4, Object param5) {
        if (level <= DBG) print(DBG, str, param1, param2, param3, param4, param5);
    }

    public void wrn(String str) {
        if (level <= WRN) print(str);
    }
    public void wrn(String str, Object param1) {
        if (level <= WRN) print(WRN, str, param1);
    }
    public void wrn(String str, Object param1, Object param2) {
        if (level <= WRN) print(WRN, str, param1, param2);
    }
    public void wrn(String str, Object param1, Object param2, Object param3) {
        if (level <= WRN) print(WRN, str, param1, param2, param3);
    }
    public void wrn(String str, Object param1, Object param2, Object param3, Object param4) {
        if (level <= WRN) print(WRN, str, param1, param2, param3, param4);
    }

    public void err(String str) {
        if (level <= ERR) print(ERR, str);
    }
    public void err(String str, Object param1) {
        if (level <= ERR) print(ERR, str, param1);
    }
    public void err(String str, Object param1, Object param2) {
        if (level <= ERR) print(ERR, str, param1, param2);
    }
    public void err(String str, Object param1, Object param2, Object param3) {
        if (level <= ERR) print(ERR, str, param1, param2, param3);
    }
    public void err(String str, Object param1, Object param2, Object param3, Object param4) {
        if (level <= ERR) print(ERR, str, param1, param2, param3, param4);
    }

    public void setLogLevel(int level) {
        this.level = level;
    }

    public CrudeLogger(int loglevel) {
        this.level = loglevel;
    }

    public CrudeLogger() {
        this(WRN);
    }

    public CrudeLogger(CrudeLogger other) {
        this.level = other.level;
    }

    public int getLevel() {
        return this.level;
    }

    public int getThrowLevel() {
        return throwLevel;
    }

    public void setThrowLevel(int throwLevel) {
        this.throwLevel = throwLevel;
    }

    public void setBreakLevel(int breakLevel) {
        this.breakLevel = breakLevel;
    }

    public boolean on(int level) {
        return this.level <= level;
    }
}
