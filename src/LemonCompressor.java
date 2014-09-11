import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Created by Sam on 6/14/14.
 */

public class LemonCompressor
{
    private static Scanner in = null;

    public static void main(String[] args) {
        PrintWriter out = null;

        try {
            in = new Scanner(new File(args[0]));
            out = new PrintWriter(new File(args[1]));
        } catch (Exception e) {
            System.out.println("Usage: java -jar lemon-compressor.jar in.c out.c");
            return;
        }
        in.useDelimiter(",");

        StringBuilder songsDef = new StringBuilder();

        while(in.findInLine("char songname") == null) {
            in.nextLine();
        }
        in.nextLine();

        while(in.findInLine("#include") == null) {
            songsDef.append(in.nextLine() + "\n");
        }
        in.nextLine();
        in.nextLine();

        out.println("int patern ;");
        out.print(songsDef.toString());

        int patternNum = 0;

        do {
            in.nextLine();
            in.nextLine();

            out.println("const UWORD data_song_" + patternNum++ + "[]=");
            out.println("{");
            out.println(parsePattern());
            out.println("};\n");

            in.nextLine();
            in.nextLine();
            in.nextLine();
            in.nextLine();
        } while(in.hasNextLine() && in.nextLine() != null);

        in.close();
        out.close();
    }

    private static String parsePattern() {
        ArrayList<String> lastNotes = new ArrayList<String>();
        StringBuilder result = new StringBuilder();

        String lastRest = "0xFF00";
        String word = null;
        String endTest = null;

        boolean paused = false;
        boolean noteFirst = false;
        boolean pauseFirst = false;

        int pauseCount = 0;
        int voice = 0;
        int dataLength = 0;

        while(endTest == null) {
            endTest = in.findWithinHorizon(Pattern.compile("0x.*(?=\\r\\n\\r\\n};)"), 8);

            word = (endTest == null) ? in.next() : endTest;
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

        return result.toString();
    }
}
