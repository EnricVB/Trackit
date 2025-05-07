package dev.enric.remote.packet.message.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * This class represents the data structure that will be sent to Remote Server to ask for Branch Sync Status.
 * Contains a list of objects with:
 * - String -> BranchName.
 * - String -> All branch commit hashes.
 */
data class BranchSyncStatusQueryData(
    val objects: List<Pair<String, List<String>>> = emptyList(),
) {

    fun encode(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)

        // Write the size of the payload
        objectOutputStream.writeInt(objects.size)

        // Write each object to the output stream
        for (branchList in objects) {
            // Write branch name
            objectOutputStream.writeUTF(branchList.first)

            // Write the number of commits
            objectOutputStream.writeInt(branchList.second.size)
            for (commit in branchList.second) {
                objectOutputStream.writeUTF(commit)
            }
        }

        // Flush and close the output stream
        objectOutputStream.flush()
        return byteArrayOutputStream.toByteArray()
    }

    companion object {
        fun decode(data: ByteArray): BranchSyncStatusQueryData {
            val byteArrayInputStream = ByteArrayInputStream(data)
            val objectInputStream = ObjectInputStream(byteArrayInputStream)

            // Read the size of the payload
            val size = objectInputStream.readInt()
            val result = mutableListOf<Pair<String, List<String>>>()

            // Read each object from the input stream
            for (i in 0 until size) {
                val branchName = objectInputStream.readUTF()

                // Read the number of commits
                val commitCount = objectInputStream.readInt()
                val commitList = mutableListOf<String>()
                for (j in 0 until commitCount) {
                    val commitHash = objectInputStream.readUTF()
                    commitList.add(commitHash)
                }

                result.add(Pair(branchName, commitList))
            }

            // Return the decoded object
            return BranchSyncStatusQueryData(result)
        }
    }
}
