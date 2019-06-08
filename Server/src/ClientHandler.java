import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;


// реализуем интерфейс Runnable, который позволяет работать с потоками
public class ClientHandler implements Runnable {
    private static final int PORT = 3443;
    //Счетчик голосовых сообщений
    //private static volatile int counter = 0;
    // количество клиента в чате, статичное поле
    private static int clients_count = 0;
    //Массив с именами всех пользователей
    private static ArrayList<String> clients = new ArrayList<>();
    // экземпляр нашего сервера
    private Server server;
    // исходящее сообщение
    private PrintWriter outMessage;
    private BufferedOutputStream outBStream;
    // входящее собщение
    private Scanner inMessage;
    private BufferedInputStream inBStream;
    // клиентский сокет
    private Socket clientSocket = null;
    //Имя данного пользователя
    private String clientName;
    //Загружается ли звуковое сообщение
    private boolean soundmessage;
    //Массив для звукового сообщения
    private volatile ArrayList<byte[]> DataLst;

    // конструктор, который принимает клиентский сокет и сервер
    public ClientHandler(Socket socket, Server server) {
        try {
            //Инициализация массива и увеличение кол-ва пользователей
            DataLst = new ArrayList<>();
            soundmessage = false;

            this.server = server;
            this.clientSocket = socket;
            //Получаем потоки
            this.outMessage = new PrintWriter(socket.getOutputStream());
            this.inMessage = new Scanner(socket.getInputStream());
            outBStream = new BufferedOutputStream(socket.getOutputStream());
            inBStream = new BufferedInputStream(socket.getInputStream());

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
                    //Если не качаем звук
                    if (inMessage.hasNext()) {
                        String clientMessage = inMessage.nextLine();
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
                        //Оповещение о новом клиенте, Высылаем ему имена всех пользователей в чате и счетчик голосовых
                        else if (clientMessage.contains("#?#Nick#?#")) {

                            String[] m = clientMessage.split("[ ]");
                            String name = m[0];
                            clientName = name;
                            if (clients.indexOf(name) != -1) {
                                outMessage.println("##INVALID##NAME##");
                                outMessage.flush();
                                try {
                                    Thread.sleep(100);
                                    clientSocket.close();
                                }
                                catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                catch (IOException e) {
                                    e.printStackTrace();
                                }

                            } else{
                                clients_count++;
                                for (int i = 0; i < clients.size(); i++) {
                                    sendMsg(clients.get(i) + "  #?#Nick#?#");
                                }
                            sendMsg("##VOICES## " + server.counter);
                                sendMsg("##FILES## "+server.file_counter);
                            server.sendMessageToAllClients(clientMessage);
                            clients.add(name);
                            server.sendMessageToAllClients(name + "  вошёл в чат! ");
                            server.sendMessageToAllClients("Клиентов в чате = " + clients_count);
                        }
                        }
                        //Начало получения голосового сообщения
                        else if (clientMessage.contains("##VOICE##MESSAGE##")) {
                            DataLst.clear();
                            System.out.println("Дальше будет запись голосового сообщения");
                            inMessage.reset();
                            soundmessage = true;
                        }

                        else if(clientMessage.contains("##NEW##FILE##"))
                        {
                            String[] mas = clientMessage.split("[ ]");
                            int length = Integer.valueOf(mas[1]);
                            String name = mas[2];
                            byte[] Data = new byte[length];
                            Thread.sleep(100);
                            inBStream.read(Data);

                            FileOutputStream fos = new FileOutputStream(new File(name));
                                fos.write(Data);
                                fos.flush();

                            fos.close();
                            server.file_counter++;
                            server.sendMessageToAllClients(clientName  + " ##NEW##FILE##");




                        }
                        //Запрос на загрузку записи
                        else if (clientMessage.contains("##ACTIVATE##")) {
                            try {
                                //Узнаем какую запись надо загрузить
                                String[] mas = clientMessage.split("[ ]");
                                int number = Integer.valueOf(mas[1]);
                                DataLst.clear();
                                //Загружаем запись из файла
                                FileInputStream fin = new FileInputStream("Sound" + number + ".txt");
                                while (fin.available() > 0) {
                                    byte[] DataPart = new byte[10000];
                                    fin.read(DataPart);
                                    DataLst.add(DataPart);
                                }
                                fin.close();
                                //Отсылаем
                                sendMsg("##VOICE##MESSAGE##\n");
                                int k = 0;
                                System.out.println(DataLst.size());
                                for (int i = 0; i < DataLst.size(); i++) {
                                    Thread.sleep(20);
                                    outBStream.write(DataLst.get(i));
                                    System.out.println(DataLst.get(i).length);
                                    System.out.println(DataLst.get(i));
                                    outBStream.flush();
                                    k++;
                                }
                                outBStream.flush();
                                Thread.sleep(10);
                                outMessage.println("\n");
                                outMessage.flush();
                                outMessage.println("##END##OF##VOICE##MESSAGE\n");
                                outMessage.flush();
                                System.out.println("Отправлено" + k + " частей");
                            } catch (IOException e) {

                            }


                        }

                        else if(clientMessage.contains("##DOWNLOAD##FILE##"))
                        {
                            try {
                                //Узнаем какую запись надо загрузить
                                String[] mas = clientMessage.split("[ ]");
                                int number = Integer.valueOf(mas[1]);
                                //Загружаем запись из файла
                                FileInputStream fin = new FileInputStream("FILE" + number + ".txt");
                               int length = fin.available();
                               byte[] File_data = new byte[length];
                               fin.read(File_data);
                                fin.close();
                                //Отсылаем

                                sendMsg("##FILE## ");
                                outBStream.write(File_data);
                                outBStream.flush();
                                Thread.sleep(10);
                                outMessage.println("\n");
                                outMessage.flush();
                            } catch (IOException e) {

                            }


                        }
                        //рассылка нормальных сообщений
                        else {
                            server.sendMessageToAllClients(clientMessage);
                        }
                    }

                }
                //Если качаем аудио
                else {

                    byte[] DataPart = new byte[10000];


                    inBStream.read(DataPart);
                    String part = new String(DataPart);

                    //Если закончили качать аудио, то записываем в файл.
                    if (part.contains("##END##OF##VOICE##MESSAGE")) {
                        System.out.println(part);
                        System.out.println("Конец голосового сообщения!");
                        //Загружаем запись в файл
                        FileOutputStream fos = new FileOutputStream(new File("Sound" + server.counter + ".txt"));
                        for (int i = 0; i < DataLst.size(); i++) {

                            fos.write(DataLst.get(i));
                            fos.flush();
                        }
                        fos.close();
                        server.counter++;
                        //отправляем сообщение о том, что есть новое голосовое сообщение
                        server.sendMessageToAllClients(clientName + " " + server.counter + " " + "##NEW##VOICE##");
                        soundmessage = false;
                        DataLst.clear();

                    } else {
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
            e.printStackTrace();

        } finally {
            this.close();
        }
    }

    // отправляем сообщение
    public void sendMsg(String msg) {
        try {
            outMessage.println(msg);
            outMessage.flush();
            System.out.println(msg);
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

}
