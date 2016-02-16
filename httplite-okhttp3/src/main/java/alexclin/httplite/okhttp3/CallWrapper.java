package alexclin.httplite.okhttp3;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import alexclin.httplite.ResultCallback;
import okhttp3.Response;

/**
 * alexclin.httplite.okhttp.alexclin.httplite.okhttp3.wrapper
 *
 * @author alexclin
 * @date 16/1/2 17:12
 */
public class CallWrapper implements Call {
    private Call realCall;

    private ResultCallback callback;
    public CallWrapper(OkHttpClient client, Request originalRequest,ResultCallback callback) {
        this.realCall = client.newCall(originalRequest);
        this.callback = callback;
    }

    @Override
    public Request request() {
        return null;
    }

    @Override
    public Response execute() throws IOException {
        return realCall.execute();
    }

    @Override
    public void enqueue(Callback responseCallback) {
        realCall.enqueue(responseCallback);
    }

    @Override
    public void cancel() {
        realCall.cancel();
        callback.onCancel();
    }

    @Override
    public boolean isExecuted() {
        return realCall.isExecuted();
    }

    @Override
    public boolean isCanceled() {
        return realCall.isCanceled();
    }
}
