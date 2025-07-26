package com.thizthizzydizzy.midilogio.voice;
import com.thizthizzydizzy.midilogio.MidiLogIO;
import com.thizthizzydizzy.midilogio.connection.Connection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
public class MidiLogIOVoice{
    public static void main(String[] args) throws MidiUnavailableException, IOException, InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        var io = new MidiLogIO();
        io.addConnectionListener((conn) -> {
            if(conn.capabilities.contains("[MidiLogIO-Voice]")){
                try{
                    reinitialize(conn);
                }catch(InvalidMidiDataException ex){
                    Logger.getLogger(MidiLogIOVoice.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        io.start();

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))){
            String line;
            while((line = reader.readLine())!=null&&!line.equals("exit"));
        }
        SpeechRecognitionManager.stop();
        Thread t = new Thread(() -> {
            try{
                Thread.sleep(5000);
            }catch(InterruptedException ex){
                Logger.getLogger(MidiLogIOVoice.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.exit(0);
        }, "Assured Shutdown Thread");
        t.setDaemon(true);
        t.start();
    }
    private static void reinitialize(Connection conn) throws InvalidMidiDataException{
        ArrayList<String> commands = new ArrayList<>();
        conn.addLineListener((line) -> {
            if(line.startsWith("[MidiLogIO-Voice] COMMAND - ")){
                commands.add(line.substring("[MidiLogIO-Voice] COMMAND - ".length()));
            }
            if(line.startsWith("[MidiLogIO-Voice] END INIT")){
                String grammar = """
                                 #JSGF V1.0;
                                 grammar grammar;
                                 <junk_phrase> = <junk>""";
                int maxWords = Integer.MAX_VALUE;
                for(String command : commands){
                    maxWords = Math.min(maxWords, command.split(" ").length);
                }
                for(int i = 0; i<Math.min(Math.sqrt(maxWords), maxWords-2); i++){
                    grammar += " [<junk>]";
                }
                grammar += ";\n<junk> = a ";
                String importantConsonants = "dfhjklnprst";
                String vowels = "aeiou";
                for(char v : vowels.toCharArray()){
                    for(char c1 : importantConsonants.toCharArray()){
                        for(char c2 : importantConsonants.toCharArray()){
                            if(c1==c2)continue;
                            grammar += "|"+c1+""+v+""+c2;
                            grammar += "|"+v+""+c1+""+c2;
                        }
                    }
                }
                grammar += ";";
                int i = 0;
                for(String command : commands){
                    String stripped = "";
                    for(char c : command.toCharArray())if(Character.isLetterOrDigit(c)||Character.isWhitespace(c))stripped += c;
                    grammar += "\npublic <cmd"+i+++"> = "+stripped+";";
                    String[] words = stripped.split(" ");
                    String commandWithoutLastTwoWords = "";
                    for(int w = 0; w<words.length-2; w++){
                        commandWithoutLastTwoWords += words[w]+" ";
                    }
                    commandWithoutLastTwoWords = commandWithoutLastTwoWords.trim();
                    grammar += "\npublic <cmd"+i+++"-junk> = "+commandWithoutLastTwoWords+" <junk>;";
                }
                try{
                    SpeechRecognitionManager.restartSpeechRecognition(grammar);
                    SpeechRecognitionManager.callback = (command) -> {
                        for(var cmd : commands){
                            if(command.contains(cmd)){
                                try{
                                    conn.sendLine(cmd);
                                }catch(InvalidMidiDataException ex){
                                    Logger.getLogger(MidiLogIOVoice.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                break;
                            }
                        }
                    };
                }catch(IOException|InterruptedException|NoSuchFieldException|IllegalArgumentException|IllegalAccessException ex){
                    Logger.getLogger(MidiLogIOVoice.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        conn.sendLine("[MidiLogIO-Voice] INIT");
    }
}
