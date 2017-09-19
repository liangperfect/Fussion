package src.com.westalgo.factorycamera.supernight;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;


public class FileSaver {

    public static final String DCIM = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();

    public static final String DIRECTORY = DCIM + "/Camera";
    //public static final String DIRECTORY = "/sdcard/demo/";


    public static File getOutputMediaFile() {
        File mediaStorageDir = new File(DIRECTORY);
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdir()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath()
                + File.separator + "IMG_" + timeStamp + ".JPG");
    }

    public static boolean saveFile(int width, int height, byte[] rgb, File file) {
        Log.e("FileSaver","westalgo:width->"+width+"  height->"+height+"  rgb.length->"+rgb.length);
        ByteBuffer byteBuffer = ByteBuffer.allocate(rgb.length);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        byteBuffer.put(rgb);
        byteBuffer.rewind();
        bitmap.copyPixelsFromBuffer(byteBuffer);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            bitmap.recycle();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
