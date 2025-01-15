package dev.enric.core

interface TrackitObject<T : TrackitObject<T>> {

    fun encode() : Hash

    fun encode(writeOnDisk : Boolean = false) : Hash

    fun decode() : T

    fun printInfo() : String

    fun showDifferences(newer : Hash, oldest : Hash) : String
}
