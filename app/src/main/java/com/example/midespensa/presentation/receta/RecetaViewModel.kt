package com.example.midespensa.presentation.receta

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.midespensa.BuildConfig
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
import java.net.URLEncoder
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
    private val appId = BuildConfig.EDAMAM_APP_ID
    private val appKey = BuildConfig.EDAMAM_APP_KEY

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
                            val labelEs    = translateOffline(receta.label, enToEsTranslator)
                            val ingredEs   = receta.ingredientLines.map { translateOffline(it, enToEsTranslator) }
                            receta.copy(label = labelEs, ingredientLines = ingredEs)
                        }
                        _favoritesTranslated.value = translated
                    }
                }
        }
    }

    fun toggleTranslation() { _showTranslated.value = !_showTranslated.value }
    fun toggleShowFavorites() { _showFavorites.value = !_showFavorites.value }

    /**
     * Realiza una búsqueda de recetas usando la API de Edamam.
     * Traduce la consulta al inglés, realiza la petición a la API y traduce los resultados al español.
     * Actualiza los estados internos con las recetas obtenidas.
     *
     *  @param query Consulta de búsqueda en inglés.
     */
    fun searchRecetas(query: String) {
        Log.d("RecetaVM", "buscarRecetas llamada con query: '$query'")
        _showFavorites.value = false

        viewModelScope.launch {
            _isLoading.value = true

            // Traducir la consulta al inglés
            val queryEn = withContext(Dispatchers.Default) {
                translateOffline(query, esToEnTranslator)
            }
            Log.d("RecetaVM", "Consulta traducida: '$queryEn'")

            // Codificar y construir la URL
            val qEsc = URLEncoder.encode(queryEn, "UTF-8")
            val url =
                "https://api.edamam.com/api/recipes/v2?type=public&q=$qEsc&app_id=$appId&app_key=$appKey&to=5"
            Log.d("RecetaVM", "URL Edamam -> $url")

            // Llamada a la API
            val ingles = withContext(Dispatchers.IO) {
                runCatching {
                    val client = HttpClient(CIO) {
                        install(ContentNegotiation) {
                            json(Json { ignoreUnknownKeys = true })
                        }
                    }
                    val resp: JsonObject = client.get(url) {
                        user?.uid?.let { header("Edamam-Account-User", it) }
                    }.body()
                    Log.d("RecetaVM", "Respuesta cruda Edamam: $resp")

                    val hits = resp["hits"]?.jsonArray
                    Log.d("RecetaVM", "Número de hits: ${hits?.size ?: 0}")

                    hits
                        ?.mapNotNull { hit ->
                            hit.jsonObject["recipe"]?.jsonObject?.let { r ->
                                Receta(
                                    label = r["label"]!!.jsonPrimitive.content,
                                    image = r["image"]!!.jsonPrimitive.content,
                                    url = r["url"]!!.jsonPrimitive.content,
                                    yield = r["yield"]?.jsonPrimitive?.intOrNull ?: 1,
                                    ingredientLines = r["ingredientLines"]!!
                                        .jsonArray
                                        .map { it.jsonPrimitive.content }
                                )
                            }
                        }
                        ?: emptyList()
                }.getOrDefault(emptyList())
            }

            // Actualizar lista en inglés
            _recetasEnglish.value = ingles

            // Traducir resultados al español
            val traducidas = ingles.map { receta ->
                async(Dispatchers.Default) {
                    val lab = translateOffline(receta.label, enToEsTranslator)
                    val ing = receta.ingredientLines.map { translateOffline(it, enToEsTranslator) }
                    receta.copy(label = lab, ingredientLines = ing)
                }
            }.awaitAll()
            _recetasTranslated.value = traducidas

            delay(200)
            _isLoading.value = false
        }
    }

    /**
     * Traduce un texto de forma offline utilizando ML Kit.
     * Convierte la operación asincrónica en una coroutine suspendida.
     */
    private suspend fun translateOffline(text: String, trans: Translator): String =
        suspendCancellableCoroutine { cont ->
            trans.translate(text)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    /**
     * Añade o elimina una receta de la lista de favoritos.
     * Actualiza localmente la lista en memoria (UI instantánea) y, a continuación,
     * dispara la persistencia en Firestore sin bloquear la UI.
     *
     * @param receta La receta que el usuario está marcando o desmarcando como favorita.
     */
    fun toggleFavorito(receta: Receta) {
        val uid = user?.uid ?: return

        val actuales = _favoritesEnglish.value.toMutableList()
        if (actuales.any { it.url == receta.url }) {
            actuales.removeAll { it.url == receta.url }
            _favoritesTranslated.value = _favoritesTranslated.value.filter { it.url != receta.url }
        } else {

            actuales.add(receta)
            viewModelScope.launch(Dispatchers.Default) {
                val recetaEs = receta.copy(
                    label = translateOffline(receta.label, enToEsTranslator),
                    ingredientLines = receta.ingredientLines.map { translateOffline(it, enToEsTranslator) }
                )
                // Anexa solo la receta traducida al final de la lista existente
                _favoritesTranslated.value = _favoritesTranslated.value + recetaEs
            }
        }
        // Publica la lista en inglés en memoria; esto hace que el icono cambie al instante
        _favoritesEnglish.value = actuales

        // 2) Persiste en Firestore sin esperar respuesta (UI ya reaccionó)
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
            .addOnFailureListener { Log.e("RecetaVM", "Error guardando favorito: $it") }
    }


    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }
}