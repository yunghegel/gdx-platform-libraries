package dev.jamiecrown.gdx.geometry

import dev.jamiecrown.gdx.geometry.model.ifs.struct.IEdge
import dev.jamiecrown.gdx.geometry.model.ifs.struct.IFace
import dev.jamiecrown.gdx.geometry.model.ifs.struct.IVertex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HashCodeEqualityTest {

    @Test
    fun vertex_hash_and_equals_by_index() {
        val v1 = IVertex().apply { index = 5 }
        val v2 = IVertex().apply { index = 5 }
        val v3 = IVertex().apply { index = 7 }

        assertEquals(v1, v2)
        assertEquals(v1.hashCode(), v2.hashCode())
        assertNotEquals(v1, v3)
    }

    @Test
    fun edge_unordered_pair_hash_and_equals() {
        val e1 = IEdge().apply { v0 = 1; v1 = 3 }
        val e2 = IEdge().apply { v0 = 3; v1 = 1 }
        val e3 = IEdge().apply { v0 = 1; v1 = 4 }

        assertEquals(e1, e2)
        assertEquals(e1.hashCode(), e2.hashCode())
        assertNotEquals(e1, e3)
    }

    @Test
    fun face_unordered_triple_hash_and_equals() {
        val f1 = IFace().apply { i0 = 2; i1 = 5; i2 = 9 }
        val f2 = IFace().apply { i0 = 9; i1 = 2; i2 = 5 }
        val f3 = IFace().apply { i0 = 2; i1 = 5; i2 = 8 }

        assertEquals(f1, f2)
        assertEquals(f1.hashCode(), f2.hashCode())
        assertNotEquals(f1, f3)
    }

    @Test
    fun uninitialized_objects_use_identity_and_are_not_equal() {
        val v1 = IVertex()
        val v2 = IVertex()
        assertNotEquals(v1, v2)

        val e1 = IEdge()
        val e2 = IEdge()
        assertNotEquals(e1, e2)

        val f1 = IFace()
        val f2 = IFace()
        assertNotEquals(f1, f2)
    }
}
