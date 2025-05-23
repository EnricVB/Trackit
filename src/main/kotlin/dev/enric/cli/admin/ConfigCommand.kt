package dev.enric.cli.admin

import dev.enric.cli.TrackitCommand
import dev.enric.core.handler.admin.KeepSession
import dev.enric.core.handler.admin.RemoteDirectivesConfig
import dev.enric.core.handler.admin.RemotePathConfig
import dev.enric.logger.Logger
import dev.enric.core.security.AuthUtil
import dev.enric.util.common.console.SystemConsoleInput
import dev.enric.util.index.UserIndex
import picocli.CommandLine.Command
import picocli.CommandLine.Option

/**
 * Command to configure repository-level or system-wide settings for Trackit.
 *
 * This command is mainly used for:
 * - Setting up and storing user credentials for session persistence.
 * - Applying configuration either globally (system level) or locally (repository level).
 *
 * Usage examples:
 *   trackit config --username alice --keep-session          -> Saves session locally for user 'alice'
 *   trackit config --global --keep-session                  -> Prompts for credentials, saves session globally
 *   trackit config --username bob --password pass --global  -> Saves session globally for user 'bob'
 *
 * If credentials are not passed via options, the user will be prompted to enter them via console input.
 */
@Command(
    name = "config",
    description = [
        "Configure user credentials and session settings locally or globally.",
        "Supports persistent sessions and scoped configuration."
    ],
    mixinStandardHelpOptions = true,
    usageHelpWidth = 500,
    footer = [
        "",
        "Keep Session Examples:",
        "  trackit config --username alice --keep-session",
        "    Save session locally for user 'alice'.",
        "",
        "  trackit config --global --keep-session",
        "    Prompt for credentials, save session globally.",
        "",
        "  trackit config --username bob --password pass --global",
        "    Save session globally for user 'bob'.",
        "",
        "",
        "Remote Configuration Examples:",
        "  trackit config --remote-push <url> --remote-fetch <url>",
        "    Set remote URLs for push and fetch operations.",
        "    URLs must have this format: trackit://<username>:<password>@<host>:<port>/<absolutePathToRemoteRepository>",
        "",
        "  trackit config --remote",
        "    Show current remote configuration.",
        "",
        "Auto-Push Configuration Examples:",
        "  trackit config --autopush",
        "    Enable auto-push for missing data.",
        "",
        "  trackit config --no-autopush",
        "    Disable auto-push for missing data.",
        "",
        "Notes:",
        "  - If no credentials are provided via options, you will be prompted.",
        "  - Use --local (default) or --global to control scope of configuration."
    ],
)
class ConfigCommand : TrackitCommand() {

    /**
     * The username to authenticate the user.
     * If not provided, the user will be prompted to input it.
     */
    @Option(
        names = ["--username", "-u"],
        description = ["Define the username to configure"],
        required = false
    )
    var username: String? = null

    /**
     * The password for authentication.
     * If not provided, the user will be prompted securely via console.
     */
    @Option(
        names = ["--password", "-p"],
        description = ["Define the password of the user"],
        required = false
    )
    var password: String? = null

    /**
     * Indicates whether to persist the session, allowing the user to remain authenticated
     * even after closing and reopening the terminal.
     *
     * When enabled, credentials will be securely stored (locally or globally).
     */
    @Option(
        names = ["--keep-session", "-ks"],
        description = ["Keep session open after closing the terminal"],
        required = false
    )
    var keepSession: Boolean = false

    /**
     * Applies configuration changes globally, affecting all repositories.
     * Data is stored in the user's environment.
     *
     * Mutually exclusive with --local.
     */
    @Option(
        names = ["--global"],
        description = ["Apply changes at system level"],
        required = false
    )
    var global: Boolean = false

    /**
     * Applies configuration changes locally, within the current repository.
     * This is the default behavior unless --global is specified.
     *
     * This is never used really.
     */
    @Option(
        names = ["--local"],
        description = ["Apply changes at repository level (default)"],
        required = false
    )
    var local: Boolean = true

    @Option(
        names = ["--remote-push"],
        description = ["Define the remote URL or path for push operations"],
        required = false
    )
    var remotePush: String? = null

    @Option(
        names = ["--remote-fetch"],
        description = ["Define the remote URL or path for fetch operations"],
        required = false
    )
    var remoteFetch: String? = null

    @Option(
        names = ["--remote"],
        description = ["Shows the remote URL or path for push and fetch operations"],
        required = false
    )
    var remote: Boolean = false

    @Option(
        names = ["--autopush"],
        description = ["Determines if missing data, as Users, Roles, etc, should be sent on push automatically"],
        required = false
    )
    var autopush: Boolean = false

    @Option(
        names = ["--no-autopush"],
        description = ["Determines if missing data, as Users, Roles, etc, should not be sent on push automatically"],
        required = false
    )
    var noAutopush: Boolean = false


    /**
     * Executes the configuration logic.
     * Validates credentials if session persistence is requested,
     * and stores the session data based on scope (local/global).
     *
     * @return 0 if success, 1 if invalid credentials or input error.
     */
    override fun call(): Int {
        super.call()

        if (keepSession) {
            if (!validateCredentials()) {
                Logger.error("Invalid credentials")
                return 1
            }
            saveSession()
        }

        if (!remotePush.isNullOrBlank() || !remoteFetch.isNullOrBlank()) {
            saveRemoteConfig()
        }

        if (autopush || noAutopush) {
            saveAutoPushConfig()
        }

        if (remote) {
            showRemoteConfig()
        }

        return 0
    }

    /**
     * Validates the provided or prompted credentials using the authentication utility.
     * If credentials are missing and a console is available, prompts the user.
     * Otherwise, logs an error.
     *
     * @return true if authentication succeeds; false otherwise.
     */
    private fun validateCredentials(): Boolean {
        if (username.isNullOrBlank()) {
            val console = SystemConsoleInput.getInstance()
            username = console.readLine("Enter username: ")
        }

        if (password.isNullOrBlank()) {
            val console = SystemConsoleInput.getInstance()
            password = String(console.readPassword("Enter password: "))
        }

        return AuthUtil.authenticate(username!!, password!!)
    }

    /**
     * Saves the session based on scope (global or local), using the provided credentials.
     * Credentials are wrapped with a salt value for security purposes.
     * Logs progress and completion.
     */
    private fun saveSession() {
        val salt = UserIndex.getUser(username ?: "")?.salt
        val session = KeepSession(username ?: "", password ?: "", salt)

        if (global) {
            Logger.info("Saving session at system level")
            session.globalSave()
        } else {
            Logger.info("Saving session at repository level")
            session.localSave()
        }

        Logger.info("Session saved")
    }

    /**
     * Saves the remote configuration (push and fetch URLs) based on scope (global or local).
     * Logs progress and completion.
     */
    private fun saveRemoteConfig() {
        RemotePathConfig(remotePush, remoteFetch).save()

        Logger.info("Remote configuration saved")
    }

    /**
     * Saves the auto-push directive based on the provided value.
     * Logs progress and completion.
     */
    private fun saveAutoPushConfig() {
        RemoteDirectivesConfig().saveAutoPushDirective(autopush && !noAutopush)

        Logger.info("Remote directive saved")
    }

    /**
     * Displays the current remote configuration for push and fetch operations.
     */
    private fun showRemoteConfig() {
        val remoteConfig = RemotePathConfig()
        val (remotePush, remoteFetch) = remoteConfig.load()

        if (remotePush != null) {
            Logger.info("Remote Push URL: $remotePush")
        } else {
            Logger.info("No Remote Push URL configured.")
        }

        if (remoteFetch != null) {
            Logger.info("Remote Fetch URL: $remoteFetch")
        } else {
            Logger.info("No Remote Fetch URL configured.")
        }
    }
}
