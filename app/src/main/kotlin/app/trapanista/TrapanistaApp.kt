package app.trapanista

import android.app.Application
import com.google.firebase.FirebaseApp

class TrapanistaApp : Application() {
    /** False when the app was built without google-services.json; voice mode is then unavailable. */
    var firebaseReady = false
        private set

    override fun onCreate() {
        super.onCreate()
        firebaseReady = FirebaseApp.getApps(this).isNotEmpty() || FirebaseApp.initializeApp(this) != null
        if (firebaseReady) installAppCheck()
    }
}
