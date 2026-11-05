package app.aura.themer

import android.app.Application
import app.aura.themer.data.Fonts
import app.aura.themer.data.Repo

class AuraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Fonts.init(this)
        Repo.init(this)
    }
}
