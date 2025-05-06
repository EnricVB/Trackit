package dev.enric.cli.remote

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.admin.RemoteConfig
import dev.enric.core.handler.remote.PushHandler
import dev.enric.domain.objects.remote.DataProtocol
import dev.enric.exceptions.RemoteDirectionNotFoundException
import dev.enric.util.index.BranchIndex
import kotlinx.coroutines.runBlocking
import picocli.CommandLine.Command

@Command(
    name = "push",
    description = ["Push the current repository to a remote server"],
    mixinStandardHelpOptions = false,
)
class Push : TrackitCommand() {

    override fun call(): Int = runBlocking {
        super.call()

        // Get the remote URL from the config
        val remoteConfig = RemoteConfig()
        val (remotePush, _) = remoteConfig.load()

        if (remotePush == null) { // Check if remote push URL is set
            throw RemoteDirectionNotFoundException("Remote push URL not found in configuration. Please set it up using the config command.")
        }

        // Parse the remote URL to DataProtocol
        val remotePushUrl = DataProtocol.validateRequest(remotePush)
            ?: throw RemoteDirectionNotFoundException("Invalid remote push URL format. Please check the URL.")

        val handler = PushHandler(DataProtocol.toDataProtocol(remotePushUrl))
        val socket = handler.startRemoteConnection()

        // Check if Remote has no pull requests
        /*
        TODO:
         Para usuarios y demas: Enviar StatusQueryMessage para pedir -> Usuarios, Roles, Permisos. Si falta algun dato, detectar la configuración (autoPush: true/false), y enviarlo o mandar un error.
         Tambien se ha de revisar el sendError que no manda ningun mensaje al cliente.
        */

        // Send all objects to the remote server
        handler.sendObjectsToRemote(socket, BranchIndex.getCurrentBranch())

        return@runBlocking 0
    }
}