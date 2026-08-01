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

class WeatherRepository {
    private val api = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenMeteoApi::class.java)

    /** Pass your home's lat/lon -- see README "Weather setup" for how to find them. */
    suspend fun getWeekForecast(lat: Double, lon: Double): List<DayForecast> {
        val resp = api.getWeeklyForecast(lat, lon)
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val dateFmt = SimpleDateFormat("MMM d", Locale.US)
        val parseFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        return resp.daily.time.indices.map { i ->
            val date = parseFmt.parse(resp.daily.time[i]) ?: Date()
            DayForecast(
                dayLabel = if (i == 0) "Today" else dayFmt.format(date),
                dateLabel = dateFmt.format(date),
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
