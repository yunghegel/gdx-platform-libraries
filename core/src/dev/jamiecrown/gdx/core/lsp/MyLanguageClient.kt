package dev.jamiecrown.gdx.core.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient
import java.util.concurrent.CompletableFuture

class MyLanguageClient : LanguageClient {
    // Implement methods to handle server notifications
    override fun telemetryEvent(p0: Any?) {}
    override fun publishDiagnostics(p0: PublishDiagnosticsParams?) {
        // This is where you'll receive error and warning information
        // to display in your editor.
    }
    override fun showMessage(p0: MessageParams?) {}
    override fun showMessageRequest(p0: ShowMessageRequestParams?): CompletableFuture<MessageActionItem> {
        return CompletableFuture()
    }
    override fun logMessage(p0: MessageParams?) {}
}