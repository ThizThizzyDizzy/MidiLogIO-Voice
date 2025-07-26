package com.thizthizzydizzy.midilogio.voice;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.Microphone;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.result.WordResult;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
public class SpeechRecognitionManager{
    private static LiveSpeechRecognizer recognizer;
    private static boolean active;
    public static Consumer<String> callback = null;
    public static void restartSpeechRecognition(String grammarFile) throws IOException, InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        if(recognizer!=null){
            active = false;
            while(recognizer!=null)Thread.sleep(100);
        }

        Files.writeString(new File("grammar\\grammar.gram").toPath(), grammarFile);

        Configuration config = new Configuration();
        config.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        config.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        config.setGrammarPath("grammar");
        config.setGrammarName("grammar");
        config.setUseGrammar(true);
        recognizer = new LiveSpeechRecognizer(config);
        active = true;
        new Thread(() -> {
            try{
                recognizer.startRecognition(true);
            }catch(Exception ex){
                ex.printStackTrace();
                System.exit(0);
            }
            while(active){
                synchronized(recognizer){
                    SpeechResult result = recognizer.getResult();
                    if(result!=null){
                        processSpeech(result.getHypothesis(), result.getWords());
                    }
                }
                try{
                    Thread.sleep(1);
                }catch(InterruptedException ex){
                    break;
                }
            }
            recognizer.stopRecognition();

            try{
                Class<?> recognizerClass = recognizer.getClass();
                Field microphoneField;
                microphoneField = recognizerClass.getDeclaredField("microphone");
                microphoneField.setAccessible(true);
                Microphone microphone = (Microphone)microphoneField.get(recognizer);
                microphone.getStream().close();
            }catch(NoSuchFieldException|SecurityException|IOException|IllegalArgumentException|IllegalAccessException ex){
                Logger.getLogger(SpeechRecognitionManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            recognizer = null;
        }).start();
    }
    private static void processSpeech(String hypothesis, List<WordResult> words){
        if(callback!=null)callback.accept(hypothesis);
        System.out.println(hypothesis);
        System.out.println(words.toString());
    }
    public static void stop(){
        active = false;
    }
}
