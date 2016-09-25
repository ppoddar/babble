package babble.service;

import babble.net.http.HttpRequest;

public interface RequestRewriteRule {
    HttpRequest rewrite(HttpRequest request);
}
