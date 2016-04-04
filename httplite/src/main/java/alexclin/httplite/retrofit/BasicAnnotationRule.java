package alexclin.httplite.retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import alexclin.httplite.Request;
import alexclin.httplite.annotation.Body;
import alexclin.httplite.annotation.Form;
import alexclin.httplite.annotation.Forms;
import alexclin.httplite.annotation.GET;
import alexclin.httplite.annotation.HTTP;
import alexclin.httplite.annotation.IntoFile;
import alexclin.httplite.annotation.JsonField;
import alexclin.httplite.annotation.Multipart;
import alexclin.httplite.annotation.POST;
import alexclin.httplite.util.Util;

/**
 * BasicAnnotationRule
 *
 * @author alexclin 16/2/1 19:49
 */
public class BasicAnnotationRule implements AnnotationRule {
    private static class BodyType {
        public final Class<? extends Annotation> clazz;
        public final String type;
        public final boolean allowRepeat;

        private BodyType(Class<? extends Annotation> clazz, String type, boolean allowRepeat) {
            this.clazz = clazz;
            this.type = type;
            this.allowRepeat = allowRepeat;
        }

        @Override
        public String toString() {
            return "BodyType{" +
                    "clazz=" + clazz +
                    ", type='" + type + '\'' +
                    ", allowRepeat=" + allowRepeat +
                    '}';
        }

        @Override
        public int hashCode() {
            return clazz.getName().hashCode();
        }
    }
    public static final String BASE_BODY = "Body";
    public static final String FORM_BODY = "Form";
    public static final String MULTIPART_BODY = "Multipart";
    public static final String JSON_BODY = "JsonField";

    private List<BodyType> bodyAnnotationMap;

    public BasicAnnotationRule() {
        this.bodyAnnotationMap = new ArrayList<>();
        this.bodyAnnotationMap.add(new BodyType(Body.class,BASE_BODY,false));
        this.bodyAnnotationMap.add(new BodyType(Form.class,FORM_BODY,true));
        this.bodyAnnotationMap.add(new BodyType(Forms.class,FORM_BODY,true));
        this.bodyAnnotationMap.add(new BodyType(Multipart.class,MULTIPART_BODY,true));
        this.bodyAnnotationMap.add(new BodyType(JsonField.class,JSON_BODY,true));
    }

    public void registerBodyAnnotation(Class<? extends Annotation> clazz, String type, boolean allowRepeat){
        this.bodyAnnotationMap.add(new BodyType(clazz,type,allowRepeat));
    }

    @Override
    public void checkMethod(Method interfaceMethod,boolean isFileResult) throws RuntimeException {
        Annotation[][] methodParameterAnnotationArrays = interfaceMethod.getParameterAnnotations();
        alexclin.httplite.util.Method method = checkHttpMethod(interfaceMethod);

        boolean allowBody = Request.permitsRequestBody(method);
        boolean requireBody = Request.requiresRequestBody(method);
        boolean hasIntoFile = false;

        HashSet<BodyType> bodyTypeSet = new HashSet<>();
        String bodyTypeName = null;

        for (Annotation[] annotations : methodParameterAnnotationArrays) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof IntoFile) {
                    if (!isFileResult)
                        throw Util.methodError(interfaceMethod,"Method use @InfoFile must has result type File(Callback<File>/return-file/Observale<File>/...)");
                    hasIntoFile = true;
                } else {
                    BodyType type = bodyTypeFor(annotation);
                    if(type!=null){
                        if (!allowBody) {
                            throw Util.methodError(interfaceMethod,"HttpMethod:%s don't allow body param, but found:%s", method,annotation.getClass().getSimpleName());
                        }
                        if(bodyTypeSet.isEmpty()||bodyTypeName==null){
                            bodyTypeName = type.type;
                            bodyTypeSet.add(type);
                        }else if(bodyTypeSet.contains(type)){
                            if(!type.allowRepeat){
                                throw Util.methodError(interfaceMethod,String.format("HttpMethod:%s don't allow more than one %s param", method,type.type));
                            }
                        }else if(!type.type.equals(bodyTypeName)){
                            throw methodError(interfaceMethod,bodyTypeSet,type);
                        }
                    }
                }
            }
        }

        if(isFileResult&&!hasIntoFile){
            throw Util.methodError(interfaceMethod,"method with result type File(Callback<File>/return-file/Observable<File>/...) must has @IntoFile parameter");
        }

        if(requireBody && bodyTypeName == null){
            throw Util.methodError(interfaceMethod,"Http method:%s must has body parameter such as:@Form/Forms @Multipart @Body @JsonField or other body Annotation",method);
        }
    }

    private RuntimeException methodError(Method interfaceMethod, HashSet<BodyType> types,BodyType nBodyType) {
        String firstName = nBodyType.clazz.getSimpleName();
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for(BodyType type:types){
            if (isFirst){
                builder.append(type.clazz.getSimpleName());
                isFirst = false;
            }else {
                builder.append("/").append(type.clazz.getSimpleName());
            }
        }
        return Util.methodError(interfaceMethod,"You can not use @%s and @%s on on the same one method",firstName,builder.toString());
    }

    private alexclin.httplite.util.Method checkHttpMethod(Method interfaceMethod){
        alexclin.httplite.util.Method method = null;
        GET get = interfaceMethod.getAnnotation(GET.class);
        if(get!=null){
            method = alexclin.httplite.util.Method.GET;
        }
        if(method==null){
            POST post = interfaceMethod.getAnnotation(POST.class);
            if(post!=null) method = alexclin.httplite.util.Method.POST;
        }
        if(method==null){
            HTTP http = interfaceMethod.getAnnotation(HTTP.class);
            if(http!=null) method = http.method();
        }
        if (method==null) {
            String info = Util.printArray("You must set one http annotation on each method but there is:%s", interfaceMethod.getAnnotations());
            throw Util.methodError(interfaceMethod,info);
        }
        return method;
    }

    private BodyType bodyTypeFor(Annotation annotation){
        for(BodyType type:bodyAnnotationMap){
            if(Util.isSubType(annotation.getClass(),type.clazz)){
                return type;
            }
        }
        return null;
    }
}
