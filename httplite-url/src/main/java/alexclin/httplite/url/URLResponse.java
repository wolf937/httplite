package alexclin.httplite.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.ResponseBody;

/**
 * alexclin.httplite.urlconnection
 *
 * @author alexclin
 * @date 16/1/2 19:33
 */
public class URLResponse implements Response {

    private ResponseBody body;
    private Request request;

    private int code;
    private String message;
    private Map<String,List<String>> headers;

    public URLResponse(HttpURLConnection urlConnection, Request request) throws IOException{
        code = urlConnection.getResponseCode();
        message = urlConnection.getResponseMessage();
        headers = urlConnection.getHeaderFields();
        this.request = request;
        this.body = new URLResponseBody(urlConnection);
    }

    public URLResponse(int code,String message,Map<String,List<String>> headers,String mediaType,long length,InputStream inputStream,Request request){
        this.code = code;
        this.message = message;
        this.headers = headers;
        this.request = request;
        this.body = new URLResponseBody(mediaType,length,inputStream);
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public List<String> headers(String name) {
        return headers.get(name);
    }

    @Override
    public String header(String name) {
        List<String> list = headers(name);
        if(list!=null&&!list.isEmpty()){
            return list.get(0);
        }
        return null;
    }

    @Override
    public Map<String, List<String>> headers() {
        return headers;
    }

    @Override
    public ResponseBody body() {
        return body;
    }
}
