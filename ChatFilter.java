package chatapplication;

import java.util.*;
import java.io.*;

public class ChatFilter {
    private static List<String> badWords = new ArrayList<>();

    public ChatFilter(String badWordsFileName) {
        String line = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(badWordsFileName));
            System.out.println("Banned Words File: " + badWordsFileName);
            System.out.println("Banned Words:");

            while((line = br.readLine()) != null) {
                badWords.add(line);
                System.out.println(line);
            }
        }
        catch (IOException e) {
            System.out.println("Couldn't find file for badwords called  \"" + badWordsFileName + "\"");
        }
    }

    public String filter(String msg) {
        String cleanMessage = msg;

        for(String c : badWords) {
            if(cleanMessage.toLowerCase().contains(c.toLowerCase())) {
                String replace = "";
                for(int i=0; i<c.length(); i++) {
                    replace += "*";
                }
                // This (?i) thing is black magic, but w/e
                cleanMessage = cleanMessage.replaceAll("(?i)" + c, replace);
            }
        }

        return cleanMessage;
    }
}