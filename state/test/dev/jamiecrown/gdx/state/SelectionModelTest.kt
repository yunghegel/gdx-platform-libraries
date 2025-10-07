package dev.jamiecrown.gdx.state

import dev.jamiecrown.gdx.state.selection.SelectionListener
import dev.jamiecrown.gdx.state.selection.SelectionModel
import dev.jamiecrown.gdx.state.selection.SelectionPolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SelectionModelTest {

    @Test
    fun testSingleSelectionPolicyAndToggle() {
        println("[DEBUG_LOG] Starting SelectionModelTest.testSingleSelectionPolicyAndToggle")
        val model = SelectionModel<Int>(SelectionPolicy(allowMultiple = false, allowEmpty = true))
        val events = mutableListOf<String>()
        model.addListener(SelectionListener { e ->
            println("[DEBUG_LOG] Selection changed cause=${'$'}{e.cause} added=${'$'}{e.added} removed=${'$'}{e.removed} after=${'$'}{e.after}")
            events += "${e.cause}:${e.after}"
        })
        model.select(1, cause = "select1")
        model.select(2, cause = "select2")
        model.toggle(2, cause = "toggle2") // deselects 2 (allowEmpty=true)
        assertEquals(listOf("select1:[1]", "select2:[2]", "toggle2:[]"), events)
        println("[DEBUG_LOG] Completed SelectionModelTest.testSingleSelectionPolicyAndToggle")
    }

    @Test
    fun testMultiWithMaxAndPredicate() {
        println("[DEBUG_LOG] Starting SelectionModelTest.testMultiWithMaxAndPredicate")
        val policy = SelectionPolicy<Int>(allowMultiple = true, allowEmpty = false, maxSelections = 2, canSelect = { it % 2 == 0 })
        val model = SelectionModel(policy)
        val events = mutableListOf<String>()
        model.addListener(SelectionListener { e ->
            println("[DEBUG_LOG] Selection changed cause=${'$'}{e.cause} added=${'$'}{e.added} removed=${'$'}{e.removed} after=${'$'}{e.after}")
            events += e.after.joinToString(",", prefix = e.cause + ":", postfix = "")
        })
        model.setAll(listOf(1, 2, 4, 6), cause = "setAll") // filtered to even, then cap to size 2 -> [2,4]
        model.select(8, cause = "select8") // max reached; should not add
        assertEquals(listOf("setAll:2,4"), events)
        assertEquals(listOf(2, 4), model.getSelection())
        println("[DEBUG_LOG] Completed SelectionModelTest.testMultiWithMaxAndPredicate")
    }
}
