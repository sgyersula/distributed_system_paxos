package kvpaxos;
import java.io.Serializable;

/**
 * Please fill in the data structure you use to represent the request message for each RMI call.
 * Hint: Make it more generic such that you can use it for each RMI call.
 * Hint: Easier to make each variable public
 */
public class Request implements Serializable {
    static final long serialVersionUID=11L;
    // Your data here
    String Op_name;
    String key;
    Integer value;
    Integer client_id;
    Integer req_id;

    // Your constructor and methods here
    // for Put request
    public Request(String Op_name, String key, Integer value, Integer client_id, Integer req_id) {
        this.Op_name = Op_name;
        this.key = key;
        this.value = value;
        this.client_id = client_id;
        this.req_id = req_id;
    }
    // for Get request
    public Request(String Op_name, String key, Integer client_id, Integer req_id){
        this.Op_name = Op_name;
        this.key = key;
        this.value = -1;
        this.client_id = client_id;
        this.req_id = req_id;
    }

}
