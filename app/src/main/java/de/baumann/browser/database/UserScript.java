/*      Copyright (C) 2023 woheller69

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.baumann.browser.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class UserScript {
    private String script;
    private String type;
    private int id;
    private int rank;
    private boolean active;
    private final List<String> matchPatterns = new ArrayList<>();
    public static String META_BEGIN = "// ==UserScript==";
    public static String META_END = "// ==/UserScript==";
    public static String DOC_START = "document-start";
    public static String DOC_END = "document-end";

    public UserScript(){
    }

    public UserScript(int id, String script, String type, int rank, boolean active){
        this.id = id;
        this.script = script;
        this.type = type;
        this.rank = rank;
        this.active = active;
    }

    public List<String> getMatchPatterns() {return matchPatterns;}

    public String getScript() {
        return script;
    }

    public void setScript(String script) {this.script = script;}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getName(){
        Scanner scanner = new Scanner(script);
        String name ="@name";
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains("@name ")) {
                name = line.split("@name")[1].trim();
                break;
            }
            if (line.contains(META_END)) break;
        }
        scanner.close();
        return name;
    }

    public String getNameSpace(){
        Scanner scanner = new Scanner(script);
        String namespace ="@namespace";
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains("@namepace")) {
                namespace = line.split("@namespace")[1].trim();
                break;
            }
            if (line.contains(META_END)) break;
        }
        scanner.close();
        return namespace;
    }

    public static String getTypefromScript(String script){
        Scanner scanner = new Scanner(script);
        String runAt =DOC_END;  //default value
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains("@run-at")) {
                runAt = line.split("@run-at")[1].trim().equals(DOC_START) ? DOC_START : DOC_END;
                break;
            }
            if (line.contains(META_END)) break;
        }
        scanner.close();
        return runAt;
    }
}