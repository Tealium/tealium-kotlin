package com.tealium.mobile;

import android.app.Application;

import com.tealium.collectdispatcher.CollectDispatcher;
// import com.tealium.core.consent.ConsentManager;
import com.tealium.core.Collectors;
import com.tealium.core.DispatcherFactory;
import com.tealium.core.Environment;
import com.tealium.core.Module;
import com.tealium.core.ModuleFactory;
import com.tealium.core.Tealium;
import com.tealium.core.TealiumConfig;
import com.tealium.tagmanagementdispatcher.TagManagementDispatcher;

import java.util.HashSet;
import java.util.Set;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class JavaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Set<DispatcherFactory> dispatchers = new HashSet<>();
        dispatchers.add(CollectDispatcher.Companion);
        dispatchers.add(TagManagementDispatcher.Companion);

        // Set<ModuleFactory> modules = new HashSet<>();
        // modules.add(ConsentManager.Companion);


        TealiumConfig config = new TealiumConfig(this,
                "services-james",
                "lib-mobile",
                Environment.QA,
                null,
                Collectors.getCore(),
                dispatchers);


        // Tealium instance = new Tealium("name", config, new Function1<Tealium, Unit>() {
        //     @Override
        //     public Unit invoke(Tealium tealium) {
        //         tealium.getModules().getModule(ConsentManager.class).setEnabled(false);
        //         return null;
        //     }
        // });
        // ConsentManager cm = instance.getModules().getModule(ConsentManager.class);
        //instance.getConsentManager().

    }
}
