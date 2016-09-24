package babble.net.http;

class HttpHeader {
    String name;
    String value;
    
    public HttpHeader(String name, String value) {
        this.name  = name;
        this.value = value;
        
    }
    
    public HttpHeader(String line) {
        if (line == null) return;
        int idx = line.indexOf(':');
        if (idx == -1) return;
        name = line.substring(0,idx).trim();
        value = line.substring(idx+1).trim();
    }
    
    
    public String getName() {
        return name == null ? "" : name;
    }

    public String getValue() {
        return value;
    }
    
    public String toString() {
        return getName() + ':' + getValue();
    }


}