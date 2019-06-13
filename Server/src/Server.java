import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server implements  Runnable{
    // порт, который будет прослушивать наш сервер
    static final int PORT = 3443;
    private ServerSocket serverSocket = null;
      //Счетчик голосовых сообщений
    public static volatile int sound_counter = 0;
    public static volatile ArrayList<String> files = new ArrayList<>();
    // список клиентов, которые будут подключаться к серверу
    private ArrayList<ClientHandler> clients = new ArrayList<ClientHandler>();


    public Server() throws  IOException{
        serverSocket = new ServerSocket(PORT);
    }
    public void run(){

        // серверный сокет
        Socket clientSocket = null;

        try {
            // создаём серверный сокет на определенном порту
            //serverSocket = new ServerSocket(PORT);
            System.out.println("Сервер запущен!");
            // запускаем бесконечный цикл
            while (true) {
                // таким образом ждём подключений от сервера
                clientSocket = serverSocket.accept();
                // создаём обработчик клиента, который подключился к серверу
                // this - это наш сервер
                ClientHandler client = new ClientHandler(clientSocket, this);
                clients.add(client);
                // каждое подключение клиента обрабатываем в новом потоке
                new Thread(client).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {

        }
    }

    // отправляем сообщение всем клиентам
    public void sendMessageToAllClients(String msg) {
        for (ClientHandler o : clients) {
            o.sendMsg(msg);
        }

    }


    // удаляем клиента из коллекции при выходе из чата
    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public void ShutDown(){
        sendMessageToAllClients("##SERVER##DOWN##");
        try {
            serverSocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }
}