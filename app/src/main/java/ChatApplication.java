package com.example.easychat;

import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.FirebaseUtil;

public class ChatApplication extends Application implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityStarted(@NonNull android.app.Activity activity) {
        // Esta lógica foi removida para permitir o controle manual do status
    }

    @Override
    public void onActivityStopped(@NonNull android.app.Activity activity) {
        // Esta lógica foi removida para permitir o controle manual do status
    }

    // Métodos não utilizados
    @Override
    public void onActivityCreated(@NonNull android.app.Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override
    public void onActivityResumed(@NonNull android.app.Activity activity) {}
    @Override
    public void onActivityPaused(@NonNull android.app.Activity activity) {}
    @Override
    public void onActivitySaveInstanceState(@NonNull android.app.Activity activity, @NonNull Bundle outState) {}
    @Override
    public void onActivityDestroyed(@NonNull android.app.Activity activity) {}
}