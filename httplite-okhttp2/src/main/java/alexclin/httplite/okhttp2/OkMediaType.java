package alexclin.httplite.okhttp2;

import com.squareup.okhttp.MediaType;

import java.nio.charset.Charset;

/**
 * OkMediaType
 *
 * @author alexclin 16/1/1 14:39
 */
class OkMediaType implements alexclin.httplite.listener.MediaType {
    private MediaType mediaType;

    public OkMediaType(MediaType mediaType) {
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
}
