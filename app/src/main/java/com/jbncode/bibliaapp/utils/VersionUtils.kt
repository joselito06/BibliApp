package com.jbncode.bibliaapp.utils

object VersionUtils {
    /**
     * Compara la versión instalada con la versión mínima requerida.
     * Retorna true si currentVersion < targetVersion (requiere actualización).
     */
    fun isUpdateRequired(currentVersion: String, minRequiredVersion: String): Boolean {
        if (currentVersion == minRequiredVersion) return false

        val currentArr = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val targetArr = minRequiredVersion.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(currentArr.size, targetArr.size)
        for (i in 0 until maxLength) {
            val v1 = currentArr.getOrElse(i) { 0 }
            val v2 = targetArr.getOrElse(i) { 0 }
            if (v1 < v2) return true
            if (v1 > v2) return false
        }
        return false
    }
}