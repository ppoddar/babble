package babble.net.http;

class HttpHeader {
    String name;
    String value;
    
    public HttpHeader(String name, String value) {
        this.name = name.trim();
        this.value = value.trim();
        
    }
    
    public HttpHeader(String line) {
        int idx = line.indexOf(':');
        name = line.substring(0,idx).trim();
        value = line.substring(idx+1).trim();
    }
    
    
    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
    
    public String toString() {
        return name + ':' + value;
    }


}