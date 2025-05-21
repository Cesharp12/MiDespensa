package com.example.midespensa.presentation.receta

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.midespensa.data.model.Receta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
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
    private val db = FirebaseFirestore.getInstance()
    val user: FirebaseUser? = auth.currentUser

    // Estados
    private val _recetasEnglish = mutableStateOf<List<Receta>>(emptyList())
    private val _recetasTranslated = mutableStateOf<List<Receta>>(emptyList())
    private val _showTranslated = mutableStateOf(false)
    private val _isLoading = mutableStateOf(false)
    private val _favorites = mutableStateOf<List<Receta>>(emptyList())

    // Expuestos
    val recetas: State<List<Receta>> = derivedStateOf { if (_showTranslated.value) _recetasTranslated.value else _recetasEnglish.value }
    val isLoading: State<Boolean> = _isLoading
    val showTranslated: State<Boolean> = _showTranslated

    // Favoritos
    private val _favoritesEnglish = mutableStateOf<List<Receta>>(emptyList())
    private val _favoritesTranslated = mutableStateOf<List<Receta>>(emptyList())
    private val _showFavorites = mutableStateOf(false)
    val favorites: State<List<Receta>> = derivedStateOf {
        if (_showTranslated.value) _favoritesTranslated.value else _favoritesEnglish.value
    }
    val showFavorites: State<Boolean> = _showFavorites

    // Edamam
    private val appId = "b0e21ed4"
    private val appKey = "4d358f163ca0241eecfe44afcd28796a"

    // Traductores ML Kit
    private val enToEsTranslator: Translator by lazy {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.SPANISH)
                .build()
        )
    }
    private val esToEnTranslator: Translator by lazy {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.SPANISH)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()
        )
    }

    init {
        // Descargar modelos ML Kit al iniciar
        viewModelScope.launch {
            try {
                enToEsTranslator.downloadModelIfNeeded().await()
                esToEnTranslator.downloadModelIfNeeded().await()
                Log.d("RecetaVM", "Modelos ML Kit descargados")
            } catch (e: Exception) {
                Log.e("RecetaVM", "Error descargando modelos: ${e.message}")
            }
        }

        // Escuchar favoritos en Firestore
        user?.uid?.let { uid ->
            db.collection("usuarios")
                .document(uid)
                .addSnapshotListener { snap, _ ->
                    // Extrae la lista cruda de mapas
                    val raw = snap
                        ?.get("recetas") as? List<Map<String,Any>>
                        ?: emptyList()

                    // Mapea a objetos Receta en INGLÉS
                    val english = raw.map { map ->
                        Receta(
                            label           = map["label"] as String,
                            image           = map["image"] as String,
                            url             = map["url"] as String,
                            yield           = (map["yield"] as Long).toInt(),
                            ingredientLines = map["ingredientLines"] as List<String>
                        )
                    }

                    // Actualiza versión inglesa
                    _favoritesEnglish.value = english

                    // Traducir OFFLINE en paralelo y rellenar la versión en ESPAÑOL
                    viewModelScope.launch(Dispatchers.Default) {
                        val translated = english.map { receta ->
                            val labelEs    = traducirOffline(receta.label, enToEsTranslator)
                            val ingredEs   = receta.ingredientLines.map { traducirOffline(it, enToEsTranslator) }
                            receta.copy(label = labelEs, ingredientLines = ingredEs)
                        }
                        _favoritesTranslated.value = translated
                    }
                }
        }
    }

    fun toggleTranslation() { _showTranslated.value = !_showTranslated.value }
    fun toggleShowFavorites() { _showFavorites.value = !_showFavorites.value }

    fun buscarRecetas(query: String) {

        _showFavorites.value = false
        viewModelScope.launch {
            _isLoading.value = true

            // Traducir consulta
            val queryEn = withContext(Dispatchers.Default) {
                traducirOffline(query, esToEnTranslator)
            }

            // Fetch Edamam
            val ingles = withContext(Dispatchers.IO) {
                runCatching {
                    val url = "https://api.edamam.com/api/recipes/v2?type=public&q=$queryEn&app_id=$appId&app_key=$appKey&to=5"
                    val client = HttpClient(CIO) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
                    val resp: JsonObject = client.get(url) { user?.uid?.let { header("Edamam-Account-User", it) } }.body()
                    resp["hits"]?.jsonArray?.mapNotNull { hit ->
                        hit.jsonObject["recipe"]?.jsonObject?.let { r ->
                            Receta(
                                label = r["label"]!!.jsonPrimitive.content,
                                image = r["image"]!!.jsonPrimitive.content,
                                url = r["url"]!!.jsonPrimitive.content,
                                yield = r["yield"]?.jsonPrimitive?.intOrNull ?: 1,
                                ingredientLines = r["ingredientLines"]!!.jsonArray.map { it.jsonPrimitive.content }
                            )
                        }
                    } ?: emptyList()
                }.getOrDefault(emptyList())
            }
            _recetasEnglish.value = ingles

            // Traducir resultados
            val traducidas = ingles.map { receta ->
                async(Dispatchers.Default) {
                    val lab = traducirOffline(receta.label, enToEsTranslator)
                    val ing = receta.ingredientLines.map { traducirOffline(it, enToEsTranslator) }
                    receta.copy(label = lab, ingredientLines = ing)
                }
            }.awaitAll()
            _recetasTranslated.value = traducidas

            delay(200)
            _isLoading.value = false
        }
    }

    private suspend fun traducirOffline(text: String, trans: Translator): String =
        suspendCancellableCoroutine { cont ->
            trans.translate(text)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    fun toggleFavorite(receta: Receta) {
        val uid = user?.uid ?: return

        // 1) Trabajamos sobre la lista ENGLISh que mantenemos localmente
        val actuales = _favoritesEnglish.value.toMutableList()
        if (actuales.any { it.url == receta.url }) {
            actuales.removeAll { it.url == receta.url }
        } else {
            actuales.add(receta)
        }

        // 2) Actualizamos inmediatamente el estado local EN & ES
        _favoritesEnglish.value = actuales

        viewModelScope.launch(Dispatchers.Default) {
            val translated = actuales.map { r ->
                val lblEs  = traducirOffline(r.label, enToEsTranslator)
                val ingsEs = r.ingredientLines.map { traducirOffline(it, enToEsTranslator) }
                r.copy(label = lblEs, ingredientLines = ingsEs)
            }
            _favoritesTranslated.value = translated
        }

        // 3) Persistimos en Firestore
        val data = actuales.map { r ->
            mapOf(
                "label"           to r.label,
                "image"           to r.image,
                "url"             to r.url,
                "yield"           to r.yield,
                "ingredientLines" to r.ingredientLines
            )
        }
        db.collection("usuarios")
            .document(uid)
            .update("recetas", data)
            .addOnFailureListener { Log.e("RecetaVM", "Error fav: $it") }
    }

    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }
}