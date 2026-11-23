package com.sudoflame.app.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class FriendStreak(
    val pairId: String,
    val friendUid: String,
    val friendName: String,
    val streak: Int,
    val iPlayedToday: Boolean,
    val friendPlayedToday: Boolean,
    val friendPersonalStreak: Int,
)

sealed interface FriendState {
    data object NotConfigured : FriendState
    data object Loading : FriendState
    data class Ready(val myCode: String, val friends: List<FriendStreak>) : FriendState
    data class Error(val message: String) : FriendState
}

/**
 * Firestore yapısı:
 *  users/{uid}            -> name, code, streak, lastPlayDay
 *  friendships/{a_b}      -> members[a,b], names{uid:name}, last{uid:day}, streak, lastBoth
 */
object FriendRepository {
    private const val TAG = "FriendRepo"
    private val _state = MutableStateFlow<FriendState>(FriendState.Loading)
    val state: StateFlow<FriendState> = _state.asStateFlow()

    private var enabled = false
    private var listener: ListenerRegistration? = null
    private var myCode: String = ""
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val uid: String? get() = if (enabled) auth.currentUser?.uid else null

    fun init(context: Context) {
        enabled = FirebaseApp.getApps(context).isNotEmpty()
        if (!enabled) _state.value = FriendState.NotConfigured
    }

    suspend fun connect() {
        if (!enabled) return
        try {
            if (auth.currentUser == null) auth.signInAnonymously().await()
            val id = auth.currentUser!!.uid
            val ref = db.collection("users").document(id)
            val snap = ref.get().await()
            myCode = snap.getString("code") ?: generateUniqueCode().also {
                ref.set(mapOf("code" to it), SetOptions.merge()).await()
            }
            pushProfile()
            listen(id)
        } catch (e: Exception) {
            Log.w(TAG, "connect", e)
            _state.value = FriendState.Error("Bağlantı kurulamadı. İnternetini kontrol et.")
        }
    }

    private suspend fun generateUniqueCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // karışan karakterler yok (0/O, 1/I)
        repeat(5) {
            val code = (1..6).map { alphabet.random() }.joinToString("")
            val exists = db.collection("users").whereEqualTo("code", code).limit(1).get().await()
            if (exists.isEmpty) return code
        }
        return (1..8).map { alphabet.random() }.joinToString("")
    }

    /** Profilimizi (isim, kişisel seri) güncelle. */
    suspend fun pushProfile() {
        val id = uid ?: return
        val p = ProgressRepository.current
        runCatching {
            db.collection("users").document(id).set(
                mapOf("name" to p.playerName, "streak" to p.streak, "lastPlayDay" to p.lastPlayDay),
                SetOptions.merge()
            ).await()
        }
    }

    private fun listen(id: String) {
        listener?.remove()
        listener = db.collection("friendships").whereArrayContains("members", id)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    _state.value = FriendState.Error("Arkadaş listesi alınamadı.")
                    return@addSnapshotListener
                }
                val today = Today.epochDay()
                val list = snap?.documents.orEmpty().mapNotNull { d ->
                    @Suppress("UNCHECKED_CAST")
                    val members = d.get("members") as? List<String> ?: return@mapNotNull null
                    val other = members.firstOrNull { it != id } ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    val names = d.get("names") as? Map<String, Any?> ?: emptyMap()
                    @Suppress("UNCHECKED_CAST")
                    val last = d.get("last") as? Map<String, Any?> ?: emptyMap()
                    @Suppress("UNCHECKED_CAST")
                    val streaks = d.get("personal") as? Map<String, Any?> ?: emptyMap()
                    val lastBoth = d.getLong("lastBoth") ?: -1
                    val raw = (d.getLong("streak") ?: 0).toInt()
                    FriendStreak(
                        pairId = d.id,
                        friendUid = other,
                        friendName = names[other] as? String ?: "Arkadaş",
                        streak = if (lastBoth >= today - 1) raw else 0,
                        iPlayedToday = (last[id] as? Number)?.toLong() == today,
                        friendPlayedToday = (last[other] as? Number)?.toLong() == today,
                        friendPersonalStreak = (streaks[other] as? Number)?.toInt() ?: 0,
                    )
                }.sortedByDescending { it.streak }
                _state.value = FriendState.Ready(myCode, list)
            }
    }

    /** @return hata mesajı ya da null (başarılı) */
    suspend fun addFriend(codeInput: String): String? {
        val id = uid ?: return "Arkadaş özelliği hazır değil."
        val code = codeInput.trim().uppercase()
        if (code.length < 6) return "Kod 6 karakter olmalı."
        if (code == myCode) return "Bu senin kendi kodun 🙂"
        return try {
            val res = db.collection("users").whereEqualTo("code", code).limit(1).get().await()
            val other = res.documents.firstOrNull() ?: return "Bu koda sahip oyuncu bulunamadı."
            val pairId = listOf(id, other.id).sorted().joinToString("_")
            val ref = db.collection("friendships").document(pairId)
            if (ref.get().await().exists()) return "Zaten arkadaşsınız!"
            val me = ProgressRepository.current
            ref.set(
                mapOf(
                    "members" to listOf(id, other.id),
                    "names" to mapOf(id to me.playerName, other.id to (other.getString("name") ?: "Arkadaş")),
                    "last" to mapOf(id to me.lastPlayDay, other.id to (other.getLong("lastPlayDay") ?: -1)),
                    "personal" to mapOf(id to me.streak, other.id to (other.getLong("streak") ?: 0)),
                    "streak" to 0,
                    "lastBoth" to -1,
                    "created" to FieldValue.serverTimestamp(),
                )
            ).await()
            null
        } catch (e: Exception) {
            Log.w(TAG, "addFriend", e)
            "Bir hata oluştu, tekrar dene."
        }
    }

    suspend fun removeFriend(pairId: String) {
        if (uid == null) return
        runCatching { db.collection("friendships").document(pairId).delete().await() }
    }

    /** Bir bulmaca kazanıldığında çağrılır: tüm ortak serileri günceller. */
    suspend fun onPlayedToday() {
        val id = uid ?: return
        val today = Today.epochDay()
        val p = ProgressRepository.current
        pushProfile()
        try {
            val docs = db.collection("friendships").whereArrayContains("members", id).get().await()
            for (d in docs.documents) {
                db.runTransaction<Unit> { tx ->
                    val s = tx.get(d.reference)
                    @Suppress("UNCHECKED_CAST")
                    val members = s.get("members") as? List<String> ?: return@runTransaction
                    val other = members.firstOrNull { it != id } ?: return@runTransaction
                    @Suppress("UNCHECKED_CAST")
                    val last = s.get("last") as? Map<String, Any?> ?: emptyMap()
                    val otherDay = (last[other] as? Number)?.toLong() ?: -1
                    val lastBoth = s.getLong("lastBoth") ?: -1
                    var streak = (s.getLong("streak") ?: 0).toInt()
                    val updates = mutableMapOf<String, Any>(
                        "last.$id" to today,
                        "names.$id" to p.playerName,
                        "personal.$id" to p.streak,
                    )
                    if (otherDay == today && lastBoth != today) {
                        streak = if (lastBoth == today - 1) streak + 1 else 1
                        updates["streak"] = streak
                        updates["lastBoth"] = today
                    }
                    tx.update(d.reference, updates)
                    Unit
                }.await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "onPlayedToday", e)
        }
    }
}
