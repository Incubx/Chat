import javax.swing.*;
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

        setBounds(0,0,400,100);
        try{
        Server server = new Server();
            setVisible(true);
        Thread server_stream = new Thread(server);
        server_stream.start();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                    // Отключаем сервер и удаляем все загруженные голосовые сообщения и файлы.
                   server.ShutDown();
                for(int i = 0; i< Server.sound_counter; i++)
                {
                    File file = new File("SOUND"+i+".txt");
                    file.delete();
                }
                for(int i = 0; i< Server.files.size(); i++)
                {
                    File file = new File(Server.files.get(i));
                    file.delete();

                }
            }
        });
        }
        catch (IOException e)
        {
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            JOptionPane.showMessageDialog(topFrame, "Не удалось создать сервер");
        }



    }
}
