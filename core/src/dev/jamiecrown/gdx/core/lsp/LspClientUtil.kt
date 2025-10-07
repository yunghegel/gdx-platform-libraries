package dev.jamiecrown.gdx.core.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.CompletableFuture

/**
 * A utility class to simplify communication with a Language Server.
 *
 * @param languageServer The proxy object for the remote language server.
 * @param rootUri The root URI of the workspace, crucial for the server to locate files.
 */
class LspClientUtil(
    private val languageServer: LanguageServer,
    private val rootUri: String
) {

    private var documentVersion = 0
    private fun dbg(msg: String) = println("[DEBUG_LOG] LspClientUtil: $msg")

    /**
     * Notifies the language server that a document has been opened.
     * This should be called whenever a file is loaded into the editor.
     *
     * @param fileUri The URI of the document that was opened.
     * @param languageId The language identifier for the document (e.g., "glsl").
     * @param text The full content of the document.
     */
    fun notifyDidOpen(fileUri: String, languageId: String, text: String) {
        val textDocument = TextDocumentItem(fileUri, languageId, ++documentVersion, text)
        val params = DidOpenTextDocumentParams(textDocument)
        dbg("didOpen uri=$fileUri languageId=$languageId version=${textDocument.version} length=${text.length}")
        languageServer.textDocumentService.didOpen(params)
    }

    /**
     * Notifies the language server that a document has been changed.
     * This should be called whenever the user modifies the content of the editor.
     *
     * @param fileUri The URI of the document that was changed.
     * @param newText The new, full content of the document.
     */
    fun notifyDidChange(fileUri: String, newText: String) {
        val versionedTextDocumentIdentifier = VersionedTextDocumentIdentifier(fileUri, ++documentVersion)
        val textDocumentContentChangeEvent = TextDocumentContentChangeEvent(newText)
        val params = DidChangeTextDocumentParams(versionedTextDocumentIdentifier, listOf(textDocumentContentChangeEvent))
        dbg("didChange uri=$fileUri version=${versionedTextDocumentIdentifier.version} length=${newText.length}")
        languageServer.textDocumentService.didChange(params)
    }

    /**
     * Notifies the language server that a document has been closed.
     * This should be called when the file is closed in the editor.
     *
     * @param fileUri The URI of the document that was closed.
     */
    fun notifyDidClose(fileUri: String) {
        val textDocument = TextDocumentIdentifier(fileUri)
        val params = DidCloseTextDocumentParams(textDocument)
        dbg("didClose uri=$fileUri")
        languageServer.textDocumentService.didClose(params)
    }

    /**
     * Requests code completion proposals from the language server.
     *
     * @param fileUri The URI of the document.
     * @param line The zero-based line number of the cursor.
     * @param char The zero-based character offset of the cursor.
     * @return A CompletableFuture that will resolve to a list of completion items.
     */
    fun requestCompletion(fileUri: String, line: Int, char: Int): CompletableFuture<Either<List<CompletionItem>, CompletionList>> {
        val position = Position(line, char)
        val textDocument = TextDocumentIdentifier(fileUri)
        val params = CompletionParams(textDocument, position)
        dbg("requestCompletion uri=$fileUri line=$line char=$char")
        return languageServer.textDocumentService.completion(params)
    }

    /**
     * Requests hover information from the language server.
     * This is useful for displaying tooltips when the user hovers over a symbol.
     *
     * @param fileUri The URI of the document.
     * @param line The zero-based line number of the cursor.
     * @param char The zero-based character offset of the cursor.
     * @return A CompletableFuture that will resolve to the hover information.
     */
    fun requestHover(fileUri: String, line: Int, char: Int): CompletableFuture<Hover> {
        val position = Position(line, char)
        val textDocument = TextDocumentIdentifier(fileUri)
        val params = HoverParams(textDocument, position)
        dbg("requestHover uri=$fileUri line=$line char=$char")
        return languageServer.textDocumentService.hover(params)
    }

    /**
     * Requests the definition of a symbol at a given position.
     * This is used for "go to definition" functionality.
     *
     * @param fileUri The URI of the document.
     * @param line The zero-based line number of the cursor.
     * @param char The zero-based character offset of the cursor.
     * @return A CompletableFuture that will resolve to the location(s) of the definition.
     */
    fun requestDefinition(fileUri: String, line: Int, char: Int): CompletableFuture<Either<List<Location>, List<LocationLink>>> {
        val position = Position(line, char)
        val textDocument = TextDocumentIdentifier(fileUri)
        val params = DefinitionParams(textDocument, position)
        dbg("requestDefinition uri=$fileUri line=$line char=$char")
        return languageServer.textDocumentService.definition(params)
    }


}