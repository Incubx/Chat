import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

public class Window extends JFrame {

    private JPanel mainPanel;

    public Window() {

        setContentPane(mainPanel);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Сервер");
        setVisible(true);
        setBounds(0,0,400,100);
        Server server = new Server();
        Thread server_stream = new Thread(server);
        server_stream.start();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    // отправляем служебное сообщение, которое является признаком того, что клиент вышел из чата
                   server.serverSocket.close();
                } catch (IOException exc) {
                    exc.printStackTrace();

                }
                for(int i=0;i<server.counter;i++)
                {
                    File file = new File("Sound"+i+".txt");
                    file.delete();
                }
            }
        });



    }
}
