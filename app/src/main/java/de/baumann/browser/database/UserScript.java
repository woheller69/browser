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
    private final List<String> patterns = new ArrayList<>();
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

    public List<String> getPatterns() {return patterns;}

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