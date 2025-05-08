package dev.enric.remote.packet.message.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * This class represents the data structure for branch synchronization status.
 * Contains a list of objects with:
 * - String -> BranchName.
 * - BranchSyncStatus -> Status of the branch.
 * - Pair ->
 * - - Left -> List of Commits that are missing on the remote.
 * - - Right -> List of Commits that are missing on the local.
 */
data class BranchSyncStatusResponseData(
    val objects: List<Triple<String, BranchSyncStatus, Pair<List<String>, List<String>>>> = emptyList(),
) {

    fun encode(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)

        // Write the size of the payload
        objectOutputStream.writeInt(objects.size)

        // Write each object to the output stream
        for ((branchName, status, commitsPair) in objects) {
            // Write branch name, status and commits
            objectOutputStream.writeUTF(branchName)
            objectOutputStream.writeUTF(status.name)

            // Write the number of commits
            objectOutputStream.writeInt(commitsPair.first.size)
            for (commit in commitsPair.first) {
                objectOutputStream.writeUTF(commit)
            }

            // Write the number of commits
            objectOutputStream.writeInt(commitsPair.second.size)
            for (commit in commitsPair.second) {
                objectOutputStream.writeUTF(commit)
            }
        }

        // Flush and close the output stream
        objectOutputStream.flush()
        return byteArrayOutputStream.toByteArray()
    }

    companion object {
        fun decode(data: ByteArray): BranchSyncStatusResponseData {
            val byteArrayInputStream = ByteArrayInputStream(data)
            val objectInputStream = ObjectInputStream(byteArrayInputStream)

            // Read the size of the payload
            val size = objectInputStream.readInt()
            val result = mutableListOf<Triple<String, BranchSyncStatus, Pair<List<String>, List<String>>>>()

            // Read each object from the input stream
            for (i in 0 until size) {
                val branchName = objectInputStream.readUTF()
                val status = BranchSyncStatus.valueOf(objectInputStream.readUTF())
                val commits = Pair(
                    List(objectInputStream.readInt()) {
                        objectInputStream.readUTF()
                    },
                    List(objectInputStream.readInt()) {
                        objectInputStream.readUTF()
                    }
                )

                result.add(Triple(branchName, status, commits))
            }

            // Return the decoded object
            return BranchSyncStatusResponseData(result)
        }
    }

    enum class BranchSyncStatus {
        BEHIND,
        AHEAD,
        SYNCED,
        DIVERGED,
        ONLY_LOCAL,
        ONLY_REMOTE,
    }
}
