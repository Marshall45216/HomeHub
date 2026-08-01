package com.familycal.tv.graph

import com.familycal.tv.model.DayForecast
import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*

/**
 * Open-Meteo requires no API key and no account -- good fit for a hobby
 * family-TV project. Swap in a different provider here if you'd rather use
 * one you already have a key for (e.g. OpenWeatherMap).
 */
data class OpenMeteoResponse(
    val daily: DailyBlock
)

data class DailyBlock(
    val time: List<String>,
    @SerializedName("temperature_2m_max") val tempMax: List<Double>,
    @SerializedName("temperature_2m_min") val tempMin: List<Double>,
    @SerializedName("weathercode") val weatherCode: List<Int>
)

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getWeeklyForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weathercode",
        @Query("temperature_unit") unit: String = "fahrenheit",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") days: Int = 7
    ): OpenMeteoResponse
}

// Open-Meteo's free geocoding service -- turns a typed city name ("Bradenton, FL")
// into coordinates, no API key needed. Separate host from the forecast API.
data class GeocodeResponse(val results: List<GeocodeResult>?)
data class GeocodeResult(val name: String, val admin1: String?, val latitude: Double, val longitude: Double)

interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 1
    ): GeocodeResponse
}

/** Thrown when a typed city name doesn't match anything -- catch this to show the person a friendly message. */
class CityNotFoundException(city: String) : Exception("Couldn't find a location matching \"$city\"")

class WeatherRepository {
    private val api = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenMeteoApi::class.java)

    private val geocodingApi = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeocodingApi::class.java)

    /**
     * Looks up whatever the person typed (e.g. "Bradenton", "Bradenton, FL",
     * "Bradenton, Florida") and returns the matched place name plus its
     * forecast. Throws CityNotFoundException if nothing matches.
     */
    suspend fun getWeekForecastForCity(cityQuery: String): Pair<String, List<DayForecast>> {
        val geo = geocodingApi.search(cityQuery)
        val match = geo.results?.firstOrNull() ?: throw CityNotFoundException(cityQuery)
        val label = if (match.admin1 != null) "${match.name}, ${match.admin1}" else match.name
        return label to getWeekForecast(match.latitude, match.longitude)
    }

    /** Pass exact lat/lon directly if you already have them. */
    suspend fun getWeekForecast(lat: Double, lon: Double): List<DayForecast> {
        val resp = api.getWeeklyForecast(lat, lon)
        val dayFmt = java.time.format.DateTimeFormatter.ofPattern("EEE", Locale.US)

        return resp.daily.time.indices.map { i ->
            val date = java.time.LocalDate.parse(resp.daily.time[i])
            DayForecast(
                date = date,
                dayLabel = if (i == 0) "Today" else date.format(dayFmt),
                highF = resp.daily.tempMax[i].toInt(),
                lowF = resp.daily.tempMin[i].toInt(),
                condition = mapWeatherCode(resp.daily.weatherCode[i])
            )
        }
    }

    // WMO weather codes -> our simplified condition strings (see
    // https://open-meteo.com/en/docs for the full code table)
    private fun mapWeatherCode(code: Int): String = when (code) {
        0 -> "Sunny"
        1, 2 -> "Partly Cloudy"
        3 -> "Cloudy"
        45, 48 -> "Cloudy"
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> "Rain"
        71, 73, 75, 77, 85, 86 -> "Snow"
        95, 96, 99 -> "Storm"
        else -> "Partly Cloudy"
    }
}
