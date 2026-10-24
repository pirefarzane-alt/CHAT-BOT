package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ChatMessage
import com.example.model.HamedTone
import com.example.model.PlatformTheme
import com.example.model.Sender
import com.example.model.SentimentType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HamedFirestoreRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeMessages(userId: String): Flow<List<ChatMessage>> = flow {
        val path = "users/$userId/messages"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        val text = doc.getString("text") ?: return@mapNotNull null
                        val senderStr = doc.getString("sender") ?: "USER"
                        val sender = if (senderStr == "HAMED") Sender.HAMED else Sender.USER
                        val sentimentStr = doc.getString("sentiment") ?: "CALM"
                        val sentiment = runCatching { SentimentType.valueOf(sentimentStr) }.getOrDefault(SentimentType.CALM)
                        val sentimentScore = (doc.getDouble("sentimentScore") ?: 0.85).toFloat()
                        val sentimentAnalysis = doc.getString("sentimentAnalysis")
                        val isAudio = doc.getBoolean("isAudio") ?: false
                        val audioDuration = doc.getLong("audioDurationSec")?.toInt() ?: 0
                        val timestamp = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()

                        ChatMessage(
                            id = doc.id,
                            sender = sender,
                            text = text,
                            timestamp = timestamp,
                            sentiment = sentiment,
                            sentimentScore = sentimentScore,
                            sentimentAnalysis = sentimentAnalysis,
                            isAudio = isAudio,
                            audioDurationSec = audioDuration
                        )
                    }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveMessage(userId: String, message: ChatMessage) {
        val path = "users/$userId/messages/${message.id}"
        val docRef = db.collection("users").document(userId).collection("messages").document(message.id)
        try {
            val payload = hashMapOf<String, Any>(
                "userId" to userId,
                "sender" to message.sender.name,
                "text" to message.text,
                "sentiment" to message.sentiment.name,
                "sentimentScore" to message.sentimentScore.toDouble(),
                "isAudio" to message.isAudio,
                "audioDurationSec" to message.audioDurationSec,
                "createdAt" to FieldValue.serverTimestamp()
            )
            message.sentimentAnalysis?.let { payload["sentimentAnalysis"] = it }

            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun savePreferences(userId: String, tone: HamedTone, theme: PlatformTheme) {
        val path = "users/$userId/settings/preferences"
        val docRef = db.collection("users").document(userId).collection("settings").document("preferences")
        try {
            val payload = hashMapOf<String, Any>(
                "userId" to userId,
                "selectedTone" to tone.id,
                "platformTheme" to theme.id,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    suspend fun clearUserMessages(userId: String) {
        val path = "users/$userId/messages"
        try {
            val snapshot = db.collection("users").document(userId).collection("messages").get().await()
            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }
}
