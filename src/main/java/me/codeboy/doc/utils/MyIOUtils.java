package me.codeboy.doc.utils;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.util.NamedThreadFactory;

import java.io.*;
import java.util.concurrent.*;

/**
 * @author gandazhi
 */
public class MyIOUtils {

    private static final int CORE_POOL_SIZE = 5;
    private static final int MAXIMUM_POOL_SIZE = 10;
    private static final int KEEP_ALIVE_TIME = 10;
    private static final String THREAD_NAME = "上传图片";
    private static ExecutorService saveExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
            KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingDeque<>(1000), new NamedThreadFactory(THREAD_NAME));

    public static void uploadFile(InputStream inputStream, String savePath) throws Exception {
        FileInputStream in = (FileInputStream) inputStream;
        ByteArrayOutputStream byteArray = null;
        try {
            byteArray = new ByteArrayOutputStream();
            IOUtils.copy(in, byteArray);
            byte[] memeryByte = byteArray.toByteArray();
            saveExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(new File(savePath));
                        IOUtils.write(memeryByte, out);
                    } catch (Exception e) {
                    } finally {
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            });

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {

                }
            }

            if (byteArray != null) {
                try {
                    byteArray.close();
                } catch (IOException e) {
                }
            }

        }
    }

}
