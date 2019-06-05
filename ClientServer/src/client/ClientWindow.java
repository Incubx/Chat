package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Random;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.BadLocationException;


public class ClientWindow extends JFrame {

    private static final int SERVER_PORT = 3443;
    // клиентский сокет
    private Socket clientSocket;
    // входящее сообщение
    private InputStream inStream;
    private BufferedInputStream inBStream;
    private Scanner inMessage;
    // исходящее сообщение
    private OutputStream outStream;
    private BufferedOutputStream outBStream;
    private PrintWriter outMessage;
    // следующие поля отвечают за элементы формы
    private JTextField jtfMessage;
    //private JTextField jtfName;
    // имя клиента
    private String clientName = "";
    private JButton jbSendMessage;
    private JLabel jlNumberOfClients;
    private JPanel mainPanel;
    private JTextPane jtaTextAreaMessage12;
    private JScrollPane Scroller;
    private JList ClientList;
    private JButton write_btn;
    private JButton stop_btn;
    private JButton play_btn;
    private ArrayList<String> clients;
    private ArrayList<Color> clientscolors;
    private Random rand = new Random();
    private StyledDocument doc;
    private DefaultListModel listmodel;
    private int counter;
    private AudioCapture01 sounder = new AudioCapture01();
    private boolean downloading;
    private ArrayList<byte[] > DataList;

    // конструктор, если не получилось подключиться, то возвращаемся к начальному окну.
    public ClientWindow(String SERVER_HOST,String Nickname) {
        try {
            DataList = new ArrayList<>();
            downloading = false;
            counter =0;
             doc = jtaTextAreaMessage12.getStyledDocument();
            clients = new ArrayList<>();
            clientscolors= new ArrayList<>();
            // подключаемся к серверу
            clientSocket = new Socket(SERVER_HOST, SERVER_PORT);
            inMessage = new Scanner(clientSocket.getInputStream());
            outMessage = new PrintWriter(clientSocket.getOutputStream());
            inStream = clientSocket.getInputStream();
            outStream = clientSocket.getOutputStream();
            outBStream= new BufferedOutputStream(outStream);
            inBStream = new BufferedInputStream(inStream);


            clientName = Nickname;
            sendNick();
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setContentPane(mainPanel);
            setBounds(600, 300, 600, 500);
            setTitle("Chat");
            stop_btn.setEnabled(false);
            play_btn.setEnabled(false);
             listmodel = new DefaultListModel();
             ClientList.setModel(listmodel);
            setVisible(true);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("error");
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            JOptionPane.showMessageDialog(topFrame, "не удалось подключиться к серверу!");
            Start_window restart = new Start_window(SERVER_HOST,Nickname);
            this.dispose();


        }



        // обработчик события нажатия кнопки отправки сообщения
        jbSendMessage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // если имя клиента, и сообщение непустые, то отправляем сообщение
                    sendMsg();
                    // фокус на текстовое поле с сообщением
                    jtfMessage.grabFocus();
                }

        });
        // при фокусе поле сообщения очищается
        jtfMessage.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                jtfMessage.setText("");
            }
        });

        jtfMessage.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    sendMsg();
                    // фокус на текстовое поле с сообщением
                    jtfMessage.grabFocus();
                }
            }
        });

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
                                } else if (inMes.contains("#?#Nick#?#")) {
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

                                } else if (inMes.contains("##VOICE##MESSAGE##")) {

                                    System.out.println("Далее качается аудио файл");
                                    inMessage.reset();
                                    downloading = true;

                                } else if (inMes.contains("##session##end##")) {
                                    String name = getName(inMes);
                                    listmodel.removeElement(name);
                                    int index = clients.indexOf(name);
                                    clients.remove(index);
                                    clientscolors.remove(index);
                                } else {
                                    String[] m = inMes.split("[ ]");
                                    String name = m[0];
                                    Color clr = new Color(0, 0, 0);
                                    //Поиск цвета клиента
                                    for (int i = 0; i < clients.size(); i++) {

                                        if (name.equals(clients.get(i))) {
                                            clr = clientscolors.get(i);
                                            break;
                                        }

                                    }
                                    // выводим сообщение
                                    StringBuilder str = new StringBuilder();
                                    for (int i = 1; i < m.length; i++) {
                                        str.append(m[i]);
                                        str.append(" ");
                                    }
                                    addColoredText(name, clr);
                                    doc.insertString(doc.getLength(), str.toString() + "\n", null);
                                }
                            } else {
                                byte[] DataPart = new byte[10000];
                                inBStream.read(DataPart);
                                String part = new String(DataPart);


                                if (part.contains("##END##OF##VOICE##MESSAGE")) {
                                    System.out.println(part);
                                    System.out.println("Конец голосового сообщения!");
                                    downloading = false;
                                    play_btn.setEnabled(true);
                                } else {
                                    DataList.add(DataPart);
                                    System.out.println(DataPart.length);
                                    System.out.println(DataPart.toString());
                                }

                            }
                        }
                    }
                } catch (Exception e) {
                }

            }
            }).start();


        //Действие при закрытии окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    // отправляем служебное сообщение, которое является признаком того, что клиент вышел из чата
                    outMessage.println(clientName+"  ##session##end##");
                    outMessage.flush();
                    outMessage.close();
                    inMessage.close();
                    clientSocket.close();
                } catch (IOException exc) {

                }
            }
        });

        //Запись звука
        write_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                write_btn.setEnabled(false);
                stop_btn.setEnabled(true);
                play_btn.setEnabled(false);
                //Захват данных
                // с микрофона
                //пока не нажата Stop
                sounder.captureAudio();


            }
        });
        //остановка записи
        stop_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sounder.stopCapture= true;
                write_btn.setEnabled(true);
                stop_btn.setEnabled(false);
                play_btn.setEnabled(true);

            }
        });
        //Воспроизвести пока что сюда же!
        play_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {


                try{
                for(int i=0;i<DataList.size();i++)
                sounder.byteArrayPlayStream.write(DataList.get(i));}
                catch (IOException ex)
                {

                }
                sounder.playAudio();




            }
        });
    }

    // отправка сообщения
    public void sendMsg() {
        if(jtfMessage.getText().isEmpty()) {
            try {
                outMessage.println("##VOICE##MESSAGE##\n");
                outMessage.flush();
                int sdvig = 0;
                byte[] data = sounder.byteArrayOutputStream.toByteArray();
                System.out.println(data.length);
                byte[] data_part = new byte[10000];
                int k = 0;
                while (sdvig < data.length - 1) {

                    for (int i = 0; i < 10000; i++)
                        data_part[i] = data[i + sdvig];



                    outBStream.write(data_part);
                    System.out.println(data_part.toString());
                    System.out.println(data_part.length+" отправлено");
                    outBStream.flush();
                    k++;
                    sdvig += 10000;
                }

                outBStream.flush();
                outMessage.println("##END##OF##VOICE##MESSAGE\n");
                outMessage.flush();
                System.out.println("Отправлено"+k +" частей");
            }
            catch (IOException e)
            {

            }

        }
        else {
            // формируем сообщение для отправки на сервер
            String messageStr = clientName + " : " + jtfMessage.getText();

            // отправляем сообщение
            outMessage.println(messageStr);
            outMessage.flush();
            jtfMessage.setText("");
        }
    }

    //Отправкра начального сообщения с ником
    public void sendNick() {
        // формируем сообщение для отправки на сервер
        String messageStr =clientName+"  #?#Nick#?# ";
        // отправляем сообщение
        outMessage.println(messageStr);
        outMessage.flush();
    }

    //Написать ник цветным
    public void addColoredText(String text, Color color) {
        StyledDocument doc = jtaTextAreaMessage12.getStyledDocument();

        Style style = jtaTextAreaMessage12.addStyle("Color Style", null);
        StyleConstants.setForeground(style, color);
        try {
            doc.insertString(doc.getLength(), text, style);
        }
        catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    //Узнать ник из строки
    private String getName(String inMes){
        String[] m =inMes.split("[ ]");
        String name=m[0];
        return name;
    }


}




