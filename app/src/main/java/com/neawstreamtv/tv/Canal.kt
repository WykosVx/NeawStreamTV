package com.neawstreamtv.tv

data class Canal(
    val nombre: String,
    val url: String,
    val logoUrl: String = "",
    val grupo: String = ""
)

object M3uParser {
    fun parsearLista(contenido: String): List<Canal> {
        val canales = mutableListOf<Canal>()
        val lineas = contenido.lines()
        
        var nombreActual = ""
        var logoActual = ""
        var grupoActual = ""

        for (linea in lineas) {
            val trimmed = linea.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXTINF:")) {
                // Reiniciamos valores por cada EXTINF
                nombreActual = ""
                logoActual = ""
                grupoActual = ""

                try {
                    grupoActual = extraerAtributo(trimmed, "group-title") 
                        ?: extraerAtributo(trimmed, "tvg-group") 
                        ?: ""

                    logoActual = extraerAtributo(trimmed, "tvg-logo") ?: ""

                    val ultimaComa = trimmed.lastIndexOf(',')
                    if (ultimaComa != -1 && ultimaComa < trimmed.length - 1) {
                        nombreActual = trimmed.substring(ultimaComa + 1).trim()
                    }
                } catch (e: Exception) {
                    LogManager.add("Error parseando EXTINF: ${e.message}")
                }
            } else if (!trimmed.startsWith("#")) {
                var urlFinal = trimmed
                
                if (trimmed.contains("|") && !trimmed.startsWith("http")) {
                    val partes = trimmed.split("|", limit = 2)
                    if (partes.size == 2) {
                        nombreActual = partes[0].trim()
                        urlFinal = partes[1].trim()
                    }
                }

                if (urlFinal.startsWith("http") || urlFinal.startsWith("rtmp") || urlFinal.startsWith("rtsp") || urlFinal.startsWith("magnet")) {
                    if (nombreActual.isEmpty()) {
                        nombreActual = "Canal ${canales.size + 1}"
                    }
                    
                    canales.add(
                        Canal(
                            nombre = nombreActual,
                            url = urlFinal,
                            logoUrl = logoActual,
                            grupo = grupoActual
                        )
                    )
                    
                    // Limpiar estados para el siguiente
                    nombreActual = ""
                    logoActual = ""
                    grupoActual = ""
                }
            }
        }

        LogManager.add("Listas procesadas: ${canales.size} canales encontrados.")
        return canales
    }

    private fun extraerAtributo(linea: String, atributo: String): String? {
        try {
            val pattern = "$atributo=\"([^\"]*)\"".toRegex(RegexOption.IGNORE_CASE)
            val match = pattern.find(linea)
            if (match != null) {
                return match.groups[1]?.value
            }
            val patternSinComillas = "$atributo=([^\\s]+)".toRegex(RegexOption.IGNORE_CASE)
            val match2 = patternSinComillas.find(linea)
            return match2?.groups[1]?.value
        } catch (e: Exception) {
            return null
        }
    }
}