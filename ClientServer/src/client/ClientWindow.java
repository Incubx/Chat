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
    private Scanner inMessage;
    // исходящее сообщение
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
    private ArrayList<String> clients;
    private ArrayList<Color> clientscolors;
    private Random rand = new Random();
    private StyledDocument doc;
    private DefaultListModel listmodel;
    private int counter;
    // конструктор
    public ClientWindow(String SERVER_HOST,String Nickname) {
        try {
            counter =0;
             doc = jtaTextAreaMessage12.getStyledDocument();
            clients = new ArrayList<>();
            clientscolors= new ArrayList<>();
            // подключаемся к серверу
            clientSocket = new Socket(SERVER_HOST, SERVER_PORT);
            inMessage = new Scanner(clientSocket.getInputStream());
            outMessage = new PrintWriter(clientSocket.getOutputStream());
            clientName = Nickname;
            sendNick();
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setContentPane(mainPanel);
            setBounds(600, 300, 600, 500);
            setTitle("Chat");
             listmodel = new DefaultListModel();
             ClientList.setModel(listmodel);
            setVisible(true);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("error");
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            JOptionPane.showMessageDialog(topFrame, "не удалось подключиться к серверу!");
            Start_window restart = new Start_window();
            this.dispose();


        }



        // обработчик события нажатия кнопки отправки сообщения
        jbSendMessage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // если имя клиента, и сообщение непустые, то отправляем сообщение
                if (!jtfMessage.getText().trim().isEmpty()) {
                    sendMsg();
                    // фокус на текстовое поле с сообщением
                    jtfMessage.grabFocus();
                }
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
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    if (!jtfMessage.getText().trim().isEmpty()) {
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
                            // считываем его

                            String inMes = inMessage.nextLine();
                            String clientsInChat = "Клиентов в чате = ";
                            if (inMes.indexOf(clientsInChat) == 0) {
                                jlNumberOfClients.setText(inMes);
                            }
                            else if(inMes.contains("#?#Nick#?#")){
                                String name = getName(inMes);
                                clients.add(name);
                                counter++;
                                Color clr = new Color(rand.nextFloat(),rand.nextFloat(),rand.nextFloat());
                                clr.brighter();
                                clientscolors.add(clr);
                               System.out.print("НОВЫЙ КЛИЕНТ!");
                               listmodel.addElement(name);
                               ClientList.setSelectedIndex(counter);
                               ClientList.ensureIndexIsVisible(counter);

                            }
                            else if(inMes.contains("##session##end##"))
                            {
                                String name = getName(inMes);
                                listmodel.removeElement(name);
                            }
                            else {
                                String[] m =inMes.split("[ ]");
                                String name=m[0];
                                Color clr = new Color(0,0,0);
                                //Поиск цвета клиента
                                for (int i=0;i<clients.size();i++) {

                                    if(name.equals(clients.get(i)))
                                    {
                                        clr = clientscolors.get(i);
                                        break;
                                    }

                                }
                                // выводим сообщение
                                StringBuilder str= new StringBuilder();
                                for(int i=1;i<m.length;i++) {
                                    str.append(m[i]);
                                    str.append(" ");
                                }
                                addColoredText(name,clr);
                                doc.insertString(doc.getLength(),str.toString()+"\n",null);
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        }).start();
        // добавляем обработчик события закрытия окна клиентского приложения
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


    }

    // отправка сообщения
    public void sendMsg() {
        // формируем сообщение для отправки на сервер
        String messageStr = clientName + " : " + jtfMessage.getText();
        // отправляем сообщение
        outMessage.println(messageStr);
        outMessage.flush();
        jtfMessage.setText("");
    }

    public void sendNick() {
        // формируем сообщение для отправки на сервер
        String messageStr =clientName+"  #?#Nick#?# ";
        // отправляем сообщение
        outMessage.println(messageStr);
        outMessage.flush();
    }


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

    private String getName(String inMes){
        String[] m =inMes.split("[ ]");
        String name=m[0];
        return name;
    }

}

