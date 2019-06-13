package client;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;


public class ClientWindow extends JFrame {

    private static final int SERVER_PORT = 3443;
    // клиентский сокет
    private Socket clientSocket;
    //Счетчик голосовых сообщений
    private int counter_voice;

    // входящее сообщения (для звука, для текста)
    private InputStream inBStream;
    private Scanner inMessage;
    // исходящее сообщение (для звука, для текста)
    private OutputStream outBStream;
    private PrintWriter outMessage;
    // имя клиента
    private String clientName = "";
    //Массив пользователей и их цвета в чате
    private ArrayList<String> clients;
    private ArrayList<Color> clientscolors;
    //Рандомизатор для цвета
    private Random rand = new Random();
    //Текст из поля чата
    private StyledDocument doc;
    //Список пользоваталей
    private DefaultListModel listmodel;
    private int counter;
    //Объект для записи и воспроизведения звука
    private AudioCapturer capturer;
    private AudioPlayer player;
    //флаг, приходят ли сейчас обычные сообщения или качается аудио.


    private File selectedfile;

    //Обработчик нажатия на кнопки в чате
    private Sound_Listener sound_listener;
    private File_Listener file_listener;


    private byte[] Sound;
    // следующие поля отвечают за элементы формы
    private JTextField MessageField;
    private JButton send_btn;
    private JLabel jlNumberOfClients;
    private JPanel mainPanel;
    private JTextPane CharArea;
    private JList ClientList;
    private JButton play_btn;
    private JScrollPane Scroller;
    private JToggleButton Capture_btn;
    private JButton LoadFileBtn;
    //Ссылка на данное окно, чтобы была возможность закрыть его из другого потока.
    private ClientWindow cl = this;
    private  boolean no_sound;


    // конструктор, если не получилось подключиться, то возвращаемся к начальному окну.
    public ClientWindow(String SERVER_HOST, String Nickname) {
        try {
            no_sound = false;
            ClientList.setFocusable(false);
            jlNumberOfClients.setFocusable(false);
            //Инициализация массивов и объектов
            counter_voice = 0;
            selectedfile = new File("");
            counter = 0;
            doc = CharArea.getStyledDocument();
            sound_listener = new Sound_Listener();
            file_listener= new File_Listener();
            play_btn.setEnabled(false);
            listmodel = new DefaultListModel();
            ClientList.setModel(listmodel);


            clients = new ArrayList<>();
            clientscolors = new ArrayList<>();

            // подключаемся к серверу
            clientSocket = new Socket(SERVER_HOST, SERVER_PORT);
            //Получаем поток записи и считывания (текста и для звука буферный)
            inMessage = new Scanner(clientSocket.getInputStream());
            outMessage = new PrintWriter(clientSocket.getOutputStream());
            /*outBStream = new BufferedOutputStream(clientSocket.getOutputStream());
            inBStream = new BufferedInputStream(clientSocket.getInputStream());*/
            //Узнаем ник клиента
            clientName = Nickname;
            //Отправляем данную информацию на сервер
            sendNick();
            //Начальные настройки окна
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setContentPane(mainPanel);
            setBounds(600, 300, 700, 500);
            setTitle("Chat");
            setVisible(true);

            player = new AudioPlayer();
            System.out.println("Динамик подключен!");
            try{
            capturer = new AudioCapturer();
            System.out.println("Микрофон подключен");
            }
            catch (Exception e){

                JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
                JOptionPane.showMessageDialog(topFrame, "Микрофон не обнаружен!");
                Capture_btn.setEnabled(false);
                ImageIcon imageIcon = new ImageIcon("Icons\\No_micro.png");
                Capture_btn.setIcon(imageIcon);
                System.out.println("Микрофон не подключен");




            }

            //Если не удастся подключиться к серверу вернемся к стартовому окну
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("error");
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            JOptionPane.showMessageDialog(topFrame, "не удалось подключиться к серверу!");
            Start_window restart = new Start_window(SERVER_HOST, Nickname);
            this.dispose();


        }
        catch (Exception e)
        {
            System.out.println("error");
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            JOptionPane.showMessageDialog(topFrame, "динамики не обнаружены!");
             no_sound = true;
            play_btn.setEnabled(false);
            try{
                capturer = new AudioCapturer();
                System.out.println("Микрофон подключен");}
            catch (Exception ex){

                JFrame topFrame1 = (JFrame) SwingUtilities.getWindowAncestor(this);
                JOptionPane.showMessageDialog(topFrame1, "Микрофон не обнаружен!");
                System.out.println("Микрофон не подключен");
                Capture_btn.setEnabled(false);
                ImageIcon imageIcon = new ImageIcon("Icons\\No_micro.png");
                Capture_btn.setIcon(imageIcon);


            }
        }


        // в отдельном потоке начинаем работу с сервером
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // бесконечный цикл
                    while (true) {

                        // если есть входящее сообщение
                        if (inMessage.hasNext()) {
                            // считываем его
                            String inMes = inMessage.nextLine();
                            System.out.println(inMes);
                            //Если это информация о кол-ве клиентов
                            if (inMes.contains("Клиентов в чате = ")) {
                                jlNumberOfClients.setText(inMes);
                            }
                            //Если Ник занят, уходим в стартовое окно.
                            else if (inMes.contains("##INVALID##NAME##")) {
                                JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(cl);
                                JOptionPane.showMessageDialog(topFrame, "Данный Никнейм Занят!");
                                Start_window restart = new Start_window(SERVER_HOST, "");
                                cl.dispose();

                            }
                            //Если информация о нике другого пользователя,
                            //Создаем для него цвет и добавляем в список
                            else if (inMes.contains("#?#Nick#?#")) {
                                String name = getName(inMes);
                                clients.add(name);
                                counter++;
                                Color clr = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
                                clr.brighter();
                                clientscolors.add(clr);
                                //Добавление в список имени клиента
                                listmodel.addElement(name);
                                ClientList.setSelectedIndex(counter);
                                ClientList.ensureIndexIsVisible(counter);


                            }
                            //Счетчик сколько до подключения была голосовых сообщений
                            else if (inMes.contains("##VOICES##")) {
                                String[] split = inMes.split("[ ]");
                                counter_voice = Integer.valueOf(split[1]);

                            }
                            //Удаление вышедшего из чата пользователя из списков
                            else if (inMes.contains("##session##end##")) {
                                String name = getName(inMes);
                                listmodel.removeElement(name);
                                int index = clients.indexOf(name);
                                clients.remove(index);
                                clientscolors.remove(index);
                                counter--;
                            }
                            //Если сервер отключился выкидываем на стартовое окно
                            else if (inMes.contains("##SERVER##DOWN##")) {

                                JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(cl);
                                capturer.targetDataLine.close();
                                JOptionPane.showMessageDialog(topFrame, "Подключение разорвано!");
                                Start_window restart = new Start_window(SERVER_HOST, Nickname);
                                cl.dispose();
                            }
                            //На сервер пришло новое звуковое сообщение
                            else if (inMes.contains("##NEW##VOICE##")) {
                                String name = getName(inMes);
                                Color clr = findColor(name);
                                addColoredText(name, clr);

                                JButton voice_btn = new JButton("прослушать сообщение");
                                voice_btn.setActionCommand(Integer.toString(counter_voice));
                                counter_voice++;
                                voice_btn.addActionListener(sound_listener);
                                voice_btn.setFont(new Font("Arial", Font.PLAIN, 12));
                                voice_btn.setOpaque(false);
                                voice_btn.setSize(200, 15);
                                if(no_sound) voice_btn.setEnabled(false);
                                CharArea.setCaretPosition(doc.getLength());
                                CharArea.insertComponent(voice_btn);
                                doc.insertString(doc.getLength(), "\n", null);


                            }
                            //На сервер пришел новый файл
                            else if (inMes.contains("##NEW##FILE##")) { //Clientname,filename,kod
                                String[] mas = inMes.split("[ ]");
                                String name = mas[0];
                                String fileName = mas[1];
                                Color clr = findColor(name);
                                addColoredText(name, clr);
                                System.out.println("Пришел файл");
                                JButton file_btn = new JButton(fileName);
                                file_btn.setActionCommand(fileName);
                                file_btn.addActionListener(file_listener);
                                file_btn.setFont(new Font("Arial", Font.PLAIN, 10));
                                file_btn.setOpaque(false);
                                file_btn.setSize(100, 12);
                                CharArea.setCaretPosition(doc.getLength());
                                CharArea.insertComponent(file_btn);
                                doc.insertString(doc.getLength(), "\n", null);
                            }
                            //Приходит запрошенное голосовое сообщение
                            else if(inMes.contains("##REQUESTED##VOICE##")) {
                                byte[] bytes = new byte[8*1024];
                                int count;
                                inBStream = new DataInputStream(clientSocket.getInputStream());
                                while ((count = inBStream.read(bytes)) > 0) {
                                    System.out.println(bytes[0]);
                                    if(count==1) break;
                                    player.byteArrayPlayStream.write(bytes, 0, count);
                                    player.byteArrayPlayStream.flush();
                                    System.out.println(count);
                                }
                                System.out.println("Принятно!");
                                player.playAudio();
                            }
                            //Приходит запрошенный файл
                            else if(inMes.contains("##REQUESTED##FILE##"))  {
                                String[] mas = inMes.split("[ ]");
                                String filename = mas[1];

                                byte[] bytes = new byte[16*1024];
                                FileOutputStream fout= new FileOutputStream("Downloads\\"+filename);
                                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                                int count;
                                while ((count = in.read(bytes)) > 0) {
                                    if(count==1) break;
                                    fout.write(bytes, 0, count);
                                    System.out.println(count);
                                }
                                String Path= new File(".").getAbsolutePath();
                                Process process = Runtime.getRuntime().exec("explorer.exe "+Path+"\\Downloads");

                                fout.close();
                            }
                            //Пришло обычное сообщение
                            else {
                                String[] m = inMes.split("[ ]");
                                String name = m[0];
                                Color clr = findColor(name);
                                // выводим сообщение
                                StringBuilder str = new StringBuilder();
                                for (int i = 1; i < m.length; i++) {
                                    str.append(m[i]);
                                    str.append(" ");
                                }
                                addColoredText(name, clr);
                                doc.insertString(doc.getLength(), str.toString() + "\n", null);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();


        Capture_btn.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(Capture_btn.isSelected()) {
                    play_btn.setEnabled(false);
                    //Захват данных
                    // с микрофона
                    //пока не отпущена кнопка
                    capturer.byteArrayOutputStream.reset();
                    try {
                        capturer.captureAudio();
                    }
                    catch (LineUnavailableException ex)
                    {
                        ex.printStackTrace();
                    }

                }
                else {
                    capturer.stopCapture = true;
                    try{
                        capturer.captureThread.join();
                        Sound = new byte[capturer.byteArrayOutputStream.size()];
                        Sound = capturer.byteArrayOutputStream.toByteArray();
                        FileOutputStream fout = new FileOutputStream("Temp_sound");
                        fout.write(Sound);
                        fout.flush();
                        fout.close();
                    }
                    catch (InterruptedException ex)
                    {
                        ex.printStackTrace();
                    }
                    catch (IOException ex)
                    {

                    }
                    if(!no_sound) play_btn.setEnabled(true);

                }

            }
        });


        // обработчик события нажатия на кнопки отправки сообщения
        send_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                //отправляем сообщение
                sendMsg();
                // фокус на текстовое поле с сообщением
                MessageField.grabFocus();
            }

        });
        // при фокусе поле сообщения очищается
        MessageField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                MessageField.setText("");
            }
        });
        //При нажатии на Enter отправка сообщения при нажатии на V запись отпускаю клавишу перестаем записывать.
        MessageField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    sendMsg();
                    play_btn.setEnabled(false);
                    // фокус на текстовое поле с сообщением
                    MessageField.grabFocus();
                }
            }
        });

        //Воспроизвести записанное голосовое сообщение
        play_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                player.byteArrayPlayStream = new ByteArrayOutputStream();
                player.byteArrayPlayStream.reset();
                //Получаем звук из записанного
                //Записанный массив перекидываем в воспроизведение.
                try {
                    player.byteArrayPlayStream.write(Sound);
                    player.playAudio();

                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
                catch (LineUnavailableException ex)
                {

                }

                MessageField.grabFocus();


            }
        });

        //Загрузка файла в чат
        LoadFileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                    JFileChooser fileopen = new JFileChooser();
                    int ret = fileopen.showDialog(null, "Прикрепить файл");
                    if (ret == JFileChooser.APPROVE_OPTION) {
                         selectedfile = fileopen.getSelectedFile();
                        ImageIcon image = new ImageIcon("Icons\\skrepka_green.png");
                        LoadFileBtn.setIcon(image);
                    }




            }
        });


        //Действие при закрытии окна
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    // отправляем служебное сообщение, которое является признаком того, что клиент вышел из чата
                    outMessage.println(clientName + "  ##session##end##");
                    outMessage.flush();
                    inMessage.close();
                    Thread.sleep(100);
                    outMessage.close();

                    clientSocket.close();
                } catch (IOException exc) {
                    exc.printStackTrace();

                }
                catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }
        });
    }


    //Узнать ник из строки
    private String getName(String inMes) {
        String[] m = inMes.split("[ ]");
        String name = m[0];
        return name;
    }

    //Поиск цвета пользователя
    private Color findColor(String name) {
        Color clr = new Color(0, 0, 0);
        //Поиск цвета клиента
        for (int i = 0; i < clients.size(); i++) {

            if (name.equals(clients.get(i))) {
                clr = clientscolors.get(i);
                break;
            }

        }
        return clr;
    }

    // отправка сообщения
    private void sendMsg() {
        if(!selectedfile.getName().isEmpty()){
            sendFile();}
        if (MessageField.getText().isEmpty()) {
            try {
                if(Sound.length>0){
                    System.out.println("Отправляем голосовое сообщение");
                    outMessage.println("##VOICE##MESSAGE##");
                    outMessage.flush();
                    Thread.sleep(100);
                    outBStream = new DataOutputStream(clientSocket.getOutputStream());
                    byte[] data = new byte[8*1024];
                    int count;
                    int sdvig =0;
                    FileInputStream fin = new FileInputStream("Temp_sound");
                    while((count=fin.read(data))>0) {
                        outBStream.write(data,0,count);
                        System.out.println(count);
                    }
                    Thread.sleep(100);
                    System.out.println("Отправлено!");
                    outBStream.flush();
                    outBStream.write(-1);
                    play_btn.setEnabled(false);
                    Sound = new byte[0];
                    fin.close();
                    File file = new File("Temp_sound");
                    file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        } else {

            // формируем сообщение для отправки на сервер
            String messageStr = clientName + " : " + MessageField.getText();
            // отправляем сообщение
            outMessage.println(messageStr);
            outMessage.flush();
            MessageField.setText("");


            MessageField.grabFocus();
        }
    }

    //Отправкра начального сообщения с ником
    private void sendNick() {
        // формируем сообщение для отправки на сервер
        String messageStr = clientName + "  #?#Nick#?# ";
        // отправляем сообщение
        outMessage.println(messageStr);
        outMessage.flush();
    }

    //Написать ник цветным
    private void addColoredText(String text, Color color) {
        StyledDocument doc = CharArea.getStyledDocument();

        Style style = CharArea.addStyle("Color Style", null);
        StyleConstants.setForeground(style, color);
        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

   public void sendFile(){

           try {
               Thread.sleep(150);
               outMessage.println("##SHARED##FILE## " + selectedfile.getName());
               outMessage.flush();
               Thread.sleep(100);

               byte[] bytes = new byte[16 * 1024];
               FileInputStream in = new FileInputStream(selectedfile);
               DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());


               int count;
               while ((count = in.read(bytes)) > 0) {
                   out.write(bytes, 0, count);
                   System.out.println(count);
               }
               out.flush();
               Thread.sleep(100);
               out.write(-1);
               System.out.println("Файл отправлен");
               selectedfile = new File("");
               ImageIcon image = new ImageIcon("Icons\\skrepka.png");
               LoadFileBtn.setIcon(image);
               Thread.sleep(150);
           }
           catch (IOException e)
           {
               e.printStackTrace();
           }
           catch (InterruptedException e)
           {
               e.printStackTrace();
           }

   }

    //При нажатии на воспроизведение сообщения отсылаем запрос на сервер чтобы скачать аудио
    public class Sound_Listener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            outMessage.println("##DOWNLOAD##VOICE##REQUEST## " + e.getActionCommand());
            outMessage.flush();

        }
    }

    public class File_Listener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            outMessage.println("##DOWNLOAD##FILE##REQUEST## " + e.getActionCommand());
            outMessage.flush();
            System.out.println("Запрос на загрузку");

        }
    }
    //Нажатие на клавишу




}