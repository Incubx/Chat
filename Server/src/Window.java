import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;


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
                    // Отключаем сервер и удаляем все загруженные голосовые сообщения и файлы.
                   server.ShutDown();
                for(int i=0;i<server.sound_counter;i++)
                {
                    File file = new File("SOUND"+i+".txt");
                    file.delete();
                }
                for(int i=0;i<server.files.size();i++)
                {
                    File file = new File(server.files.get(i));
                    file.delete();

                }
            }
        });



    }
}
