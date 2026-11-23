package com.sudoflame.app

import android.app.Application
import com.sudoflame.app.billing.BillingManager
import com.sudoflame.app.data.FriendRepository
import com.sudoflame.app.data.ProgressRepository
import com.sudoflame.app.notify.ReminderWorker

class SudoFlameApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ProgressRepository.init(this)
        FriendRepository.init(this)
        BillingManager.init(this)
        ReminderWorker.createChannel(this)
        ReminderWorker.schedule(this)
    }
}
