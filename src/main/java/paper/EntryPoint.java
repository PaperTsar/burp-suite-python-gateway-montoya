package paper;

import burp.api.montoya.MontoyaApi;
import paper.contextmenu.ContextMenuHandler;

public class EntryPoint {
    public MontoyaApi api;
    public ContextMenuHandler contextMenuHandler;
    public AuxiliaryGatewayHandler auxiliaryGatewayHandler;

    public AuxiliaryGatewayHandler getAuxiliaryGatewayHandler() {
        return auxiliaryGatewayHandler;
    }

    public MontoyaApi getApi() {
        return api;
    }

    public ContextMenuHandler getContextMenuHandler() {
        return contextMenuHandler;
    }
}
