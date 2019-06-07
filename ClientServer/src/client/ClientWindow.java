package client;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
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
    private AudioCapture01 sounder = new AudioCapture01();
    //флаг, приходят ли сейчас обычные сообщения или качается аудио.
    private boolean downloading;
    //Массив фаргментов аудио
    private ArrayList<byte[]> DataList;
    //Обработчик нажатия на кнопки в чате
    private TestActionListener Listener;

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
    private ClientWindow cl = this;


    // конструктор, если не получилось подключиться, то возвращаемся к начальному окну.
    public ClientWindow(String SERVER_HOST, String Nickname) {
        try {

            sounder = new AudioCapture01();
            ClientList.setFocusable(false);
            jlNumberOfClients.setFocusable(false);

            counter_voice = 0;
            DataList = new ArrayList<>();
            downloading = false;
            counter = 0;
            //Инициализация массивов и объектов
            doc = CharArea.getStyledDocument();
            clients = new ArrayList<>();
            clientscolors = new ArrayList<>();
            Listener = new TestActionListener();
            play_btn.setEnabled(false);
            listmodel = new DefaultListModel();
            ClientList.setModel(listmodel);
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
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setContentPane(mainPanel);
            setBounds(600, 300, 600, 500);
            setTitle("Chat");
            setVisible(true);
            //Если не удастся подключиться к серверу вернемся к стартовому окну
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("error");
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            JOptionPane.showMessageDialog(topFrame, "не удалось подключиться к серверу!");
            Start_window restart = new Start_window(SERVER_HOST, Nickname);
            this.dispose();


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

                            if (!downloading) {
                                // считываем его
                                String inMes = inMessage.nextLine();
                                System.out.println(inMes);
                                if (inMes.contains("Клиентов в чате = ")) {
                                    jlNumberOfClients.setText(inMes);
                                }
                                //Если Ник занят
                                else if(inMes.contains("##INVALID##NAME##"))
                                {
                                    JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(cl);
                                    JOptionPane.showMessageDialog(topFrame, "Данный Никнейм Занят!");
                                    Start_window restart = new Start_window(SERVER_HOST, "");
                                    cl.dispose();

                                }
                                else if (inMes.contains("#?#Nick#?#")) {
                                    String name = getName(inMes);
                                    clients.add(name);
                                    counter++;
                                    Color clr = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
                                    clr.brighter();
                                    clientscolors.add(clr);
                                    System.out.print("НОВЫЙ КЛИЕНТ!");
                                    listmodel.addElement(name);
                                    ClientList.setSelectedIndex(counter);
                                    ClientList.ensureIndexIsVisible(counter);

                                } else if (inMes.contains("##VOICES##")) {
                                    String[] split = inMes.split("[ ]");
                                    counter_voice = Integer.valueOf(split[1]);
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
                                }
                                else if(inMes.contains("##SERVER##DOWN##"))
                                {

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
                                    voice_btn.addActionListener(Listener);
                                    voice_btn.setFont(new Font("Arial", Font.PLAIN, 12));
                                    voice_btn.setOpaque(false);
                                    voice_btn.setSize(200, 15);
                                    CharArea.setCaretPosition(doc.getLength());
                                    CharArea.insertComponent(voice_btn);
                                    doc.insertString(doc.getLength(),"\n",null);


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
                                    play_btn.setEnabled(true);
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

                try {
                    sounder.byteArrayPlayStream.write(sounder.byteArrayOutputStream.toByteArray());
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
                sounder.playAudio();

                MessageField.grabFocus();


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
                if(sounder.byteArrayOutputStream.size()>0){
                outMessage.println("##VOICE##MESSAGE##\n");
                outMessage.flush();
                int sdvig = 0;
                byte[] data = sounder.byteArrayOutputStream.toByteArray();
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
                    sounder.byteArrayOutputStream.reset();
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

    //При нажатии на воспроизведение сообщения отсылаем запрос на сервер чтобы скачать аудио
    public class TestActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            outMessage.println("##ACTIVATE## " + e.getActionCommand());
            outMessage.flush();

        }
    }
    //Нажатие на клавишу
}