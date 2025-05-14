package com.example.midespensa.presentation.receta

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.midespensa.data.model.Receta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RecetaViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val user: FirebaseUser? = auth.currentUser

    // States
    private val _recetasEnglish = mutableStateOf<List<Receta>>(emptyList())
    private val _recetasTranslated = mutableStateOf<List<Receta>>(emptyList())
    private val _showTranslated = mutableStateOf(false)
    private val _isLoading = mutableStateOf(false)

    // Exposed
    val recetas: State<List<Receta>> = derivedStateOf {
        if (_showTranslated.value) _recetasTranslated.value else _recetasEnglish.value
    }
    val isLoading: State<Boolean> = _isLoading
    val showTranslated: State<Boolean> = _showTranslated

    // Edamam keys
    private val appId = "b0e21ed4"
    private val appKey = "4d358f163ca0241eecfe44afcd28796a"

    // Recetas en favoritos
    private val db = FirebaseFirestore.getInstance()
    private val _favorites = mutableStateOf<List<Receta>>(emptyList())
    val favorites: State<List<Receta>> = _favorites

    // ML Kit translators
    private val enToEsTranslator: Translator by lazy {
        val opts = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.SPANISH)
            .build()
        Translation.getClient(opts)
    }

    private val esToEnTranslator: Translator by lazy {
        val opts = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.SPANISH)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        Translation.getClient(opts)
    }

    init {
        // Descargar ambos modelos al iniciar
        viewModelScope.launch {
            try {
                enToEsTranslator.downloadModelIfNeeded().await()
                esToEnTranslator.downloadModelIfNeeded().await()
                Log.d("RecetaVM", "Modelos ML Kit descargados")
            } catch (e: Exception) {
                Log.e("RecetaVM", "Error descargando modelos: ${e.message}")
            }
        }
    }

    fun toggleTranslation() {
        _showTranslated.value = !_showTranslated.value
    }

    fun buscarRecetas(query: String) {
        viewModelScope.launch {
            _isLoading.value = true

            // 0) Traducir consulta de ES a EN
            val queryEn = withContext(Dispatchers.Default) {
                traducirOffline(query, esToEnTranslator)
            }

            // 1) Fetch recetas en Idioma Original
            val ingles = withContext(Dispatchers.IO) {
                try {
                    val url =
                        "https://api.edamam.com/api/recipes/v2?type=public&q=$queryEn&app_id=$appId&app_key=$appKey&to=5"
                    val client = HttpClient(CIO) {
                        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                    }
                    val response: JsonObject = client.get(url) {
                        user?.uid?.let { header("Edamam-Account-User", it) }
                    }.body()

                    response["hits"]?.jsonArray?.mapNotNull { hit ->
                        val r = hit.jsonObject["recipe"]?.jsonObject ?: return@mapNotNull null
                        Receta(
                            label = r["label"]!!.jsonPrimitive.content,
                            image = r["image"]!!.jsonPrimitive.content,
                            url = r["url"]!!.jsonPrimitive.content,
                            yield = r["yield"]?.jsonPrimitive?.intOrNull ?: 1,
                            ingredientLines = r["ingredientLines"]!!.jsonArray.map {
                                it.jsonPrimitive.content
                            }
                        )
                    } ?: emptyList()
                } catch (e: Exception) {
                    Log.e("RecetaVM", "Error al buscar recetas: ${e.message}")
                    emptyList()
                }
            }

            _recetasEnglish.value = ingles

            // 2) Traducir en paralelo con ML Kit
            val traducidas = ingles.map { receta ->
                async(Dispatchers.Default) {
                    val labelEs = traducirOffline(receta.label, enToEsTranslator)
                    val ingredEs = receta.ingredientLines.map { traducirOffline(it, enToEsTranslator) }
                    receta.copy(label = labelEs, ingredientLines = ingredEs)
                }
            }.awaitAll()

            // 3) Delay para animación
            delay(200)

            // 4) Actualizar UI
            _recetasTranslated.value = traducidas
            _isLoading.value = false
        }
    }

    /**
     * Traduce texto offline usando ML Kit
     */
    private suspend fun traducirOffline(
        texto: String,
        translator: Translator
    ): String = suspendCancellableCoroutine { cont ->
        translator.translate(texto)
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    init {
        user?.uid?.let { uid ->
            db.collection("usuarios").document(uid)
                .addSnapshotListener { snap, _ ->
                    val favs = snap?.get("recetas") as? List<Map<String,Any>> ?: emptyList()
                    _favorites.value = favs.map { map ->
                        Receta(
                            label = map["label"] as String,
                            image = map["image"] as String,
                            url = map["url"] as String,
                            yield = (map["yield"] as Long).toInt(),
                            ingredientLines = map["ingredientLines"] as List<String>
                        )
                    }
                }
        }
    }


    fun toggleFavorite(receta: Receta) {
        val uid = user?.uid ?: return
        val actuales = _favorites.value.toMutableList()
        if (actuales.any { it.url == receta.url }) {
            actuales.removeAll { it.url == receta.url }
        } else {
            actuales.add(receta)
        }
        // Reconstruye la lista entera bajo el mismo campo
        val data = actuales.map { r ->
            mapOf(
                "label" to r.label,
                "image" to r.image,
                "url" to r.url,
                "yield" to r.yield,
                "ingredientLines" to r.ingredientLines
            )
        }
        db.collection("usuarios")
            .document(uid)
            .update("recetas", data)
            .addOnFailureListener { Log.e("RecetaVM", "Error fav: $it") }
    }

//    fun toggleFavorite(receta: Receta) {
//        val uid = user?.uid ?: return
//        val actuales = _favorites.value.toMutableList()
//        if (actuales.any { it.url == receta.url }) {
//            actuales.removeAll { it.url == receta.url }
//        } else {
//            actuales.add(receta)
//        }
//        // Actualiza Firestore
//        val data = actuales.map { r ->
//            mapOf(
//                "label" to r.label,
//                "image" to r.image,
//                "url" to r.url,
//                "yield" to r.yield,
//                "ingredientLines" to r.ingredientLines
//            )
//        }
//            db.collection("usuarios").document(uid)
//                .update("recetas", data)
//                .addOnFailureListener { Log.e("RecetaVM", "Error fav: $it") }
//    }

    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }
}

