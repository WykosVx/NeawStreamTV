package com.neawstreamtv.tv

// 1. Estructura de datos (Esta línea debe estar visible al inicio para evitar el error rojo)
data class Canal(
    val nombre: String,
    val url: String,
    val logoUrl: String
)

// 2. Tu algoritmo de lectura M3U
object M3uParser {
    fun parsearLista(body: String): List<Canal> {
        val lineas = body.split("\n")
        val listaTemporal = mutableListOf<Canal>()
        var nombre: String? = null
        var logoUrl: String? = null

        for (rawLinea in lineas) {
            val linea = rawLinea.trim()
            if (linea.startsWith("#EXTINF")) {
                // Extrae el nombre después de la última coma (igual que tu split(',').last)
                nombre = linea.substringAfterLast(",").trim()

                // Expresión regular idéntica para buscar el logo de la TV
                val match = Regex("""tvg-logo="([^"]+)"""").find(linea)
                logoUrl = match?.groupValues?.get(1) ?: ""
            } else if (linea.isNotEmpty() && !linea.startsWith("#") && nombre != null) {
                listaTemporal.add(Canal(nombre = nombre, url = linea, logoUrl = logoUrl ?: ""))
                nombre = null
                logoUrl = null
            }
        }
        return listaTemporal
    }
}