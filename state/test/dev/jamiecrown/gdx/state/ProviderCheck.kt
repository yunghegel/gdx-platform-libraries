package dev.jamiecrown.gdx.state

import dev.jamiecrown.gdx.state.app.AppStateManager
import dev.jamiecrown.gdx.state.storage.FileKeyValueStore
import org.junit.jupiter.api.BeforeAll
import sun.tools.jconsole.OutputViewer.init
import kotlin.test.BeforeTest
import kotlin.test.Test

class ProviderCheck {

    object Count {
        var count = 0
        val store : FileKeyValueStore by provision(java.io.File("state"),"test")
        val manager : AppStateManager by provision(store)

        @JvmStatic
        @BeforeAll
        fun init() {
            Injector.configure(Count.manager)
            val obj : Injectable by provision()

        }
    }

    @JvmInline
    value class ArgumentsId(val id: String

    )

    class Injectable() {
        @Persist("y")
        var y : Int = 0

        @Persist("x")
        var x : Int = 0

        override fun toString(): String {
            return "[$x,y=$y]"
        }
    }




    class Arguments(@Persist("y") val xv : Int, @Persist("x") val y : Int) { init {
        Count.count++
        }

        override fun toString(): String {
            return "[$xv,y=$y]"
        }
    }












    @Test
    fun `constructor with parameters should initialize`() {
        /*
          there should only be one instance of Arguments created
         */
        val a : Injectable by inject()
        val b : Injectable by inject()
        assert(a === b)
        assert (Count.count == 1)


    }


    @Test
    fun `provision enables provider for type` () {
        val p : Injectable by provision()
        println("p = $p")

        val q : Injectable by inject()

        println("q = $q")
        assert(p === q)


    }


}