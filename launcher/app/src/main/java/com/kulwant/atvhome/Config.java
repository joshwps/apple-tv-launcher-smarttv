package com.kulwant.atvhome;

import android.content.Context;

public class Config {
    // Retorna a lista de pacotes da dock definida no config.xml
    public static String[] getDefaultDockApps(Context c) {
        return c.getResources().getStringArray(R.array.dock_packages);
    }

    // Retorna a lista de pacotes que devem ser ocultados
    public static String[] getHiddenApps(Context c) {
        return c.getResources().getStringArray(R.array.hidden_packages);
    }
}
