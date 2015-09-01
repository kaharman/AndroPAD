package id.my.kaharman.andropad;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpConnection {

    private final static int TIMEOUT_CONNECTION = 5000;     //ms
    private final static int TIMEOUT_READ = 20000;          //ms
    
    public HttpConnection() {
    }
    
    public String downloadUrl(String myUrl) throws IOException {

        InputStream is = null;
        BufferedReader reader = null;
        StringBuilder stringBuilder;

        try {
            URL url = new URL(myUrl);
            Log.d("downloadUrl", myUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(TIMEOUT_READ);
            conn.setConnectTimeout(TIMEOUT_CONNECTION);
            conn.setRequestMethod("GET");
            conn.connect();
            int response = conn.getResponseCode();
            Log.d("downloadUrl", "The response is: " + response);
            is = conn.getInputStream();

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            stringBuilder = new StringBuilder();

            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            return stringBuilder.toString();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException ioe)
                {
                    ioe.printStackTrace();
                }
            }
        }
    }
}
