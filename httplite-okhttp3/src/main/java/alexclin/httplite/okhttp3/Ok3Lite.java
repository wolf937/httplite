package alexclin.httplite.okhttp3;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import alexclin.httplite.Handle;
import alexclin.httplite.HttpLiteBuilder;
import alexclin.httplite.LiteClient;
import alexclin.httplite.Request;
import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.exception.IllegalOperationException;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.MediaType;
import alexclin.httplite.listener.Response;
import alexclin.httplite.util.ClientSettings;
import alexclin.httplite.util.Util;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.CookieJar;
import okhttp3.Dispatcher;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

/**
 * Ok3Lite
 *
 * @author alexclin 16/2/16 20:15
 */
public class Ok3Lite implements LiteClient {
    private final OkHttpClient mClient;
    private final Ok3Factory mFactory;

    private Ok3Lite(OkHttpClient client){
        mClient = client;
        mFactory = new Ok3Factory();
    }

    @Override
    public Response execute(Request request) throws Exception {
        Call call = mClient.newCall(makeRequest(request));
        request.handle().setHandle(new CallHandle(call));
        return new OkResponse(call.execute(),request);
    }

    @Override
    public void enqueue(final Request request,final Callback<Response> callback) {
        okhttp3.Callback okCallback = new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Exception throwable;
                if("Canceled".equals(e.getMessage())){
                    throwable = new CanceledException(e);
                }else{
                    throwable = e;
                }
                callback.onFailed(request,throwable);
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                callback.onSuccess(request,response.headers().toMultimap(),new OkResponse(response,request));
            }
        };
        Call call = mClient.newCall(makeRequest(request));
        request.handle().setHandle(new CallHandle(call));
        call.enqueue(okCallback);
    }

    @Override
    public void cancel(Object tag) {
        List<Call> list = mClient.dispatcher().runningCalls();
        for(Call call:list){
            if(Util.equal(tag,call.request().tag())){
                call.cancel();
            }
        }
        list = mClient.dispatcher().queuedCalls();
        for(Call call:list){
            if(Util.equal(tag,call.request().tag())){
                call.cancel();
            }
        }
    }

    @Override
    public void cancelAll() {
        mClient.dispatcher().cancelAll();
    }

    @Override
    public void shutDown() {
        cancelAll();
        mClient.dispatcher().executorService().shutdown();
    }

    @Override
    public MediaType mediaType(String mediaType) {
        return OkMediaType.create(mediaType);
    }

    private okhttp3.Request makeRequest(Request real){
        okhttp3.Request.Builder rb = new okhttp3.Request.Builder().url(real.getUrl()).tag(real.getTag());
        Headers okHeader = createHeader(real.getHeaders());
        if(okHeader!=null) rb.headers(okHeader);
        alexclin.httplite.RequestBody liteBody = real.getRequestBody();
        RequestBody requestBody = null;
        if(liteBody!=null){
            requestBody = mFactory.convertBody(liteBody,real.getWrapListener());
        }
        switch (real.getMethod()){
            case GET:
                rb = rb.get();
                break;
            case POST:
                rb = rb.post(requestBody);
                break;
            case PUT:
                rb = rb.put(requestBody);
                break;
            case PATCH:
                rb = rb.patch(requestBody);
                break;
            case HEAD:
                rb = rb.head();
                break;
            case DELETE:
                if(real.getRequestBody()==null){
                    rb = rb.delete();
                }else{
                    rb = rb.delete(requestBody);
                }
                break;
        }
        if(real.getCacheExpiredTime()>0){
            rb.cacheControl(new CacheControl.Builder().maxAge(real.getCacheExpiredTime(), TimeUnit.SECONDS).build());
        }else if(real.getCacheExpiredTime()== alexclin.httplite.Request.FORCE_CACHE){
            rb.cacheControl(CacheControl.FORCE_CACHE);
        }else if(real.getCacheExpiredTime()== alexclin.httplite.Request.NO_CACHE){
            rb.cacheControl(CacheControl.FORCE_NETWORK);
        }
        return rb.build();
    }

    static Headers createHeader(Map<String, List<String>> headers){
        if(headers!=null&&!headers.isEmpty()){
            Headers.Builder hb = new Headers.Builder();
            for(String key:headers.keySet()){
                List<String> values = headers.get(key);
                for(String value:values){
                    hb.add(key,value);
                }
            }
            return hb.build();
        }
        return null;
    }

    private static class CallHandle implements Handle{
        private Call call;

        private CallHandle(Call call) {
            this.call = call;
        }

        @Override
        public void cancel() {
            call.cancel();
        }

        @Override
        public boolean isCanceled() {
            return call.isCanceled();
        }

        @Override
        public boolean isExecuted() {
            return call.isExecuted();
        }

        @Override
        public void setHandle(Handle handle) {
            throw new IllegalOperationException("not support method");
        }
    }

    public static final class Builder extends HttpLiteBuilder{
        private OkHttpClient client;
        private CookieJar cookieJar;

        public Builder(OkHttpClient client) {
            this.client = client;
        }

        public Builder() {
        }

        public Builder setCookieJar(CookieJar cookieJar) {
            this.cookieJar = cookieJar;
            return this;
        }

        public Builder setCookieStore(CookieStore cookieStore){
            if(cookieStore!=null)
                this.cookieJar = new CookieJarImpl(new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL));
            return this;
        }

        public Builder setCookieStore(CookieStore cookieStore, CookiePolicy policy){
            if(cookieStore!=null)
                this.cookieJar = new CookieJarImpl(new CookieManager(cookieStore, policy));
            return this;
        }

        @Override
        protected LiteClient initClient(ClientSettings settings) {
            OkHttpClient.Builder builder = client==null?new OkHttpClient.Builder():client.newBuilder();
            builder.followSslRedirects(settings.isFollowSslRedirects())
                    .followRedirects(settings.isFollowRedirects());
            if(settings.getSocketFactory() != null) builder.socketFactory(settings.getSocketFactory());
            if(settings.getSslSocketFactory() != null) builder.sslSocketFactory(settings.getSslSocketFactory());
            if(settings.getHostnameVerifier() != null) builder.hostnameVerifier(settings.getHostnameVerifier());
            if(settings.getProxySelector() != null) builder.proxySelector(settings.getProxySelector());
            if(settings.getProxy() != null) builder.proxy(settings.getProxy());
            if(settings.getRequestExecutor()!=null) builder.dispatcher(new Dispatcher(settings.getRequestExecutor()));
            builder.retryOnConnectionFailure(settings.getMaxRetryCount() > 0);
            builder.connectTimeout(settings.getConnectTimeout(), TimeUnit.MILLISECONDS);
            builder.readTimeout(settings.getReadTimeout(), TimeUnit.MILLISECONDS);
            builder.writeTimeout(settings.getWriteTimeout(), TimeUnit.MILLISECONDS);
            if(cookieJar!=null)
                builder.cookieJar(cookieJar);
            if (settings.getCacheDir() != null) {
                builder.cache(new Cache(settings.getCacheDir(), settings.getCacheMaxSize()));
            }
            client = builder.build();
            return new Ok3Lite(client);
        }
    }
}
