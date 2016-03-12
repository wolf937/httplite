package alexclin.httplite;

import java.io.File;
import java.lang.reflect.Type;

import alexclin.httplite.listener.Callback;
import alexclin.httplite.util.Util;

/**
 * HttpCall
 *
 * @author alexclin at 16/1/26 22:12
 */
public class HttpCall implements Call{
    protected Request request;

    protected HttpCall(Request request) {
        this.request = request;
    }

    @SuppressWarnings("unchecked")
    public <T> Handle execute(Callback<T> callback){
        Type type = Util.type(Callback.class, callback);
        if(type==File.class){
            return download((Callback<File>)callback);
        }else{
            ResultCallback rcb = createHttpCalback(callback, type);
            return executeSelf(rcb);
        }
    }

    public Response executeSync() throws Exception{
        return executeSyncInner(null);
    }

    public <T> T executeSync(Clazz<T> clazz) throws Exception{
        ResultCallback<T> callback = createResultCallback(clazz);
        Response response = executeSyncInner(callback);
        return parseResult(response, callback);
    }

    @SuppressWarnings("unchecked")
    protected  <T> ResultCallback<T> createResultCallback(Clazz<T> clazz) {
        Type type = clazz.type();
        ResultCallback<T> callback;
        if(type==File.class) {
            callback = (ResultCallback<T>)(this.<File>createDownloadCallback(null));
        }else{
            callback = this.<T>createHttpCalback(null, type);
        }
        return callback;
    }

    @Override
    public DownloadHandle download(Callback<File> callback) {
        final DownloadCallback rcb = createDownloadCallback(callback);
        executeSelf(rcb);
        return rcb;
    }

    protected  <T> ResultCallback createHttpCalback(Callback<T> callback,Type type) {
        HttpLite lite = request.lite;
        ResultCallback<T> rcb = new HttpCallback<>(callback,this,type);
        if(lite.getRequestFilter()!=null) lite.getRequestFilter().onRequest(lite,request, type);
        return rcb;
    }

    protected <T> T parseResult(Response response, ResultCallback<T> callback) throws Exception{
        return callback.parseResponse(response);
    }

    protected MediaType mediaType(String mediaType){
        return request.lite.parse(mediaType);
    }

    protected DownloadCallback createDownloadCallback(Callback<File> callback) {
        DownloadCallback.DownloadParams params = request.getDownloadParams();
        if(params==null){
            throw new IllegalArgumentException("to execute Callback<File>, you must call intoFile() on Request before execute");
        }
        return new DownloadCallback(callback,this,params);
    }

    private Response executeSyncInner(ResultCallback callback) throws Exception{
        Runnable preWork = null;
        if(callback instanceof DownloadCallback){
            preWork = (DownloadCallback)callback;
        }
        HttpLite lite = request.lite;
        if(lite.getRequestFilter()!=null) lite.getRequestFilter().onRequest(lite,request,callback.resultType());
        if(preWork!=null) preWork.run();
        Response response = lite.getClient().executeSync(request);
        if(lite.getResponseFilter()!=null) lite.getResponseFilter().onResponse(lite,request, response);
        return response;
    }

    <T> Handle executeSelf(final ResultCallback<T> callback){
        HttpLite lite = request.lite;
        boolean isDownload = callback instanceof DownloadCallback;
        final Runnable preWork = isDownload?(DownloadCallback)callback:null;
        if(isDownload&&lite.getCustomDownloadExecutor()!=null){
            lite.getCustomDownloadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        callback.onResponse(executeSyncInner(callback));
                    } catch (Exception e) {
                        callback.onFailed(e);
                    }
                }
            });
            return (DownloadCallback)callback;
        }else{
            return lite.getClient().execute(request,callback,preWork);
        }
    }

    public static class Factory implements CallFactory{

        @Override
        public Call newCall(Request request) {
            return new HttpCall(request);
        }
    }
}
