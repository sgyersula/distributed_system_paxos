package kvpaxos;

import java.io.Serializable;

/**
 * Please fill in the data structure you use to represent the response message for each RMI call.
 * Hint: Make it more generic such that you can use it for each RMI call.
 */
public class Response implements Serializable {
    static final long serialVersionUID=22L;
    // your data here
    boolean ok;
    Integer answer;
    // Your constructor and methods here
    public Response(boolean ok, Integer answer){
        this.ok = ok;
        this.answer = answer;
    }
}
