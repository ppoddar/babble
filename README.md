# babble

babble is library for request-response communication stack:
 1. **Asynchronous**: client receives response for a request at a later time via callback   
 2. **Non-Blocking**: nothing waits on network i/o
 3. **Embeddable**: embed in existing program and the functions can be called over HTTP from `node.js` applications 
 4. **Multi-Protocol**: HTTP, JSON-RPC protocol included. easy to roll out your own procol
 5. **lightweight**: just uses JDK 7 or 8. No dependency (OK, almost. needs `slf4j` for logging)
 
