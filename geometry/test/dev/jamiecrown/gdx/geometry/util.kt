package dev.jamiecrown.gdx.geometry

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Mesh
import net.mgsx.gltf.loaders.gltf.GLTFLoader

fun loadFirstMeshFromGLTF(path: String): Mesh {
    val sceneAsset = GLTFLoader().load(Gdx.files.internal(path))
    // In Scene3D, SceneAsset.scene is a ModelInstance. Get its Model, then first Mesh.
    val model = sceneAsset.scene.model
    require(model.meshes.size > 0) { "No meshes found in GLTF at $path" }
    return model.meshes.first()
}