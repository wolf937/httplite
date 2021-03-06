package alexclin.httplite.okhttp3;

import java.nio.charset.Charset;

import okhttp3.MediaType;

/**
 * OkMediaType
 *
 * @author alexclin 16/1/1 14:39
 */
class OkMediaType implements alexclin.httplite.listener.MediaType {
    private MediaType mediaType;

    OkMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    public String type() {
        return mediaType.type();
    }

    @Override
    public String subtype() {
        return mediaType.subtype();
    }

    @Override
    public Charset charset() {
        return mediaType.charset();
    }

    @Override
    public Charset charset(Charset defaultValue) {
        return mediaType.charset(defaultValue);
    }

    static alexclin.httplite.listener.MediaType create(String mediaType){
        MediaType type = MediaType.parse(mediaType);
        return type==null?null:new OkMediaType(type);
    }
}
