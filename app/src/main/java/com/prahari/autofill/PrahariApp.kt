package com.prahari.autofill

import android.app.Application
import com.prahari.autofill.data.api.GeminiApi
import com.prahari.autofill.data.api.PrahariApi
import com.prahari.autofill.data.firebase.FirebaseManager
import com.prahari.autofill.data.secure.SecureStorage

class PrahariApp : Application() {
    lateinit var secureStorage: SecureStorage
        private set
    lateinit var prahariApi: PrahariApi
        private set
    lateinit var geminiApi: GeminiApi
        private set
    lateinit var firebase: FirebaseManager
        private set

    override fun onCreate() {
        super.onCreate()
        secureStorage = SecureStorage(this)
        prahariApi = PrahariApi(secureStorage)
        geminiApi = GeminiApi(secureStorage)
        firebase = FirebaseManager()
    }
}
