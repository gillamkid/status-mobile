package im.status.ethereum.module;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import statusgo.Statusgo;

public class StatusPackage implements ReactPackage {

    private boolean rootedDevice;

    public static String getImageTLSCert() {
        return Statusgo.imageServerTLSCert();
    }

    public StatusPackage(boolean rootedDevice) {
        this.rootedDevice = rootedDevice;
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();

        modules.add(new StatusModule(reactContext, this.rootedDevice));
        modules.add(new AccountManager(reactContext));
        modules.add(new EncryptionUtils(reactContext));
        modules.add(new DatabaseManager(reactContext));
        modules.add(new UIHelper(reactContext));
        modules.add(new LogManager(reactContext));
        modules.add(new Utils(reactContext));
        modules.add(new RNSelectableTextInputModule(reactContext));

        return modules;
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Arrays.<ViewManager>asList(
                new RNSelectableTextInputViewManager()
        );
    }
}
