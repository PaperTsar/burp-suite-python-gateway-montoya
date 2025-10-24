package paper;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Registration;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.logging.Logging;
import paper.contextmenu.ContextMenuHandler;
import py4j.ClientServer;
import py4j.GatewayServer;
import burp.api.montoya.ui.settings.SettingsPanelBuilder;
import burp.api.montoya.ui.settings.SettingsPanelPersistence;
import burp.api.montoya.ui.settings.SettingsPanelSetting;
import burp.api.montoya.ui.settings.SettingsPanelWithData;
import py4j.GatewayServer.GatewayServerBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Extension implements BurpExtension, ExtensionUnloadingHandler {
    private static final String EXTENSION_NAME = "A Python Bridge";
    private static final String KEY_JAVA_LISTEN_ADDRESS = "Java Listen Address";
    private static final String KEY_JAVA_LISTEN_PORT = "Java Listen Port";
    private static final String KEY_PYTHON_CALLBACK_ADDRESS = "Python Callback Address";
    private static final String KEY_PYTHON_CALLBACK_PORT = "Python Callback Port";
    private static final String KEY_AUTH_TOKEN = "Auth Token";
    private static final String KEY_READ_TIMEOUT = "Read Timeout";
    private static final String KEY_CONNECT_TIMEOUT = "Connect Timeout";
    private static final String KEY_CLIENT_SERVER = "Use Single Threaded Client Server";

    private GatewayServer gatewayServer = null;
    private ClientServer clientGatewayServer = null;
    private Registration settingsPanelRegistration = null;
    private SettingsPanelWithData settingsPanel = null;
    private MontoyaApi api = null;
    private EntryPoint entryPoint = null;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName(EXTENSION_NAME);
        api.extension().registerUnloadingHandler(this);

        Logging logging = api.logging();

        settingsPanel = SettingsPanelBuilder.settingsPanel()
                .withPersistence(SettingsPanelPersistence.USER_SETTINGS)
                .withTitle(EXTENSION_NAME)
                .withDescription("""
                        Connect to Burp with Py4j and get full access to the Montoya API.
                        
                        Note: reload the extension after changing the settings!
                        Warning: using singlethreaded implementation is buggy. Will always listen on 127.0.0.1!""")
                .withKeywords("Python", "Settings", "Gateway", "Monotya")
                .withPersistence(SettingsPanelPersistence.PROJECT_SETTINGS)
                .withSettings(
                        SettingsPanelSetting.stringSetting(KEY_JAVA_LISTEN_ADDRESS, GatewayServer.DEFAULT_ADDRESS),
                        SettingsPanelSetting.integerSetting(KEY_JAVA_LISTEN_PORT, GatewayServer.DEFAULT_PORT),
                        SettingsPanelSetting.stringSetting(KEY_PYTHON_CALLBACK_ADDRESS, "127.0.0.1"),
                        SettingsPanelSetting.integerSetting(KEY_PYTHON_CALLBACK_PORT, GatewayServer.DEFAULT_PYTHON_PORT),
                        SettingsPanelSetting.stringSetting(KEY_AUTH_TOKEN, ""),
                        SettingsPanelSetting.integerSetting(KEY_READ_TIMEOUT, GatewayServer.DEFAULT_READ_TIMEOUT),
                        SettingsPanelSetting.integerSetting(KEY_CONNECT_TIMEOUT, GatewayServer.DEFAULT_CONNECT_TIMEOUT),
                        SettingsPanelSetting.booleanSetting(KEY_CLIENT_SERVER, false)
                )
                .build();

        // Register the settings panel
        settingsPanelRegistration = api.userInterface().registerSettingsPanel(settingsPanel);

        entryPoint = new EntryPoint();
        entryPoint.api = api;
        entryPoint.contextMenuHandler = new ContextMenuHandler();
        entryPoint.auxiliaryGatewayHandler = new AuxiliaryGatewayHandler(
                settingsPanel.getString(KEY_AUTH_TOKEN),
                settingsPanel.getString(KEY_JAVA_LISTEN_ADDRESS),
                settingsPanel.getString(KEY_PYTHON_CALLBACK_ADDRESS),
                entryPoint);

        api.userInterface().registerContextMenuItemsProvider(entryPoint.contextMenuHandler);

        try {
            if (settingsPanel.getBoolean(KEY_CLIENT_SERVER)) {
                // use single threaded ClientServer implementation
                ClientServer.ClientServerBuilder builder = new ClientServer.ClientServerBuilder();
                builder.entryPoint(entryPoint);
                builder.javaAddress(InetAddress.getByName(settingsPanel.getString(KEY_JAVA_LISTEN_ADDRESS)));
                builder.javaPort(settingsPanel.getInteger(KEY_JAVA_LISTEN_PORT));


                if (!settingsPanel.getString(KEY_AUTH_TOKEN).isBlank()) {
                    builder.authToken(settingsPanel.getString(KEY_AUTH_TOKEN));
                }

                if (!settingsPanel.getString(KEY_PYTHON_CALLBACK_ADDRESS).isBlank()) {
                    builder.pythonAddress(InetAddress.getByName(settingsPanel.getString(KEY_PYTHON_CALLBACK_ADDRESS)));
                }

                if (!settingsPanel.getString(KEY_PYTHON_CALLBACK_PORT).isBlank()) {
                    builder.pythonPort(settingsPanel.getInteger(KEY_PYTHON_CALLBACK_PORT));
                }
                builder.autoStartJavaServer(false);
                clientGatewayServer = builder.build();
                clientGatewayServer.startServer(true);
                logging.logToOutput("Started ClientServer!");
            } else {
                // use traditional, multithreaded GatewayServer implementation
                GatewayServerBuilder builder = new GatewayServerBuilder();
                builder.entryPoint(entryPoint);
                builder.javaAddress(InetAddress.getByName(settingsPanel.getString(KEY_JAVA_LISTEN_ADDRESS)));
                builder.javaPort(settingsPanel.getInteger(KEY_JAVA_LISTEN_PORT));


                if (!settingsPanel.getString(KEY_AUTH_TOKEN).isBlank()) {
                    builder.authToken(settingsPanel.getString(KEY_AUTH_TOKEN));
                }

                if (!settingsPanel.getString(KEY_PYTHON_CALLBACK_ADDRESS).isBlank()) {
                    builder.callbackClient(settingsPanel.getInteger(KEY_PYTHON_CALLBACK_PORT),
                            InetAddress.getByName(settingsPanel.getString(KEY_PYTHON_CALLBACK_ADDRESS)));
                }

                gatewayServer = builder.build();
                gatewayServer.start();
                logging.logToOutput("Started GatewayServer!");
            }
        } catch (UnknownHostException e) {
            logging.logToOutput("Unable to start server!");
            logging.logToError(e.toString());
            return;
        }

        logging.logToOutput(String.format("""
                        Burp Python Gateway (Montoya) Loaded!
                            %s -> %s
                            %s -> %d
                            %s -> %s
                            %s -> %d
                            %s -> %s
                            %s -> %d
                            %s -> %d
                        
                        You can change these in Burp settings! Make sure to reload the extension.""",
                KEY_JAVA_LISTEN_ADDRESS, settingsPanel.getString(KEY_JAVA_LISTEN_ADDRESS),
                KEY_JAVA_LISTEN_PORT, settingsPanel.getInteger(KEY_JAVA_LISTEN_PORT),
                KEY_PYTHON_CALLBACK_ADDRESS, settingsPanel.getString(KEY_PYTHON_CALLBACK_ADDRESS),
                KEY_PYTHON_CALLBACK_PORT, settingsPanel.getInteger(KEY_PYTHON_CALLBACK_PORT),
                KEY_AUTH_TOKEN, settingsPanel.getString(KEY_AUTH_TOKEN),
                KEY_READ_TIMEOUT, settingsPanel.getInteger(KEY_READ_TIMEOUT),
                KEY_CONNECT_TIMEOUT, settingsPanel.getInteger(KEY_CONNECT_TIMEOUT)
        ));

        if (settingsPanel.getBoolean(KEY_CLIENT_SERVER)) {
            logging.logToOutput("\nUsing single-threaded ClientServer implementation. Connect in Python with: py4j.ClientServer");
            logging.logToOutput(String.format("""
                    
                    Warning: py4j is buggy, Java will always listen on 127.0.0.1
                    gateway = ClientServer(
                        java_parameters=JavaParameters(address="%s", port=%d),
                        python_parameters=PythonParameters(address="%s", port=%d))""",
                    settingsPanel.getString(KEY_JAVA_LISTEN_ADDRESS),
                    settingsPanel.getInteger(KEY_JAVA_LISTEN_PORT),
                    settingsPanel.getString(KEY_PYTHON_CALLBACK_ADDRESS),
                    settingsPanel.getInteger(KEY_PYTHON_CALLBACK_PORT)));
        } else {
            logging.logToOutput("\nUsing multithreaded GatewayServer implementation. Connect in Python with: py4j.JavaGateway");
            logging.logToOutput(String.format("""
                    
                    gateway = JavaGateway(
                        gateway_parameters=GatewayParameters(address="%s", port=%d),
                        callback_server_parameters=CallbackServerParameters(address="%s", port=%d))""",
                    settingsPanel.getString(KEY_JAVA_LISTEN_ADDRESS),
                    settingsPanel.getInteger(KEY_JAVA_LISTEN_PORT),
                    settingsPanel.getString(KEY_PYTHON_CALLBACK_ADDRESS),
                    settingsPanel.getInteger(KEY_PYTHON_CALLBACK_PORT)));
        }
    }

    @Override
    public void extensionUnloaded() {
        if (gatewayServer != null) {
            gatewayServer.shutdown();
        }

        if (clientGatewayServer != null) {
            clientGatewayServer.shutdown();
        }

        entryPoint.auxiliaryGatewayHandler.shutdownAllGateways();

        if (settingsPanelRegistration != null) {
            settingsPanelRegistration.deregister();
        }
    }
}