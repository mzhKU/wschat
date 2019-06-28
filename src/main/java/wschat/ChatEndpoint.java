package wschat;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/*
 * @ServerEndpoint: Container ensures availability of the class
 *                  as a WebSocket server listening to a specific
 *                  URI space.
 * @OnOpen:         Invoked by container when new WebSocket
 *                  connection is initiated.
 * @OnMessage:      Receives the information from the WebSocket
 *                  container when a message is sent to the
 *                  endpoint.
 * @OnClose:        Called by the container when WebSocket connection
 *                  closes.
 */

// URI defined relative to root of server container and must
// begin with forward slash
@ServerEndpoint(
        value = "/chat/{username}",
        decoders = MessageDecoder.class,
        encoders = MessageEncoder.class
)
public class ChatEndpoint {

    private Session session;
    private static Set<ChatEndpoint> chatEndpoints = new CopyOnWriteArraySet();
    private static Map<String, String> users = new HashMap();

    /*
     * When a new user logs in, @OnOpen is mapped to a data structure of
     * active users. Then a message is created and sent to all endpoints
     * using broadcast method.
     */
    @OnOpen
    public void onOpen(Session session,
                       @PathParam("username") String username) throws IOException {
        this.session = session;
        chatEndpoints.add(this);
        users.put(session.getId(), username);

        Message message = new Message();
        message.setFrom(username);
        message.setContent("Connected");
        broadcast(message);
    }

    /*
     * Whenever a message is sent.
     */
    @OnMessage
    public void onMessage(Session session, Message message) throws IOException {
        message.setFrom(users.get(session.getId()));
        broadcast(message);
    }

    /*
     * Called when a user logs of.
     */
    @OnClose
    public void onClose(Session session) throws IOException {
        chatEndpoints.remove(this);
        Message message = new Message();
        message.setFrom(users.get(session.getId()));
        message.setContent("Disconnected");
        broadcast(message);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // Do error handling here
    }

    private static void broadcast(Message message) {
        chatEndpoints.forEach(endpoint -> {
            synchronized (endpoint) {
                try {
                    endpoint.session.getBasicRemote()
                            .sendObject(message);
                } catch (IOException | EncodeException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}