package paper;

import py4j.GatewayServer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuxiliaryGatewayHandler {
    private final Map<String, GatewayServer> servers = new ConcurrentHashMap<>();
    private final EntryPoint entryPoint;
    private final String authtoken;
    private final String javaListenAddress;
    private final String pythonCallbackAddress;

    public AuxiliaryGatewayHandler(String authtoken, String javaListenAddress, String pythonCallbackAddress, EntryPoint entryPoint) {
        this.authtoken = authtoken;
        this.javaListenAddress = javaListenAddress;
        this.pythonCallbackAddress = pythonCallbackAddress;
        this.entryPoint = entryPoint;
    }

    public synchronized StartGatewayReturnValue startGateway() throws UnknownHostException {
        GatewayServer.GatewayServerBuilder builder = new GatewayServer.GatewayServerBuilder();
        builder.entryPoint(entryPoint);
        builder.javaAddress(InetAddress.getByName(javaListenAddress));
        builder.javaPort(0);
        if (!authtoken.isBlank()) {
            builder.authToken(authtoken);
        }
        builder.callbackClient(1024, // dummy port
                InetAddress.getByName(pythonCallbackAddress));

        GatewayServer gatewayServer = builder.build();
        gatewayServer.start();

        String identifier = UUID.randomUUID().toString();
        servers.put(identifier, gatewayServer);
        return new StartGatewayReturnValue(gatewayServer.getListeningPort(), identifier);
    }

    public synchronized void indicateShutdown(String identifier) {
        if (servers.containsKey(identifier)) {
            GatewayServer server = servers.get(identifier);
            // Calling server.shutdown() is unnecessary and will cause a socket exception on python side.
            // The server will be shut down if python calls it's gateway.shutdown() anyway, which it must!
            // Here we just have to remove references to avoid memory leaks.
            servers.remove(identifier);
        }
    }

    public record StartGatewayReturnValue(int javaPort, String identifier) {
    }

    public synchronized void shutdownAllGateways() {
        for (GatewayServer server : servers.values()) {
            try {
                server.shutdown();
            } catch (Exception e) {
            }
        }
        servers.clear();
    }
}
