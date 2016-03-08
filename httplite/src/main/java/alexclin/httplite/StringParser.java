package alexclin.httplite;

import java.lang.reflect.Type;

import alexclin.httplite.listener.ResponseParser;

/**
 * StringParser
 *
 * @author alexclin  16/1/1 23:08
 */
public abstract class StringParser implements ResponseParser{

    @Override
    public final <T> T praseResponse(Response response, Type type) throws Exception{
        return praseResponse(HttpCallback.decodeResponseToString(response),type);
    }

    public abstract <T> T praseResponse(String content, Type type) throws Exception;
}
