package client;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AudioCapturer {
    public Thread captureThread;
    public TargetDataLine targetDataLine;
    volatile boolean stopCapture = false;
    volatile ByteArrayOutputStream
            byteArrayOutputStream;
    ByteArrayOutputStream byteArrayPlayStream;
    AudioFormat audioFormat;
    AudioInputStream audioInputStream;
    SourceDataLine sourceDataLine;


    public AudioCapturer()   throws LineUnavailableException {
        byteArrayOutputStream = new ByteArrayOutputStream();

            //Установим все для захвата
            audioFormat = getAudioFormat();
            DataLine.Info dataLineInfo =
                    new DataLine.Info(
                            TargetDataLine.class,
                            audioFormat);
            targetDataLine = (TargetDataLine)
                    AudioSystem.getLine(
                            dataLineInfo);
            targetDataLine.open(audioFormat);
            targetDataLine.start();
            targetDataLine.close();
    }

    //Этот метод захватывает аудио
    // с микрофона и сохраняет
    // в объект ByteArrayOutputStream
    public void captureAudio() throws  LineUnavailableException {

            //Создаем поток для захвата аудио
            // и запускаем его
            //он будет работать
            //пока не нажмут кнопку
        targetDataLine.open(audioFormat);
        targetDataLine.start();
            captureThread =
                    new Thread(
                            new CaptureThread());
            captureThread.start();



    }

    //Этот метод проигрывает аудио
    // данные, которые были сохранены
    // в ByteArrayOutputStream
    public void playAudio() {
        try {

            //Устанавливаем всё
            //для проигрывания

            byte audioData[] =
                    byteArrayPlayStream.
                            toByteArray();

            InputStream byteArrayInputStream
                    = new ByteArrayInputStream(
                    audioData);
            AudioFormat audioFormat =
                    getAudioFormat();
            audioInputStream =
                    new AudioInputStream(
                            byteArrayInputStream,
                            audioFormat,
                            audioData.length / audioFormat.
                                    getFrameSize());
            DataLine.Info dataLineInfo =
                    new DataLine.Info(
                            SourceDataLine.class,
                            audioFormat);
            sourceDataLine = (SourceDataLine)
                    AudioSystem.getLine(
                            dataLineInfo);
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();

            //Создаем поток для проигрывания
            // данных и запускаем его
            // он будет работать пока
            // все записанные данные не проиграются

            Thread playThread =
                    new Thread(new PlayThread());
            playThread.start();

        } catch (Exception e) {
            System.out.println(e);
        }
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
//===================================//

    //Внутренний класс для захвата
// данных с микрофона
    class CaptureThread extends Thread {

        byte tempBuffer[] = new byte[10000];

        public void run() {

            stopCapture = false;
            try {


                while (!stopCapture) {


                    int cnt = targetDataLine.read(
                            tempBuffer,
                            0,
                            tempBuffer.length);
                    if (cnt > 0) {
                        //Сохраняем данные в выходной поток

                        byteArrayOutputStream.write(
                                tempBuffer, 0, cnt);
                    }
                }
                System.out.println(byteArrayOutputStream.size());
                targetDataLine.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
    //===================================//
//Внутренний класс  для
// проигрывания сохраненных аудио данных
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
//===================================//

}//end outer class AudioCapturer.java
