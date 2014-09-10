import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by Sam on 6/14/14.
 */

public class LemonCompressor
{
    private static Scanner in = null;
    private static PrintWriter out = null;

    public static void main(String[] args) {
        try {
            in = new Scanner(new File(args[0]));
            out = new PrintWriter(new File(args[1]));
        } catch (Exception e) {
            System.out.println("Usage: java -jar lemon-compressor.jar in.c out.c");
            return;
        }
        in.useDelimiter(",");

        ArrayList<String> lastNotes = new ArrayList<String>();
        String lastRest = "0xFF00";

        String word;
        boolean paused = false;
        boolean noteFirst = false;
        boolean pauseFirst = false;
        int pauseCount = 0;
        int voice = 0;
        int dataLength = 0;

        StringBuilder result = new StringBuilder();

        while(in.hasNext()) {
            word = in.next();
            word = word.replaceAll("\r\n","");

            if (!paused) {
                if (word.equals("0x9000")) {
                    pauseCount++;
                    paused = true;
                    if (!noteFirst) {
                        pauseFirst = true;
                    }
                } else {
                    lastNotes.add(word);
                    if (!pauseFirst) {
                        noteFirst = true;
                    }
                }
            } else {
                if (word.equals("0x9000")) {
                    pauseCount++;
                } else {
                    String hex = Integer.toHexString(pauseCount);
                    if (pauseCount < 16) {
                        hex = "0" + hex;
                    }
                    if (!(pauseFirst && lastRest.equals("0xFF00"))) {
                        result.append(lastRest + ",");
                        dataLength++;
                    }
                    for (String w : lastNotes) {
                        result.append(w + ",");
                        dataLength++;
                    }
                    result.append("\n");
                    lastNotes.clear();

                    //lastRest = "0xF" + Integer.toHexString(voice) + hex;
                    lastRest = "0xFF" + hex;
                    lastNotes.add(word);

                    pauseCount = 0;
                    paused = false;
                }
            }
            voice++;

            if (voice == 4) {
                voice = 0;
            }
        }
        String hex = Integer.toHexString(pauseCount);
        if (pauseCount < 16) {
            hex = "0" + hex;
        }
        result.append(lastRest + ",");
        dataLength++;
        for (String w : lastNotes) {
            result.append(w + ",");
            dataLength++;
        }
        result.append("\n");
        result.append("0xFF" + hex);
        dataLength++;
        result.insert(0, "0x00" + Integer.toHexString(dataLength) + ",\n");

        out.println(result.toString());
        out.close();
    }

    private static void readDataBlock() throws IOException {
        in.nextLine();
        in.nextLine();

        in.useDelimiter(",");

        String word;
        boolean paused = false;
        int pauseCount = 0;

        StringBuilder result = new StringBuilder();

        while(in.hasNext()) {
            word = in.next();
            if (!paused) {
                if (word.equals("0x9000")) {
                    paused = true;
                }
                else {
                    result.append(word + ",");
                }
            }
            else {
                if (word.equals("0x9000")) {
                    pauseCount++;
                }
                else {
                    result.append("0xFF" + Integer.toHexString(pauseCount) + ",");

                    pauseCount = 0;
                    paused = false;
                }
            }
        }

        in.nextLine();
        in.nextLine();
        in.nextLine();
        in.nextLine();
    }
}
