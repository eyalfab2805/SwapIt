package com.example.swapit.data

object GeoHash {
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"
    private val BITS = intArrayOf(16, 8, 4, 2, 1)

    fun encode(lat: Double, lng: Double, precision: Int = 8): String {
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0

        val sb = StringBuilder()
        var isEven = true
        var bit = 0
        var ch = 0

        while (sb.length < precision) {
            if (isEven) {
                val mid = (lonMin + lonMax) / 2
                if (lng >= mid) { ch = ch or BITS[bit]; lonMin = mid } else lonMax = mid
            } else {
                val mid = (latMin + latMax) / 2
                if (lat >= mid) { ch = ch or BITS[bit]; latMin = mid } else latMax = mid
            }

            isEven = !isEven
            if (bit < 4) bit++ else {
                sb.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }

        return sb.toString()
    }
}
