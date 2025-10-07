package dev.jamiecrown.gdx.core.util

import com.badlogic.gdx.scenes.scene2d.utils.UIUtils
import org.lwjgl.system.macosx.LibC
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.management.ManagementFactory

/**
 * Adds some utilities to ensure that the JVM was started with the
 * `-XstartOnFirstThread` argument, which is required on macOS for LWJGL 3
 * to function.
 *
 * @author damios
 * @see [Based
 * on http://www.java-gaming.org/topics/-/37697/view.html](http://www.java-gaming.org/topics/starting-jvm-on-mac-with-xstartonfirstthread-programmatically/37697/view.html)
 */
object ThreadHelper {

    private const val JVM_RESTARTED_ARG = "jvmIsRestarted"

    /**
     * Starts a new JVM if the application was started on macOS without the
     * `-XstartOnFirstThread` argument. Returns whether a new JVM was started
     * and thus no code should be executed.
     *
     *
     * <u>Usage:</u>
     *
     * <pre>
     * public static void main(String... args) {
     * if (StartOnFirstThreadHelper.startNewJvmIfRequired()) {
     * return; // don't execute any code
     * }
     * // the actual main method code
     * }
    </pre> *
     *
     * @param redirectOutput whether the output of the new JVM should be rerouted to
     * the new JVM, so it can be accessed in the same place;
     * keeps the old JVM running if enabled
     * @return whether a new JVM was started and thus no code should be executed in
     * this one
     */
    /**
     * Starts a new JVM if the application was started on macOS without the
     * `-XstartOnFirstThread` argument. Returns whether a new JVM was started
     * and thus no code should be executed. Redirects the output of the new JVM to
     * the old one.
     *
     *
     * <u>Usage:</u>
     *
     * <pre>
     * public static void main(String... args) {
     * if (StartOnFirstThreadHelper.startNewJvmIfRequired()) {
     * return; // don't execute any code
     * }
     * // the actual main method code
     * }
    </pre> *
     *
     * @return whether a new JVM was started and thus no code should be executed in
     * this one
     */
    fun startNewJvmIfRequired(redirectOutput: Boolean = true): Boolean {
        if (!UIUtils.isMac) {
            return false
        }

        val pid = LibC.getpid()

        // check whether -XstartOnFirstThread is enabled
        if ("1" == System.getenv("JAVA_STARTED_ON_FIRST_THREAD_" + pid)) {
            return false
        }

        // check whether the JVM was previously restarted
        // avoids looping, but most certainly leads to a crash
        if ("true" == System.getProperty(JVM_RESTARTED_ARG)) {
            System.err.println(
                "There was a problem evaluating whether the JVM was started with the -XstartOnFirstThread argument.",
            )
            return false
        }

        // Restart the JVM with -XstartOnFirstThread
        val jvmArgs = ArrayList<String?>()
        val separator = System.getProperty("file.separator")
        // TODO Java 9: ProcessHandle.current().info().command();
        val javaExecPath = System.getProperty("java.home") + separator + "bin" + separator + "java"
        if (!(File(javaExecPath)).exists()) {
            System.err.println(
                "A Java installation could not be found. If you are distributing this app with a bundled JRE, be sure to set the -XstartOnFirstThread argument manually!",
            )
            return false
        }
        jvmArgs.add(javaExecPath)
        jvmArgs.add("-XstartOnFirstThread")
        jvmArgs.add("-D" + JVM_RESTARTED_ARG + "=true")
        jvmArgs.addAll(ManagementFactory.getRuntimeMXBean().inputArguments)
        jvmArgs.add("-cp")
        jvmArgs.add(System.getProperty("java.class.path"))
        jvmArgs.add(System.getenv("JAVA_MAIN_CLASS_" + pid))

        try {
            if (!redirectOutput) {
                val processBuilder = ProcessBuilder(jvmArgs)
                processBuilder.start()
            } else {
                val process = (ProcessBuilder(jvmArgs)).redirectErrorStream(true).start()
                val processOutput = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?

                while ((processOutput.readLine().also { line = it }) != null) {
                    println(line)
                }

                process.waitFor()
            }
        } catch (e: Exception) {
            System.err.println("There was a problem restarting the JVM")
            e.printStackTrace()
        }

        return true
    }

    /**
     * Starts a new JVM if required; otherwise executes the main method code given
     * as Runnable. When used with lambdas, this is allows for less verbose code
     * than [.startNewJvmIfRequired]:
     *
     * <pre>
     * public static void main(String... args) {
     * StartOnFirstThreadHelper.executeIfJVMValid(() -> {
     * // the actual main method code
     * });
     * }
    </pre> *
     *
     * @param mainMethodCode
     */
    fun executeIfJVMValid(mainMethodCode: Runnable) {
        if (startNewJvmIfRequired()) {
            return
        }
        mainMethodCode.run()
    }
}
