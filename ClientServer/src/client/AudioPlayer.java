package client;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class AudioPlayer {
    volatile ByteArrayOutputStream byteArrayPlayStream;
    AudioInputStream audioInputStream;
    SourceDataLine sourceDataLine;


    public AudioPlayer() throws Exception{

        byteArrayPlayStream = new ByteArrayOutputStream();
        byte audioData[] = new byte[0];

        InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
        AudioFormat audioFormat =
                getAudioFormat();
        audioInputStream =
                new AudioInputStream(byteArrayInputStream, audioFormat, audioData.length / audioFormat.getFrameSize());
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceDataLine.open(audioFormat);
        sourceDataLine.start();
        sourceDataLine.drain();
        sourceDataLine.close();



            //Устанавливаем всё
            //для проигрывания


    }
    public void playAudio() throws  LineUnavailableException  {


            //Создаем поток для проигрывания
            // данных и запускаем его
            // он будет работать пока
            // все записанные данные не проиграются

        byte audioData[] =
                byteArrayPlayStream.
                        toByteArray();

        InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
        AudioFormat audioFormat =
                getAudioFormat();
        audioInputStream =
                new AudioInputStream(byteArrayInputStream, audioFormat, audioData.length / audioFormat.getFrameSize());
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceDataLine.open(audioFormat);
        sourceDataLine.start();
            Thread playThread =
                    new Thread(new AudioPlayer.PlayThread());
            playThread.start();
    }

    //Этот метод создает и возвращает
    // объект AudioFormat

    private AudioFormat getAudioFormat() {
        float sampleRate = 8000.0F;
        //8000,11025,16000,22050,44100
        int sampleSizeInBits = 16;
        //8,16
        int channels = 1;
        //1,2
        boolean signed = true;
        //true,false
        boolean bigEndian = false;
        //true,false
        return new AudioFormat(
                sampleRate,
                sampleSizeInBits,
                channels,
                signed,
                bigEndian);
    }


    class PlayThread extends Thread {
        byte tempBuffer[] = new byte[10000];


        public void run() {
            try {
                Thread.currentThread().setPriority(MAX_PRIORITY);
                int cnt;
                // цикл пока не вернется -1

                while ((cnt = audioInputStream.
                        read(tempBuffer, 0,
                                tempBuffer.length)) != -1) {
                    if (cnt > 0) {
                        //Пишем данные во внутренний
                        // буфер канала
                        // откуда оно передастся
                        // на звуковой выход
                        sourceDataLine.write(
                                tempBuffer, 0, cnt);
                    }
                }

                sourceDataLine.drain();
                sourceDataLine.close();
                byteArrayPlayStream.reset();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }


}