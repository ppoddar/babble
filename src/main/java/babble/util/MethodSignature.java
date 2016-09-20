package babble.util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A signature for a Java method as a string. 
 * A signature can be constructed either from a compiled {@link Method method}
 * or from a string matching a regular expression.
 * <br>
 * The regular expression for a signature is
 * <pre>
 *    signature := return-type method-name '(' param-decl ')
 *    return-type := class-name
 *    method-name := java-identifier
 *    param-decl  := param-name':'param-type
 *    param-name  := java-identifier
 *    param-type  := class-name
 *    
 *    java-identifier is a string (one or more non-whitespace character)
 *    that is a valid identifier in Java language syntax
 *    
 *    class-name is also a string that is can be loaded by context
 *    class loader of current thread

 * </pre>  
 * 
 * @author pinaki poddar
 *
 */
public class MethodSignature {
    private Method _method;
    private String _signature;
    
    private static String REGEX_SIGNATURE = 
            "(?<ret>\\S+)\\s+(?<method>\\S+)\\((?<paramlist>\\S+)\\)";
    private static String REGEX_PARAMDECL = 
            "(?<paramtype>\\S+):(?<paramname>\\S+)";

    
    private static final String SPACE = " ";
    private static final String COMMA = ",";
    private static final String COLON = ":";
    private static final String OPEN_BRACE = "(";
    private static final String CLOSE_BRACE = ")";
    
    /**
     * Build a signature from a compiled method.
     * 
     * @param m
     */
    public MethodSignature(Method m) {
        _method    = m;
        _signature = createSignature(m).toString();
        
    }
    
    /**
     * Build a signature from a given string.
     * 
     * @param cls the class to which the method is declared
     * @param signature a stringified form of the method. The form is
     * same as result of {@link #toString() toString()}.
     */
    public MethodSignature(Class<?> cls, String signature) {
        this(parseAndValidateSignature(cls, signature));
    }
    
    public Method getMethod() {
        return _method;
    }

    
    /**
     * Gets the name of each argument in the order they have been declared.
     */
    public List<String> getArgumentNames() {
        throw new AbstractMethodError();
    }
    
    public int getParameterCount() {
        return _method.getParameterCount();
    }
    
    /**
     * Gets the name of the i-th input argument.
     * The argument names are available for if the owning method was 
     * compiled in Java 8 with <code>-parameter</code> option.
     * 
     * @param i a 0-based index
     * 
     * @return name of a parameter as declared in Java source code.
     * If parameter name is not available it appear as <code>arg&lt;n>
     * </code> where <code>n</code> is the 0-based index.
     */
    public String getArgumentName(int i) {
        return _method.getParameters()[i].getName();
    }
    
    /**
     * Gets the type of the i-th input argument.
     * 
     * @param i a 0-based index
     * 
     * @return type of i-th input argument.
     */
    public Class<?> getArgumentType(int i) {
        return _method.getParameterTypes()[i];
    }

    
    /**
     * Invoke this method on target with given input arguments.
     * 
     * @param target the target object to invoke this method
     * @param args input arguments 
     * @return return value of the method
     */
    public <R> R invoke(Object target, Object...args) {
        throw new AbstractMethodError();
    }
    
    /**
     * Gets the signature of this method.
     */
    public String toString() {
        return _signature;
    }
    
    /**
     * Parse the given signature string and validates the parsed method
     * against the given class.
     * 
     * @param cls a class where the parsed method must appear as a public
     * method and matching the input argument types and return type.
     * @param sig a method expressed as a string 
     */
    private static Method parseAndValidateSignature(Class<?> cls, 
            String sig) {
        Matcher matcher = Pattern.compile(REGEX_SIGNATURE).matcher(sig);
        
        if (matcher.matches()) {
            Class<?> returnType = resolveType(matcher.group("ret"));
            String methodName = matcher.group("method");
            System.err.println("return-type=" + returnType);
            System.err.println("method-name=" + methodName);
            
            // collect the parameters
            String paramlist = matcher.group("paramlist");
            System.err.println("paramlist=" + paramlist);
            String[] params =  paramlist.split(COMMA);
            Class<?>[] parameterTypes = new Class<?>[params.length];
            for (int i = 0; i < params.length; i++) {
                String param = params[i];
                Matcher m2 = Pattern.compile(REGEX_PARAMDECL).matcher(param);;
                if (m2.matches()) {
                    String paramName = m2.group("paramname");
                    parameterTypes[i] = resolveType(m2.group("paramtype"));
                    
                    System.err.println("\tname=" + paramName 
                            + " type=" + parameterTypes[i]);
                } else {
                    System.err.println("Param Declartion does not match input");
                    System.err.println("Signature:" + REGEX_PARAMDECL);
                    System.err.println("Input    :" + paramlist);
                }
            }
            // validate the method is declared in given class
            try {
                return cls.getMethod(methodName, parameterTypes);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            
        } else {
            System.err.println("Signature does not match input");
            System.err.println("Signature:" + REGEX_SIGNATURE);
            System.err.println("Input    :" + sig);
            
            throw new RuntimeException(sig + " not found in " + cls);
        }
    }
    
    private static Class<?> resolveType(String clsName) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            return Class.forName(clsName, false, cl);
        } catch (Exception ex) {
            throw new RuntimeException(clsName + " can not be resolved"
                    + " using classlaoder " + cl, ex);
        }
    }
    
    private static StringBuilder createSignature(Method m) {
        StringBuilder buf = new StringBuilder();
    
        buf.append(m.getReturnType().getName()).append(SPACE);
        buf.append(m.getName()).append(OPEN_BRACE);
        Parameter[] params = m.getParameters();
        Class<?>[] paramTypes = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            buf.append(paramTypes[i].getName()).append(COLON);
            buf.append(params[i].getName());
            if (i < params.length-1) buf.append(COMMA);
        }
        buf.append(CLOSE_BRACE);
        
        return buf;
    }
    
    /**
     * Gets 0-based position of the named parameter as declared in this
     * signature.
     * 
     * @param name name of a parameter
     * 
     * @return -1 if no declared parameter exists with the given name
     */
    public int getParameterIndex(String name) {
        if (name == null) {
            return -1;
        }
        Parameter[] params = _method.getParameters();
        for (int i = 0; i < params.length; i++) {
            if (name.equals(params[i].getName())) {
                return i;
            }
        }
        return -1;
    }
}
