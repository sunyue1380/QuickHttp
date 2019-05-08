package cn.schoolwow.quickhttp.util;

public class ValidateUtil {
    public static void checkNull(Object value){
        if(value!=null){
            throw new IllegalArgumentException();
        }
    }

    public static void checkNull(Object value,String message){
        if(value!=null){
            throw new IllegalArgumentException(message);
        }
    }

    public static void checkNotNull(Object value){
        if(value==null){
            throw new IllegalArgumentException();
        }
    }

    public static void checkNotNull(Object value,String message){
        if(value==null){
            throw new IllegalArgumentException(message);
        }
    }

    public static void checkNotEmpty(Object value){
        if(value==null||value.equals("")){
            throw new IllegalArgumentException();
        }
    }

    public static void checkNotEmpty(Object value,String message){
        if(value==null||value.equals("")){
            throw new IllegalArgumentException(message);
        }
    }

    public static void checkArgument(boolean expression){
        if(!expression){
            throw new IllegalArgumentException();
        }
    }

    public static void checkArgument(boolean expression,String message){
        if(!expression){
            throw new IllegalArgumentException(message);
        }
    }
}
