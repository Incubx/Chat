


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;



// реализуем интерфейс Runnable, который позволяет работать с потоками
public class ClientHandler implements Runnable {
    // экземпляр нашего сервера
    private Server server;
    // исходящее сообщение
    private PrintWriter outMessage;
    private OutputStream outStream;
    private BufferedOutputStream outBStream;
    private AudioCapture01 sounder;


    // входящее собщение
    private Scanner inMessage;
    private InputStream inStream;
    private BufferedInputStream inBStream;
    private static final int PORT = 3443;
    // клиентский сокет
    private Socket clientSocket = null;
    // количество клиента в чате, статичное поле
    private static int clients_count = 0;
    private static ArrayList<String> clients = new ArrayList<>();
    private String clientName;
    private  boolean soundmessage;
    private static ArrayList<byte[]> DataLst;

    // конструктор, который принимает клиентский сокет и сервер
    public ClientHandler(Socket socket, Server server) {
        try {
            sounder = new AudioCapture01();
            DataLst = new ArrayList<>();
            soundmessage = false;
            clients_count++;
            this.server = server;
            this.clientSocket = socket;
            this.outMessage = new PrintWriter(socket.getOutputStream());
            this.inMessage = new Scanner(socket.getInputStream());
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
            outBStream= new BufferedOutputStream(outStream);
            inBStream = new BufferedInputStream(inStream);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Переопределяем метод run(), который вызывается когда
    // мы вызываем new Thread(client).start();
    @Override
    public void run() {
        try {
            while (true) {
                // Если от клиента пришло сообщение
                if (!soundmessage) {
                    if (inMessage.hasNext()) {

                        String clientMessage = inMessage.nextLine();
                        System.out.println(clientMessage);
                        // если клиент отправляет данное сообщение, то цикл прерывается и
                        // клиент выходит из чата
                        if (clientMessage.contains("##session##end##")) {
                            String[] m = clientMessage.split("[ ]");
                            String name = m[0];
                            clients.remove(name);
                            server.sendMessageToAllClients(name + "  вышел из чата!");
                            server.sendMessageToAllClients(clientMessage);
                            break;
                        }
                        //Оповещение о новом клиенте
                        else if (clientMessage.contains("#?#Nick#?#")) {

                            String[] m = clientMessage.split("[ ]");
                            String name = m[0];
                            clientName = name;
                            for (int i = 0; i < clients.size(); i++) {
                                sendMsg(clients.get(i) + "  #?#Nick#?#");
                            }
                            server.sendMessageToAllClients(clientMessage);
                            clients.add(name);
                            server.sendMessageToAllClients(name + "  вошёл в чат! ");
                            server.sendMessageToAllClients("Клиентов в чате = " + clients_count);
                        }
                        //Пересылка голосового сообщения
                        else if (clientMessage.contains("##VOICE##MESSAGE##")) {
                            DataLst.clear();
                            System.out.println("Дальше будет запись голосового сообщения");
                            inMessage.reset();
                            soundmessage = true;
                        }
                        else if(clientMessage.contains("##ACTIVATE##"))
                        {
                            try {
                                inMessage.reset();
                                outMessage.println("##VOICE##MESSAGE##\n");
                                outMessage.flush();

                                int k = 0;
                                    System.out.println(DataLst.size());
                                    for (int i=0;i<DataLst.size();i++)
                                    {
                                        Thread.sleep(10);
                                    outBStream.write(DataLst.get(i));
                                    System.out.println(DataLst.get(i).length);
                                    System.out.println(DataLst.get(i));
                                    outBStream.flush();
                                    k++;
                                     }

                                outBStream.flush();
                                    outMessage.flush();
                                outMessage.println("##END##OF##VOICE##MESSAGE\n");
                                outMessage.flush();
                                System.out.println("Отправлено"+k +" частей");
                            }
                            catch (IOException e)
                            {

                            }

                        }
                        //Пересылка норм сообщения
                        else {
                            server.sendMessageToAllClients(clientMessage);
                        }
                    }

            }
                else {

                    byte[] DataPart = new byte[10000];


                        inBStream.read(DataPart);
                        String part = new String(DataPart);


                        if (part.contains("##END##OF##VOICE##MESSAGE")) {
                            System.out.println(part);
                            System.out.println("Конец голосового сообщения!");
                            server.sendMessageToAllClients(clientName+" : "+"ПРИСЛАЛ ГОЛОСОВОЕ СООБЩЕНИЕ!");
                            soundmessage = false;
                        }
                        else {
                            DataLst.add(DataPart);
                            System.out.println(DataPart.length);
                            System.out.println(DataPart.toString());
                        }

                }

                // останавливаем выполнение потока на 100 мс
                Thread.sleep(100);
            }

        } catch (InterruptedException ex) {
            ex.printStackTrace();

        } catch (IOException e) {

        }
            finally
         {
            this.close();
        }
    }

    // отправляем сообщение
    public void sendMsg(String msg) {
        try {
            outMessage.println(msg);
            outMessage.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }



    // клиент выходит из чата
    public void close() {
        // удаляем клиента из списка
        server.removeClient(this);
        clients_count--;
        server.sendMessageToAllClients("Клиентов в чате = " + clients_count);
    }

    private byte[] add(byte[] data,String part,int filled)
    {
        byte[] partbyte = part.getBytes();
        for(int i=filled;i<filled+partbyte.length;i++){
            data[i]=partbyte[i-filled];
            }

        return  data;
    }


}
