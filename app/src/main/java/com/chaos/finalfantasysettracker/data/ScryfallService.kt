package com.chaos.finalfantasysettracker.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class ScryfallService(
    private val gson: Gson = Gson()
) {
    suspend fun fetchCardPrice(scryfallId: String, preferFoil: Boolean): Double? = withContext(Dispatchers.IO) {
        if (scryfallId.isBlank()) return@withContext null

        val endpoint = "https://api.scryfall.com/cards/${scryfallId.trim()}"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "FinalFantasySetTracker/1.0")
        }

        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.w(TAG, "[SCRYFALL_PRICE] Non-success status=$status for id=$scryfallId")
                return@withContext null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val root = gson.fromJson(body, JsonObject::class.java)
            val prices = root?.getAsJsonObject("prices") ?: return@withContext null

            val usd = prices.get("usd")?.takeIf { !it.isJsonNull }?.asString
            val usdFoil = prices.get("usd_foil")?.takeIf { !it.isJsonNull }?.asString

            val selected = if (preferFoil) usdFoil ?: usd else usd ?: usdFoil
            val value = selected?.toDoubleOrNull()
            Log.d(TAG, "[SCRYFALL_PRICE] id=$scryfallId preferFoil=$preferFoil usd=$usd usdFoil=$usdFoil selected=$value")
            value
        } catch (t: Throwable) {
            Log.w(TAG, "[SCRYFALL_PRICE] Failed id=$scryfallId error=${t.message}")
            null
        } finally {
            connection.disconnect()
        }
    }
}

private const val TAG = "ScryfallService"
