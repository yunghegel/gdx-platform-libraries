package dev.jamiecrown.gdx.core.lsp

import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageServer

class LspClientManager(private val serverProcess: GlslAnalyzerProcess) {
    private var languageServer: LanguageServer? = null

    fun start() {
        val client = MyLanguageClient()
        val launcher = LSPLauncher.Builder<LanguageServer>()
            .setLocalService(client)
            .setRemoteInterface(LanguageServer::class.java)
            .setInput(serverProcess.getInputStream())
            .setOutput(serverProcess.getOutputStream())
            .create()
        languageServer = launcher.remoteProxy
        launcher.startListening()

        // Initialize the language server
        val initializeParams = InitializeParams().apply {
            processId = ProcessHandle.current().pid().toInt()
            rootUri = "file:///path/to/your/project" // Set a relevant root URI
            capabilities = ClientCapabilities()
        }
        languageServer?.initialize(initializeParams)?.thenRun {
            languageServer?.initialized(InitializedParams())
        }
    }
    
}