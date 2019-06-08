package client;

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
    private int counter_files;
    // входящее сообщения (для звука, для текста)
    private BufferedInputStream inBStream;
    private Scanner inMessage;
    // исходящее сообщение (для звука, для текста)
    private BufferedOutputStream outBStream;
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
    private AudioCapture01 sounder ;
    //флаг, приходят ли сейчас обычные сообщения или качается аудио.
    private boolean downloading;
    //Массив фаргментов аудио
    private ArrayList<byte[]> DataList;

    private byte[] File_Data;
    //Обработчик нажатия на кнопки в чате
    private TestActionListener sound_listener;
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


    // конструктор, если не получилось подключиться, то возвращаемся к начальному окну.
    public ClientWindow(String SERVER_HOST, String Nickname) {
        try {

            ClientList.setFocusable(false);
            jlNumberOfClients.setFocusable(false);
            //Инициализация массивов и объектов
            counter_voice = 0;
            counter_files = 0;
            downloading = false;
            counter = 0;
            doc = CharArea.getStyledDocument();
            sound_listener = new TestActionListener();
            file_listener= new File_Listener();
            play_btn.setEnabled(false);
            listmodel = new DefaultListModel();
            ClientList.setModel(listmodel);

            DataList = new ArrayList<>();
            clients = new ArrayList<>();
            clientscolors = new ArrayList<>();

            // подключаемся к серверу
            clientSocket = new Socket(SERVER_HOST, SERVER_PORT);
            //Получаем поток записи и считывания (текста и для звука буферный)
            inMessage = new Scanner(clientSocket.getInputStream());
            outMessage = new PrintWriter(clientSocket.getOutputStream());
            outBStream = new BufferedOutputStream(clientSocket.getOutputStream());
            inBStream = new BufferedInputStream(clientSocket.getInputStream());
            //Узнаем ник клиента
            clientName = Nickname;
            //Отправляем данную информацию на сервер
            sendNick();
            //Начальные настройки окна
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setContentPane(mainPanel);
            setBounds(600, 300, 600, 500);
            setTitle("Chat");
            setVisible(true);

            sounder = new AudioCapture01();
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
            JOptionPane.showMessageDialog(topFrame, "Голосовое общение не доступно, микрофон не обнаружен");
            Capture_btn.setEnabled(false);
            play_btn.setEnabled(false);
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
                            //Если не загрузка аудио
                            if (!downloading) {
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

                                    //Счетчик сколько до подключения была голосовых сообщений
                                } else if (inMes.contains("##VOICES##")) {
                                    String[] split = inMes.split("[ ]");
                                    counter_voice = Integer.valueOf(split[1]);
                                } else if (inMes.contains("##FILES##")) {
                                    String[] split = inMes.split("[ ]");
                                    counter_files = Integer.valueOf(split[1]);
                                } else if (inMes.contains("##FILED##")) {
                                    int length = inBStream.available();
                                    byte[] File_data = new byte[length];
                                    inBStream.read(File_data);
                                    inMessage = new Scanner(clientSocket.getInputStream());
                                    FileOutputStream fout = new FileOutputStream("Downloaded_file.txt");
                                    fout.write(File_data);
                                    fout.close();

                                }
                                //Начало загрузки звука
                                else if (inMes.contains("##VOICE##MESSAGE##")) {
                                    DataList.clear();
                                    System.out.println("Далее качается аудио файл");
                                    inMessage.reset();
                                    downloading = true;
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
                                    JOptionPane.showMessageDialog(topFrame, "Подключение разорвано!");
                                    Start_window restart = new Start_window(SERVER_HOST, Nickname);
                                    cl.dispose();
                                }
                                //кто-то прислал голосовое, отображение кнопки для воспроизведения
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
                                    CharArea.setCaretPosition(doc.getLength());
                                    CharArea.insertComponent(voice_btn);
                                    doc.insertString(doc.getLength(), "\n", null);


                                } else if (inMes.contains("##NEW##FILE##")) {

                                    String name = getName(inMes);
                                    Color clr = findColor(name);
                                    addColoredText(name, clr);
                                    System.out.println("Пришел файл");
                                    JButton file_btn = new JButton("загрузить файл");
                                    file_btn.setActionCommand(Integer.toString(counter_files));
                                    counter_files++;
                                    file_btn.addActionListener(file_listener);
                                    file_btn.setFont(new Font("Arial", Font.PLAIN, 10));
                                    file_btn.setOpaque(false);
                                    file_btn.setSize(100, 12);
                                    CharArea.setCaretPosition(doc.getLength());
                                    CharArea.insertComponent(file_btn);
                                    doc.insertString(doc.getLength(), "\n", null);
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
                            //Если сейчас качаем аудио
                            else {
                                byte[] DataPart = new byte[10000];
                                inBStream.read(DataPart);
                                String part = new String(DataPart);
                                //Если аудио скачалось, воспроизводим
                                if (part.contains("##END##OF##VOICE##MESSAGE")) {
                                    System.out.println(part);
                                    System.out.println("Конец голосового сообщения!");
                                    downloading = false;
                                    inMessage = new Scanner(clientSocket.getInputStream());
                                    sounder.byteArrayPlayStream.reset();
                                    for (int i = 0; i < DataList.size(); i++) {
                                        sounder.byteArrayPlayStream.write(DataList.get(i));
                                    }
                                    Thread.sleep(20);
                                    sounder.playAudio();
                                    CharArea.grabFocus();
                                    //Продолжаем скачивать
                                } else {
                                    DataList.add(DataPart);
                                    System.out.println(DataPart.length);
                                    System.out.println(DataPart.toString());
                                }


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
                    sounder.captureAudio();

                }
                else {
                    sounder.stopCapture = true;
                    try{
                    Thread.sleep(10);
                    Sound =sounder.byteArrayOutputStream.toByteArray();
                    }
                    catch (InterruptedException ex)
                    {
                        ex.printStackTrace();
                    }

                    play_btn.setEnabled(true);


                }

            }
        });

        //Действие при закрытии окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    // отправляем служебное сообщение, которое является признаком того, что клиент вышел из чата
                    outMessage.println(clientName + "  ##session##end##");
                    outMessage.flush();
                    outMessage.close();
                    inMessage.close();
                    clientSocket.close();
                } catch (IOException exc) {
                    exc.printStackTrace();

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
                sounder.byteArrayPlayStream.reset();
                //Получаем звук из записанного
                //Записанный массив перекидываем в воспроизведение.
                try {
                    sounder.byteArrayPlayStream.write(Sound);
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
                sounder.playAudio();

                MessageField.grabFocus();


            }
        });


        LoadFileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    JFileChooser fileopen = new JFileChooser();
                    int ret = fileopen.showDialog(null, "Открыть файл");
                    if (ret == JFileChooser.APPROVE_OPTION) {
                        File file = fileopen.getSelectedFile();
                        FileInputStream fin = new FileInputStream(file);
                          int length =fin.available();
                          File_Data = new byte[length];
                            fin.read(File_Data);
                            outMessage.println("##NEW##FILE## "+length+" "+file.getName());
                            outMessage.flush();

                            outBStream.write(File_Data);
                        Thread.sleep(10);
                            outBStream.flush();
                            fin.close();
                            System.out.println("Отправлен файл");
                    }


                }
                catch (FileNotFoundException ex)
                {
                    ex.printStackTrace();
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
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
    public void sendMsg() {
        if (MessageField.getText().isEmpty()) {
            try {
                if(Sound.length>0){
                outMessage.println("##VOICE##MESSAGE##\n");
                outMessage.flush();
                int sdvig = 0;
                byte[] data = Sound;
                    System.out.println(data.length);
                    byte[] data_part = new byte[10000];
                    int k = 0;
                    while (sdvig < data.length - 1) {
                        System.arraycopy(data,sdvig,data_part,0,10000);

                        outBStream.write(data_part);
                        System.out.println(data_part.toString());
                        System.out.println(data_part.length + " отправлено");
                        outBStream.flush();
                        k++;
                        sdvig += 10000;
                    }

                    outBStream.flush();
                    outMessage.println("##END##OF##VOICE##MESSAGE\n");
                    outMessage.flush();
                    System.out.println("Отправлено" + k + " частей");
                    Sound=new byte[0];
                    play_btn.setEnabled(false);
                    MessageField.grabFocus();
                }
            } catch (IOException e) {
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

    private String getFileExtension(File file) {
        String fileName = file.getName();
        // если в имени файла есть точка и она не является первым символом в названии файла
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            // то вырезаем все знаки после последней точки в названии файла, то есть ХХХХХ.txt -> txt
            return fileName.substring(fileName.lastIndexOf(".")+1);
            // в противном случае возвращаем заглушку, то есть расширение не найдено
        else return "";
    }

    //При нажатии на воспроизведение сообщения отсылаем запрос на сервер чтобы скачать аудио
    public class TestActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            outMessage.println("##ACTIVATE## " + e.getActionCommand());
            outMessage.flush();

        }
    }

    public class File_Listener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            outMessage.println("##DOWNLOAD##FILE## " + e.getActionCommand());
            outMessage.flush();
            System.out.println("Запрос на загрузку");

        }
    }
    //Нажатие на клавишу

}