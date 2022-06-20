import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class MainHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FileRequest) {                                               //Отправка файла клиенту
            new Thread(() -> {
                try {
                    FileRequest fr = (FileRequest) msg;
                    File file = new File("server_storage/" + fr.getFilename());
                    int bufSize = 1024 * 1024 * 1;
                    int partsCount = new Long(file.length() / bufSize).intValue();
                    if (file.length() % bufSize != 0) {
                        partsCount++;
                    }
                    if (Files.exists(Paths.get("server_storage/" + fr.getFilename()))) {
                        FileMessage fm = new FileMessage(Paths.get("server_storage/" + fr.getFilename()), -1, partsCount, new byte[bufSize]);
                        FileInputStream in = new FileInputStream("server_storage/" + fr.getFilename());
                        for (int i = 0; i < partsCount; i++) {
                            int readedBytes = in.read(fm.data);
                            fm.partNumber = i + 1;
                            if (readedBytes < bufSize) {
                                fm.data = Arrays.copyOfRange(fm.data, 0, readedBytes);
                            }
                            ctx.writeAndFlush(fm);
                            Thread.sleep(100);
                            System.out.println("Отправлена часть #" + (i + 1));
                        }
                        in.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        if(msg instanceof FileMessage) {                                        //Прием файла от клиента
            try {
                FileMessage fs = (FileMessage) msg;
                while (true) {
//               AbstractMessage input = (FileMessage) msg; // Network.readObject();
//                if(fs instanceof FileMessage) {
//                    FileMessage fm = (FileMessage) input;
                    boolean append = true;
                    if (fs.partsCount == 1) {
                        append = false;
                    }
                    System.out.println(fs.partNumber + " / " + fs.partsCount);
                    FileOutputStream fos = new FileOutputStream("server_storage/" + fs.filename, append);
                    fos.write(fs.data);
                    Thread.sleep(200);
                    fos.close();
                    if (fs.partNumber == fs.partsCount) {
                        break;
                    }
                }
                } catch(Exception e){
                    e.printStackTrace();
                }



//            FileMessage fs = (FileMessage) msg;
//            Files.write(Paths.get("server_storage/" + fs.getFilename()), fs.getData(), StandardOpenOption.CREATE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
