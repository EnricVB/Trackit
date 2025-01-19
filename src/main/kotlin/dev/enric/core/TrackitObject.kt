package dev.enric.core

import dev.enric.core.objects.Hash

interface TrackitObject<T : TrackitObject<T>> {

    fun encode(): Pair<Hash, ByteArray>

    fun encode(writeOnDisk: Boolean = false): Pair<Hash, ByteArray>

    fun decode(hash: Hash): T

    fun generateKey(): Hash

    fun compressContent(): ByteArray

    fun decompressContent(compressedData: ByteArray): String

    fun printInfo(): String

    fun showDifferences(newer: Hash, oldest: Hash): String
}
