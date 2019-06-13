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
    private Scanner inMessage;
    private Socket clientSocket = null;
    private String clientName;


    // конструктор, который принимает клиентский сокет и сервер
    public ClientHandler(Socket socket, Server server) {
        try {
            this.server = server;
            this.clientSocket = socket;
            //Получаем потоки
            this.outMessage = new PrintWriter(socket.getOutputStream());
            this.inMessage = new Scanner(socket.getInputStream());

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
                            sendMsg("##VOICES## " + Server.sound_counter);
                            server.sendMessageToAllClients(clientMessage);
                            clients.add(name);
                            server.sendMessageToAllClients(name + "  вошёл в чат! ");
                            server.sendMessageToAllClients("Клиентов в чате = " + clients_count);
                        }
                        }
                        //Начало получения голосового сообщения
                        else if (clientMessage.contains("##VOICE##MESSAGE##")) {

                            byte[] bytes = new byte[8*1024];
                            int count;
                            String filename = "SOUND"+ Server.sound_counter +".txt";
                            FileOutputStream fos = new FileOutputStream(filename);
                             DataInputStream inBStream = new DataInputStream(clientSocket.getInputStream());
                            while ((count = inBStream.read(bytes)) > 0) {
                                System.out.println(bytes[0]);
                                if(count==1) break;

                                fos.write(bytes, 0, count);
                                fos.flush();
                                System.out.println(count);
                            }
                            System.out.println("Принятно!");
                            fos.close();


                            server.sendMessageToAllClients(clientName+" ##NEW##VOICE##");
                            Server.sound_counter++;
                        }
                        else if(clientMessage.contains("##SHARED##FILE##"))
                        {
                            String[] mas = clientMessage.split("[ ]");
                            String filename = mas[1];

                            byte[] bytes = new byte[16*1024];
                            FileOutputStream fout= new FileOutputStream(filename);
                            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                            int count;
                            while ((count = in.read(bytes)) > 0) {
                                if(count==1) break;
                                fout.write(bytes, 0, count);
                                System.out.println(count);
                            }
                            System.out.println("Файл принят!");

                            fout.close();

                            Server.files.add(filename);
                            server.sendMessageToAllClients(clientName  +" "+filename +" ##NEW##FILE##");




                        }
                        //Запрос на загрузку записи
                        else if (clientMessage.contains("##DOWNLOAD##VOICE##REQUEST##")) {
                            try {
                                //Узнаем какую запись надо загрузить
                                String[] mas = clientMessage.split("[ ]");
                                int number = Integer.valueOf(mas[1]);

                                outMessage.println("##REQUESTED##VOICE##");
                                outMessage.flush();
                                Thread.sleep(100);

                                DataOutputStream  outBStream = new DataOutputStream(clientSocket.getOutputStream());
                                byte[] data = new byte[8*1024];
                                int count;
                                FileInputStream fin = new FileInputStream("SOUND"+number+".txt");
                                while((count=fin.read(data))>0) {
                                    outBStream.write(data,0,count);
                                    System.out.println(count);
                                }
                                Thread.sleep(100);
                                System.out.println("Отправлено!");
                                outBStream.flush();
                                outBStream.write(-1);

                            } catch (IOException e) {

                            }


                        }

                        else if(clientMessage.contains("##DOWNLOAD##FILE##REQUEST##"))
                        {
                            try {
                                //Узнаем какую запись надо загрузить
                                String[] mas = clientMessage.split("[ ]");
                                String filename = mas[1];
                                //Передаем запись
                                outMessage.println("##REQUESTED##FILE## "+filename);
                                outMessage.flush();
                                Thread.sleep(100);


                                byte[] bytes = new byte[16 * 1024];
                                FileInputStream fin = new FileInputStream(filename);
                                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

                                int count;
                                while ((count = fin.read(bytes)) > 0) {
                                    out.write(bytes, 0, count);
                                    System.out.println(count);
                                }
                                out.flush();
                                Thread.sleep(100);
                                out.write(-1);
                                System.out.println("Файл отправлен");
                                fin.close();
                                //Отсылаем


                            } catch (IOException e) {

                            }


                        }
                        //рассылка нормальных сообщений
                        else {
                            server.sendMessageToAllClients(clientMessage);
                        }
                    }

                }


                // останавливаем выполнение потока на 100 мс
                Thread.sleep(100);


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
