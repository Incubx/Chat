
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

// реализуем интерфейс Runnable, который позволяет работать с потоками
public class ClientHandler implements Runnable {
    // экземпляр нашего сервера
    private Server server;
    // исходящее сообщение
    private PrintWriter outMessage;
    // входящее собщение
    private Scanner inMessage;
    private static final int PORT = 3443;
    // клиентский сокет
    private Socket clientSocket = null;
    // количество клиента в чате, статичное поле
    private static int clients_count = 0;
    private static ArrayList<String> clients = new ArrayList<>();

    // конструктор, который принимает клиентский сокет и сервер
    public ClientHandler(Socket socket, Server server) {
        try {
            clients_count++;
            this.server = server;
            this.clientSocket = socket;
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
                    // если клиент отправляет данное сообщение, то цикл прерывается и
                    // клиент выходит из чата
                    if (clientMessage.contains("##session##end##")) {
                        String[] m =clientMessage.split("[ ]");
                        String name=m[0];
                        clients.remove(name);
                        server.sendMessageToAllClients(name + "  вышел из чата!");
                        server.sendMessageToAllClients(clientMessage);
                        break;
                    }
                    //Оповещение о новом клиенте
                    if(clientMessage.contains("#?#Nick#?#")) {
                        String[] m =clientMessage.split("[ ]");
                        String name=m[0];
                        for(int i =0;i<clients.size();i++)
                        {
                            sendMsg(clients.get(i)+"  #?#Nick#?#");
                        }
                        server.sendMessageToAllClients(clientMessage);
                        clients.add(name);
                        server.sendMessageToAllClients(name+ "  вошёл в чат! ");
                        server.sendMessageToAllClients("Клиентов в чате = " + clients_count);
                    }
                    else {
                        server.sendMessageToAllClients(clientMessage);
                    }
                }
                // останавливаем выполнение потока на 100 мс
                Thread.sleep(100);
            }
        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        finally {
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
}